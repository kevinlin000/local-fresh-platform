# ECPay Sandbox Runbook

This runbook documents the deployed ECPay sandbox verification path through the CloudFront storefront, EC2 + Nginx HTTPS API domain, Spring Boot callback endpoint, and MySQL payment event trail.

## Current Boundary

The deployed EC2 backend is currently switched to ECPay sandbox through a
systemd drop-in. Local development and fallback demos can still use the default
`demo` provider.

Already implemented:

- Backend provider switch: `PAYMENT_PROVIDER=demo|ecpay`
- ECPay CheckMacValue calculation and callback verification
- `/payment/callback` provider callback endpoint returning `1|OK` or `0|FAIL`
- Member frontend POST-form redirect when `signType` is `ECPAY_SHA256`
- Payment event trail for request, success, duplicate, and rejected callbacks

Verified on the deployed EC2 runtime:

- `/actuator/info` publicly exposes deployed commit `8d7a0d5eeefe`.
- `scripts/check-ecpay-sandbox-readiness.sh` passes health, Nginx, actuator
  info, callback invalid-signature response `0|FAIL`, and storefront reachability.
- `scripts/switch-ecpay-sandbox-ssm.sh status` shows effective
  `PAYMENT_PROVIDER=ecpay` with ECPay HashKey/HashIV redacted.
- Playwright reached ECPay stage checkout through the deployed CloudFront
  storefront for order `2068949685467095040`.
- Screenshot evidence: `docs/screenshots/10-ecpay-stage-checkout.png`.
- `payment_event` contains `ECPAY / REQUEST_CREATED / PENDING` with compact
  reference `ECPAY:REQUEST:2068949685467095040`.
- Playwright completed a real ECPay stage credit-card OTP flow for order
  `2068979325367758848`.
- ECPay ReturnURL reached `POST /payment/callback` through Nginx with HTTP
  `200`.
- The order moved to `status=2`, `pay_status=1`, and `payment_event` recorded
  `ECPAY / CALLBACK_SUCCEEDED / SUCCEEDED`.

Not yet implemented:

- Duplicate real-provider callback replay evidence
- Reconciliation job

## Current Preflight Status

As of 2026-06-22 16:55 +0800, the public preflight and success-flow result is:

- `https://localfresh-demo.duckdns.org/actuator/health`: passed
- Public API `Server` header: Nginx detected
- `https://localfresh-demo.duckdns.org/actuator/info`: passed, commit
  `8d7a0d5eeefe`
- `POST https://localfresh-demo.duckdns.org/payment/callback`: passed with
  HTTP `200` and body `0|FAIL` for an intentionally invalid ECPay payload
- Real ECPay ReturnURL callback: HTTP `200`
- Real payment event evidence: `CALLBACK_SUCCEEDED`

The previous deployed `404` blocker is resolved, and Playwright has verified
that the deployed storefront reaches ECPay stage checkout and completes sandbox
card OTP payment back through ReturnURL. The remaining payment gaps are
duplicate callback replay evidence and reconciliation.

## Local Contract Evidence

The application-level callback contract is covered locally:

```bash
cd backend-environment/local-fresh-backend
mvn -pl local-fresh-server -am \
  -Dtest=PaymentCallbackControllerEcpayContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

This test wires the real `EcpayPaymentGateway` into `PaymentCallbackController`
and proves both branches:

- a signed ECPay form callback returns `1|OK` and reaches `OrderPaymentService`
  with provider `ECPAY`, order number, `TradeNo`, `PaymentDate`, and raw payload.
- an invalid `CheckMacValue` returns `0|FAIL` and does not call the payment
  service.

This is not a replacement for real sandbox checkout. It is the repeatable local
evidence behind the deployed HTTP `200` + `0|FAIL` preflight.

## Deployed URL Plan

Use the existing deployed HTTPS entry points:

- Backend API: `https://localfresh-demo.duckdns.org`
- Member storefront: `https://d3hqnux25iirgl.cloudfront.net`
- ECPay ReturnURL: `https://localfresh-demo.duckdns.org/payment/callback`
- ECPay OrderResultURL: `https://d3hqnux25iirgl.cloudfront.net/orders`

Do not use ngrok for the main deployed verification path. Ngrok is only a local fallback when EC2 + Nginx is unavailable.

## EC2 Environment

Set these variables for the Spring Boot process:

```bash
PAYMENT_PROVIDER=ecpay
ECPAY_MERCHANT_ID=2000132
ECPAY_HASH_KEY=5294y06JbISpM5x9
ECPAY_HASH_IV=v77hoKGq4kWxNNIS
ECPAY_CHECKOUT_URL=https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5
ECPAY_RETURN_URL=https://localfresh-demo.duckdns.org/payment/callback
ECPAY_ORDER_RESULT_URL=https://d3hqnux25iirgl.cloudfront.net/orders
SOURCE_COMMIT=<deployed git commit, for example 8d7a0d5eeefe>
SOURCE_BRANCH=hardening-and-upgrade
```

The current EC2 instance uses a dedicated systemd payment drop-in at:

```text
/etc/systemd/system/local-fresh-backend.service.d/payment-provider.conf
```

Do not store production payment credentials in git. The values above are ECPay
stage values used for sandbox verification.

`SOURCE_COMMIT` and `SOURCE_BRANCH` are not secrets. They are exposed through
`/actuator/info` so the public preflight can distinguish "backend is healthy"
from "backend is running the expected version".

## Nginx Requirements

The public domain must proxy callback requests to Spring Boot on port `8080`:

```nginx
location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

`application-prod.yml` enables `server.forward-headers-strategy=framework`, so the app can honor the forwarded host and scheme from Nginx when needed.

The callback endpoint is a server-to-server form POST, so browser CORS is not involved.

## SSM Switch Commands

Check the current EC2 payment provider without exposing HashKey/HashIV:

```bash
scripts/switch-ecpay-sandbox-ssm.sh status
```

Enable ECPay sandbox through SSM:

```bash
EXPECTED_DEPLOY_COMMIT=<deployed-commit> \
scripts/switch-ecpay-sandbox-ssm.sh enable
```

Rollback to the default demo provider by removing the payment drop-in:

```bash
scripts/switch-ecpay-sandbox-ssm.sh rollback
```

The enable command runs public readiness before and after the restart. The
rollback command restarts the backend and leaves the existing deployment
identity drop-in untouched.

## Verification Steps

1. Package the backend release and deploy it using
   [docs/backend-deploy-runbook.md](backend-deploy-runbook.md):

   ```bash
   scripts/package-backend-release.sh
   ```

2. Run the public preflight before changing provider:

   ```bash
   EXPECTED_DEPLOY_COMMIT=<deployed-commit> scripts/check-ecpay-sandbox-readiness.sh
   ```

   To require a specific backend commit:

   ```bash
   EXPECTED_DEPLOY_COMMIT=8d7a0d5eeefe scripts/check-ecpay-sandbox-readiness.sh
   ```

   The callback check must return HTTP `200` with `0|FAIL` for an intentionally invalid signature. If it returns `404`, stop here and deploy the backend version containing `/payment/callback` before changing `PAYMENT_PROVIDER`.

3. Switch EC2 env to `PAYMENT_PROVIDER=ecpay` and restart Spring Boot, or use
   `scripts/switch-ecpay-sandbox-ssm.sh enable`.
4. Confirm backend health:

   ```bash
   curl -s https://localfresh-demo.duckdns.org/actuator/health
   ```

5. Open the CloudFront storefront and create or use a pending order.
6. Click `付款`; the frontend should submit a hidden POST form to ECPay stage checkout.
7. Confirm the ECPay stage checkout page displays the Local Fresh order number and amount.
8. Complete the sandbox payment on ECPay.
9. Confirm ECPay receives `1|OK` from ReturnURL.
10. Confirm the order moves from pending payment to pending confirmation.
11. Confirm `payment_event` contains:

   - `REQUEST_CREATED`
   - `CALLBACK_SUCCEEDED`
   - `provider = ECPAY`
   - `provider_trade_no = TradeNo`
   - `idempotency_key` populated

12. Send or replay the same callback once to confirm duplicate handling records
    `CALLBACK_DUPLICATE` without changing the order again.

## Rollback

Use:

```bash
scripts/switch-ecpay-sandbox-ssm.sh rollback
```

This removes only the payment-provider drop-in and restarts Spring Boot. The
member frontend will return to immediate demo payment behavior.
