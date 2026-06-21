# EC2 Runtime Templates

These templates document the current single-EC2 backend runtime without storing
hostnames, passwords, private keys, or provider credentials in git.

The intended runtime shape is:

- Nginx terminates HTTPS and proxies API traffic to Spring Boot on `127.0.0.1:8080`.
- systemd runs `/opt/local-fresh/current.jar` with the `prod` profile.
- `/etc/local-fresh/local-fresh-server.env` stores runtime configuration.
- `SOURCE_COMMIT` and `SOURCE_BRANCH` identify the deployed build through
  `/actuator/info`.

## Files

| File | Purpose |
| --- | --- |
| `local-fresh-server.env.example` | Non-secret placeholder list for EC2 environment variables. |
| `local-fresh-server.service` | systemd service for the Spring Boot backend jar. |
| `nginx-localfresh-demo.conf` | Nginx reverse proxy template for the public API domain. |

## Install Outline

On EC2, copy the files into the expected locations and replace every
`replace-me` value before starting the service:

```bash
sudo install -d -m 0750 /etc/local-fresh
sudo install -m 0640 local-fresh-server.env.example /etc/local-fresh/local-fresh-server.env
sudo install -m 0644 local-fresh-server.service /etc/systemd/system/local-fresh-server.service
sudo install -m 0644 nginx-localfresh-demo.conf /etc/nginx/sites-available/localfresh-demo.conf
sudo ln -sfn /etc/nginx/sites-available/localfresh-demo.conf /etc/nginx/sites-enabled/localfresh-demo.conf
sudo systemctl daemon-reload
sudo nginx -t
sudo systemctl enable local-fresh-server
```

After deploying a release jar, verify the public runtime:

```bash
curl -s https://localfresh-demo.duckdns.org/actuator/info
EXPECTED_DEPLOY_COMMIT=<deployed-commit> scripts/check-ecpay-sandbox-readiness.sh
```

Keep `PAYMENT_PROVIDER=demo` until the public callback preflight returns
HTTP `200` with body `0|FAIL` for the intentionally invalid ECPay payload.
