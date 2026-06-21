# ECPay Sandbox Runbook

This runbook documents the next payment hardening slice: verifying the existing ECPay parser and member-side POST redirect against the ECPay sandbox through the deployed EC2 + Nginx HTTPS API domain.

## Current Boundary

The default payment provider remains `demo` so portfolio demos still complete locally without third-party dependency.

Already implemented:

- Backend provider switch: `PAYMENT_PROVIDER=demo|ecpay`
- ECPay CheckMacValue calculation and callback verification
- `/payment/callback` provider callback endpoint returning `1|OK` or `0|FAIL`
- Member frontend POST-form redirect when `signType` is `ECPAY_SHA256`
- Payment event trail for request, success, duplicate, and rejected callbacks

Not yet verified:

- Real ECPay sandbox browser checkout
- Real ReturnURL callback through EC2 + Nginx
- Real OrderResultURL return to the CloudFront storefront
- Reconciliation job

## Current Preflight Status

As of 2026-06-21, the public preflight result is:

- `https://localfresh-demo.duckdns.org/actuator/health`: passed
- Public API `Server` header: Nginx detected
- `https://localfresh-demo.duckdns.org/actuator/info`: HTTP `404`
- `POST https://localfresh-demo.duckdns.org/payment/callback`: HTTP `404`

Do not switch the deployed Spring Boot process to `PAYMENT_PROVIDER=ecpay` until the EC2 backend is redeployed with the version containing `/payment/callback`. If ECPay sends ReturnURL callbacks while the endpoint returns `404`, the payment cannot be verified or recorded in `payment_event`.

## Local Contract Evidence

The public endpoint is still blocked by the deployed backend version, but the
application-level callback contract is covered locally:

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
evidence that the deployed preflight should return HTTP `200` + `0|FAIL` once
EC2 runs the backend version containing `/payment/callback`.

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
SOURCE_COMMIT=<deployed git commit, for example 42c12ce>
SOURCE_BRANCH=hardening-and-upgrade
```

Keep `PAYMENT_PROVIDER=demo` for normal portfolio demos.

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

## Verification Steps

1. Run the public preflight before changing provider:

   ```bash
   scripts/check-ecpay-sandbox-readiness.sh
   ```

   To require a specific backend commit:

   ```bash
   EXPECTED_DEPLOY_COMMIT=42c12ce scripts/check-ecpay-sandbox-readiness.sh
   ```

   The callback check must return HTTP `200` with `0|FAIL` for an intentionally invalid signature. If it returns `404`, stop here and deploy the backend version containing `/payment/callback` before changing `PAYMENT_PROVIDER`.

2. Switch EC2 env to `PAYMENT_PROVIDER=ecpay` and restart Spring Boot.
3. Confirm backend health:

   ```bash
   curl -s https://localfresh-demo.duckdns.org/actuator/health
   ```

4. Open the CloudFront storefront and create or use a pending order.
5. Click `付款`; the frontend should submit a hidden POST form to ECPay stage checkout.
6. Complete the sandbox payment on ECPay.
7. Confirm ECPay receives `1|OK` from ReturnURL.
8. Confirm the order moves from pending payment to pending confirmation.
9. Confirm `payment_event` contains:

   - `REQUEST_CREATED`
   - `CALLBACK_SUCCEEDED`
   - `provider = ECPAY`
   - `provider_trade_no = TradeNo`
   - `idempotency_key` populated

10. Send or replay the same callback once to confirm duplicate handling records `CALLBACK_DUPLICATE` without changing the order again.

## Rollback

Set:

```bash
PAYMENT_PROVIDER=demo
```

Then restart Spring Boot. The member frontend will return to immediate demo payment behavior.
