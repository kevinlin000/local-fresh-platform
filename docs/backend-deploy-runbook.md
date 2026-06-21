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

The local `local-fresh-cli` AWS profile can discover the EC2 instance and can
send an EC2 Instance Connect public key, but it currently cannot complete the
deploy from this workstation because SSH to TCP `22` times out and the profile
does not have permission to inspect or update the security group. Fix one of
the access paths above before running the copy/restart steps below.

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
SOURCE_BRANCH=hardening-and-upgrade
```

They are intentionally non-secret and are exposed through `/actuator/info`.

## Runtime Templates

The repository includes non-secret EC2 runtime templates under `deploy/ec2/`:

- `local-fresh-server.env.example`: placeholder environment variables for the
  Spring Boot `prod` profile.
- `local-fresh-server.service`: systemd service for `/opt/local-fresh/current.jar`.
- `nginx-localfresh-demo.conf`: HTTPS reverse proxy to `127.0.0.1:8080`.

Copy these templates to EC2, replace placeholders outside git, then validate:

```bash
sudo nginx -t
sudo systemctl daemon-reload
sudo systemctl restart local-fresh-server
sudo systemctl status local-fresh-server --no-pager
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

Keep the previous jar under `/opt/local-fresh/releases/<previous-commit>/`.
Rollback should restore the `current.jar` symlink, set `SOURCE_COMMIT` back to
the previous commit, restart the service, and rerun the same preflight.

To rollback only the payment provider while keeping the deployed jar and
deployment identity intact:

```bash
scripts/switch-ecpay-sandbox-ssm.sh rollback
```

Do not run ECPay sandbox checkout during backend jar rollback validation. Keep
or restore `PAYMENT_PROVIDER=demo` until the public callback path is verified
again.
