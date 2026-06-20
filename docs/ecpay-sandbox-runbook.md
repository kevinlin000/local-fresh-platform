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
```

Keep `PAYMENT_PROVIDER=demo` for normal portfolio demos.

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

1. Switch EC2 env to `PAYMENT_PROVIDER=ecpay` and restart Spring Boot.
2. Confirm backend health:

   ```bash
   curl -s https://localfresh-demo.duckdns.org/actuator/health
   ```

3. Open the CloudFront storefront and create or use a pending order.
4. Click `付款`; the frontend should submit a hidden POST form to ECPay stage checkout.
5. Complete the sandbox payment on ECPay.
6. Confirm ECPay receives `1|OK` from ReturnURL.
7. Confirm the order moves from pending payment to pending confirmation.
8. Confirm `payment_event` contains:

   - `REQUEST_CREATED`
   - `CALLBACK_SUCCEEDED`
   - `provider = ECPAY`
   - `provider_trade_no = TradeNo`
   - `idempotency_key` populated

9. Send or replay the same callback once to confirm duplicate handling records `CALLBACK_DUPLICATE` without changing the order again.

## Rollback

Set:

```bash
PAYMENT_PROVIDER=demo
```

Then restart Spring Boot. The member frontend will return to immediate demo payment behavior.
