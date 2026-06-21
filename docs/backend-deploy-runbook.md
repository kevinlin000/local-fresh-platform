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

## Package A Release

From the repository root:

```bash
scripts/package-backend-release.sh
```

The script:

- requires a clean worktree by default.
- runs `mvn -pl local-fresh-server -am verify`.
- copies the Spring Boot jar into `output/backend-release/<commit>/`.
- writes `release.env` with `SOURCE_COMMIT` and `SOURCE_BRANCH`.
- writes `ec2-deploy-commands.txt` with an editable EC2 command template.

Use `SKIP_VERIFY=true` only for a local dry run when a current jar already
exists:

```bash
ALLOW_DIRTY=true SKIP_VERIFY=true scripts/package-backend-release.sh
```

## EC2 Runtime Metadata

Set these variables on the Spring Boot process:

```bash
SOURCE_COMMIT=<deployed git commit>
SOURCE_BRANCH=hardening-and-upgrade
```

They are intentionally non-secret and are exposed through `/actuator/info`.

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

## Rollback

Keep the previous jar under `/opt/local-fresh/releases/<previous-commit>/`.
Rollback should restore the `current.jar` symlink, set `SOURCE_COMMIT` back to
the previous commit, restart the service, and rerun the same preflight.

Do not switch to ECPay sandbox during rollback validation. Keep
`PAYMENT_PROVIDER=demo` until the public callback path is verified again.
