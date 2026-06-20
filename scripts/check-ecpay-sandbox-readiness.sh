#!/usr/bin/env sh
set -eu

API_BASE_URL="${1:-${API_BASE_URL:-https://localfresh-demo.duckdns.org}}"
STOREFRONT_URL="${2:-${STOREFRONT_URL:-https://d3hqnux25iirgl.cloudfront.net/orders}}"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

fail() {
  printf "FAIL: %s\n" "$1" >&2
  exit 1
}

pass() {
  printf "PASS: %s\n" "$1"
}

request() {
  name="$1"
  shift
  body_file="$TMP_DIR/$name.body"
  header_file="$TMP_DIR/$name.headers"
  status_file="$TMP_DIR/$name.status"
  curl -sS -D "$header_file" -o "$body_file" -w "%{http_code}" "$@" > "$status_file"
}

trim_cr() {
  tr -d '\r'
}

printf "Checking ECPay sandbox readiness\n"
printf "API_BASE_URL=%s\n" "$API_BASE_URL"
printf "STOREFRONT_URL=%s\n" "$STOREFRONT_URL"

request health "$API_BASE_URL/actuator/health"
health_status="$(cat "$TMP_DIR/health.status")"
health_body="$(cat "$TMP_DIR/health.body")"
[ "$health_status" = "200" ] || fail "health endpoint returned HTTP $health_status"
printf "%s" "$health_body" | grep -q '"status":"UP"' || fail "health endpoint did not report UP: $health_body"
pass "backend health is UP"

server_header="$(grep -i '^Server:' "$TMP_DIR/health.headers" | head -n 1 | trim_cr || true)"
case "$server_header" in
  *nginx*) pass "public API is served through Nginx ($server_header)" ;;
  *) printf "WARN: Server header does not mention Nginx: %s\n" "${server_header:-<missing>}" ;;
esac

request callback \
  -X POST "$API_BASE_URL/payment/callback" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'MerchantID=2000132' \
  --data-urlencode 'MerchantTradeNo=PRECHECK001' \
  --data-urlencode 'RtnCode=1' \
  --data-urlencode 'TradeNo=PRECHECKTRADE' \
  --data-urlencode 'PaymentDate=2026/06/20 12:50:00' \
  --data-urlencode 'CheckMacValue=BAD'

callback_status="$(cat "$TMP_DIR/callback.status")"
callback_body="$(cat "$TMP_DIR/callback.body")"
[ "$callback_status" = "200" ] || fail "callback endpoint returned HTTP $callback_status; deploy the backend version containing /payment/callback before switching PAYMENT_PROVIDER=ecpay"
[ "$callback_body" = "0|FAIL" ] || fail "invalid callback should return 0|FAIL, got: $callback_body"
pass "callback endpoint is deployed and rejects invalid signatures with 0|FAIL"

request storefront "$STOREFRONT_URL"
storefront_status="$(cat "$TMP_DIR/storefront.status")"
case "$storefront_status" in
  200|301|302|403) pass "storefront URL is reachable with HTTP $storefront_status" ;;
  *) fail "storefront URL returned HTTP $storefront_status" ;;
esac

printf "\nReady for controlled ECPay sandbox switch.\n"
