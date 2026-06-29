# Backend Deploy Runbook

This runbook keeps the current EC2 backend sync process explicit without adding
full CD automation yet. It assumes the public API domain remains:

```text
https://localfresh-demo.duckdns.org
```

## Goal

Deploy a verified backend jar to EC2, expose its commit through
`/actuator/info`, and prove the public payment callback path is running the
expected backend version before any ECPay sandbox switch.

## Access Prerequisites

Direct deployment requires one working EC2 command channel:

- SSH with the private key for the instance key pair, or
- EC2 Instance Connect plus security-group access to TCP `22` from the current
  operator IP, or
- AWS Systems Manager Session Manager with the required instance profile and
  IAM permissions.

The current demo instance supports AWS Systems Manager. SSH does not need to be
opened for routine demo backend refreshes.

## Package A Release

From the repository root:

```bash
scripts/package-backend-release.sh
```

The script:

- requires a clean worktree by default.
- runs `mvn -pl local-fresh-server -am verify`.
- copies the Spring Boot jar into `output/backend-release/<commit>/`.
- copies `deploy/ec2/` runtime templates into `deploy-templates/`.
- writes `release.env` with `SOURCE_COMMIT` and `SOURCE_BRANCH`.
- writes `ec2-deploy-commands.txt` with an editable EC2 command template.
- writes `release-manifest.txt` and `SHA256SUMS` for deploy-package integrity.

Use `SKIP_VERIFY=true` only for a local dry run when a current jar already
exists:

```bash
ALLOW_DIRTY=true SKIP_VERIFY=true scripts/package-backend-release.sh
```

GitHub Actions also publishes the same deployable package from the backend CI
job as the `backend-release-package` artifact. Use that artifact when you want
the jar and deploy templates to come from a verified remote CI run instead of a
local workstation.

Before copying a package to EC2, verify it locally:

```bash
scripts/verify-backend-release-package.sh output/backend-release/<commit>
```

The verifier checks the expected jar, `release.env`, manifest, deploy commands,
runtime templates, and all SHA256 checksums. It also accepts the parent
`output/backend-release` directory when exactly one package should be selected
from a downloaded artifact.

## EC2 Runtime Metadata

Set these variables on the Spring Boot process:

```bash
SOURCE_COMMIT=<deployed git commit>
SOURCE_BRANCH=main
```

They are intentionally non-secret and are exposed through `/actuator/info`.

The current EC2 runtime stores these values in:

```text
/etc/systemd/system/local-fresh-backend.service.d/release.conf
```

The active demo release as of 2026-06-29 is:

```text
SOURCE_COMMIT=f93f6c41a373
SOURCE_BRANCH=main
```

## Runtime Templates

The repository includes non-secret EC2 runtime templates under `deploy/ec2/`:

- `local-fresh-server.env.example`: placeholder environment variables for the
  Spring Boot `prod` profile.
- `local-fresh-server.service`: systemd service for `/opt/local-fresh/current.jar`.
- `nginx-localfresh-demo.conf`: HTTPS reverse proxy to `127.0.0.1:8080`.

These templates document the intended normalized layout. The current demo EC2
service still uses the earlier layout:

```text
service: local-fresh-backend.service
working directory: /home/ubuntu/local-fresh
jar path: /home/ubuntu/local-fresh/sky-server-1.0-SNAPSHOT.jar
release archive path: /home/ubuntu/local-fresh/releases/<commit>/local-fresh-server.jar
application config: /home/ubuntu/local-fresh/application-prod.yml
payment drop-in: /etc/systemd/system/local-fresh-backend.service.d/payment-provider.conf
release drop-in: /etc/systemd/system/local-fresh-backend.service.d/release.conf
```

The `sky-server-1.0-SNAPSHOT.jar` filename is a legacy EC2 service path retained
for the current demo runtime. It does not represent the current product name.

If you normalize the runtime later, update the service and this runbook in the
same change. Until then, deploy to the existing path above.

Validate service configuration after any runtime-template change:

```bash
sudo nginx -t
sudo systemctl daemon-reload
sudo systemctl restart local-fresh-backend.service
sudo systemctl status local-fresh-backend.service --no-pager
```

## Deploy Through SSM + S3

Use this path when SSH is intentionally closed.

1. Upload the verified jar and metadata to a private S3 path or a controlled
   release prefix:

   ```bash
   aws s3 cp output/backend-release/<commit>/local-fresh-server-<commit>.jar \
     s3://<release-bucket>/backend-release/<commit>/local-fresh-server-<commit>.jar
   aws s3 cp output/backend-release/<commit>/release-manifest.txt \
     s3://<release-bucket>/backend-release/<commit>/release-manifest.txt
   aws s3 cp output/backend-release/<commit>/SHA256SUMS \
     s3://<release-bucket>/backend-release/<commit>/SHA256SUMS
   ```

2. Generate a short-lived presigned URL for the jar:

   ```bash
   aws s3 presign \
     s3://<release-bucket>/backend-release/<commit>/local-fresh-server-<commit>.jar \
     --expires-in 3600
   ```

3. Send an SSM `AWS-RunShellScript` command that:

   - downloads the jar with `curl -fL --retry 3`.
   - verifies the SHA256 from `release-manifest.txt`.
   - installs the jar to `/home/ubuntu/local-fresh/releases/<commit>/`.
   - backs up `/home/ubuntu/local-fresh/sky-server-1.0-SNAPSHOT.jar`.
   - replaces `/home/ubuntu/local-fresh/sky-server-1.0-SNAPSHOT.jar`.
   - writes `release.conf` with `SOURCE_COMMIT` and `SOURCE_BRANCH`.
   - restarts `local-fresh-backend.service`.
   - checks local `/actuator/health` and `/actuator/info`.

4. Run the public readiness check:

   ```bash
   EXPECTED_DEPLOY_COMMIT=<commit> scripts/check-ecpay-sandbox-readiness.sh
   scripts/switch-ecpay-sandbox-ssm.sh status
   ```

The latest demo-data refresh used this path to deploy `f93f6c41a373` while
preserving the existing ECPay sandbox payment-provider drop-in.
```

## Verify The Deployment

After restarting the EC2 service:

```bash
curl -s https://localfresh-demo.duckdns.org/actuator/info
EXPECTED_DEPLOY_COMMIT=<deployed git commit> scripts/check-ecpay-sandbox-readiness.sh
```

The expected ECPay readiness state before switching providers is:

- `/actuator/health`: HTTP `200`, status `UP`
- `/actuator/info`: HTTP `200`, includes the expected commit
- `/actuator/prometheus`: HTTP `200`, emits Prometheus scrape-format metrics
- `/payment/callback`: HTTP `200`, body `0|FAIL` for the intentionally invalid
  CheckMacValue preflight payload

Only after those pass should the deployed process be switched to:

```bash
PAYMENT_PROVIDER=ecpay
```

The current SSM helper performs the preflight, writes a dedicated payment
provider drop-in, restarts the backend, and verifies the runtime again:

```bash
EXPECTED_DEPLOY_COMMIT=<deployed git commit> scripts/switch-ecpay-sandbox-ssm.sh enable
scripts/switch-ecpay-sandbox-ssm.sh status
```

## Rollback

Keep the previous jar backup under `/home/ubuntu/local-fresh/` and release jars
under `/home/ubuntu/local-fresh/releases/<previous-commit>/`. Rollback should
copy the previous jar back to
`/home/ubuntu/local-fresh/sky-server-1.0-SNAPSHOT.jar`, set `SOURCE_COMMIT`
back to the previous commit in `release.conf`, restart
`local-fresh-backend.service`, and rerun the same preflight.

To rollback only the payment provider while keeping the deployed jar and
deployment identity intact:

```bash
scripts/switch-ecpay-sandbox-ssm.sh rollback
```

Do not run ECPay sandbox checkout during backend jar rollback validation. Keep
or restore `PAYMENT_PROVIDER=demo` until the public callback path is verified
again.
