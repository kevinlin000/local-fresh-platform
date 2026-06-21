#!/usr/bin/env sh
set -eu

MODE="${1:-status}"
AWS_REGION="${AWS_REGION:-ap-northeast-1}"
INSTANCE_ID="${INSTANCE_ID:-i-0a21d1fff310e3168}"
SERVICE_NAME="${SERVICE_NAME:-local-fresh-backend.service}"
DROPIN_PATH="${DROPIN_PATH:-/etc/systemd/system/local-fresh-backend.service.d/payment-provider.conf}"
API_BASE_URL="${API_BASE_URL:-https://localfresh-demo.duckdns.org}"
STOREFRONT_URL="${STOREFRONT_URL:-https://d3hqnux25iirgl.cloudfront.net/orders}"

# ECPay's public stage credentials are not production secrets. Override these
# when testing with a different sandbox account.
ECPAY_MERCHANT_ID="${ECPAY_MERCHANT_ID:-2000132}"
ECPAY_HASH_KEY="${ECPAY_HASH_KEY:-5294y06JbISpM5x9}"
ECPAY_HASH_IV="${ECPAY_HASH_IV:-v77hoKGq4kWxNNIS}"
ECPAY_CHECKOUT_URL="${ECPAY_CHECKOUT_URL:-https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5}"
ECPAY_RETURN_URL="${ECPAY_RETURN_URL:-https://localfresh-demo.duckdns.org/payment/callback}"
ECPAY_ORDER_RESULT_URL="${ECPAY_ORDER_RESULT_URL:-https://d3hqnux25iirgl.cloudfront.net/orders}"

case "$MODE" in
  status|enable|rollback) ;;
  *)
    printf "Usage: %s [status|enable|rollback]\n" "$0" >&2
    exit 2
    ;;
esac

if [ "$MODE" = "enable" ]; then
  EXPECTED_DEPLOY_COMMIT="${EXPECTED_DEPLOY_COMMIT:-}"
  if [ -z "$EXPECTED_DEPLOY_COMMIT" ]; then
    printf "EXPECTED_DEPLOY_COMMIT is required for enable mode.\n" >&2
    printf "Example: EXPECTED_DEPLOY_COMMIT=a8948ddb0a93 %s enable\n" "$0" >&2
    exit 2
  fi
  EXPECTED_DEPLOY_COMMIT="$EXPECTED_DEPLOY_COMMIT" \
    API_BASE_URL="$API_BASE_URL" \
    STOREFRONT_URL="$STOREFRONT_URL" \
    scripts/check-ecpay-sandbox-readiness.sh
fi

COMMANDS_JSON="$(
  LOCALFRESH_SWITCH_MODE="$MODE" \
  SERVICE_NAME="$SERVICE_NAME" \
  DROPIN_PATH="$DROPIN_PATH" \
  ECPAY_MERCHANT_ID="$ECPAY_MERCHANT_ID" \
  ECPAY_HASH_KEY="$ECPAY_HASH_KEY" \
  ECPAY_HASH_IV="$ECPAY_HASH_IV" \
  ECPAY_CHECKOUT_URL="$ECPAY_CHECKOUT_URL" \
  ECPAY_RETURN_URL="$ECPAY_RETURN_URL" \
  ECPAY_ORDER_RESULT_URL="$ECPAY_ORDER_RESULT_URL" \
  python3 - <<'PY'
import base64
import json
import os

mode = os.environ["LOCALFRESH_SWITCH_MODE"]
service = os.environ["SERVICE_NAME"]
dropin = os.environ["DROPIN_PATH"]

sanitize = (
    "sudo sed -E "
    "'s/(Environment=(ECPAY_HASH_KEY|ECPAY_HASH_IV|PAYMENT_CALLBACK_SECRET)=).*/\\1<redacted>/I' "
    f"{dropin} 2>/dev/null || true"
)

commands = [
    "set -eu",
    f"echo mode={mode}",
    f"echo service={service}",
    "echo service-active=$(systemctl is-active " + service + " || true)",
    "echo actuator-info=$(curl -fsS http://127.0.0.1:8080/actuator/info)",
]

if mode == "status":
    commands.extend([
        "echo payment-dropin:",
        sanitize,
        "echo effective-payment-env:",
        f"sudo systemctl show {service} --property=Environment | "
        "tr ' ' '\\n' | "
        "grep -E 'PAYMENT_PROVIDER|ECPAY_' | "
        "sed -E 's/(ECPAY_HASH_KEY|ECPAY_HASH_IV)=.*/\\1=<redacted>/' || true",
    ])
elif mode == "enable":
    lines = [
        "[Service]",
        "Environment=PAYMENT_PROVIDER=ecpay",
        f"Environment=ECPAY_MERCHANT_ID={os.environ['ECPAY_MERCHANT_ID']}",
        f"Environment=ECPAY_HASH_KEY={os.environ['ECPAY_HASH_KEY']}",
        f"Environment=ECPAY_HASH_IV={os.environ['ECPAY_HASH_IV']}",
        f"Environment=ECPAY_CHECKOUT_URL={os.environ['ECPAY_CHECKOUT_URL']}",
        f"Environment=ECPAY_RETURN_URL={os.environ['ECPAY_RETURN_URL']}",
        f"Environment=ECPAY_ORDER_RESULT_URL={os.environ['ECPAY_ORDER_RESULT_URL']}",
    ]
    encoded = base64.b64encode(("\n".join(lines) + "\n").encode()).decode()
    commands.extend([
        "sudo mkdir -p $(dirname " + dropin + ")",
        f"printf %s {encoded} | base64 -d | sudo tee {dropin} >/dev/null",
        f"sudo chmod 0640 {dropin}",
        "echo payment-dropin-written:",
        sanitize,
        "sudo systemctl daemon-reload",
        f"sudo systemctl restart {service}",
        "for i in 1 2 3 4 5 6 7 8 9 10 11 12; do "
        "if curl -fsS http://127.0.0.1:8080/actuator/health; then break; fi; sleep 5; done",
        f"sudo systemctl is-active {service}",
        "curl -fsS http://127.0.0.1:8080/actuator/info",
        "echo effective-payment-env:",
        f"sudo systemctl show {service} --property=Environment | "
        "tr ' ' '\\n' | "
        "grep -E 'PAYMENT_PROVIDER|ECPAY_' | "
        "sed -E 's/(ECPAY_HASH_KEY|ECPAY_HASH_IV)=.*/\\1=<redacted>/' || true",
    ])
elif mode == "rollback":
    commands.extend([
        f"sudo rm -f {dropin}",
        "echo payment-dropin-removed",
        "sudo systemctl daemon-reload",
        f"sudo systemctl restart {service}",
        "for i in 1 2 3 4 5 6 7 8 9 10 11 12; do "
        "if curl -fsS http://127.0.0.1:8080/actuator/health; then break; fi; sleep 5; done",
        f"sudo systemctl is-active {service}",
        "curl -fsS http://127.0.0.1:8080/actuator/info",
        "echo effective-payment-env:",
        f"sudo systemctl show {service} --property=Environment | "
        "tr ' ' '\\n' | "
        "grep -E 'PAYMENT_PROVIDER|ECPAY_' | "
        "sed -E 's/(ECPAY_HASH_KEY|ECPAY_HASH_IV)=.*/\\1=<redacted>/' || true",
    ])

print(json.dumps({"commands": commands}))
PY
)"

COMMAND_ID="$(
  aws ssm send-command \
    --instance-ids "$INSTANCE_ID" \
    --document-name AWS-RunShellScript \
    --comment "localfresh-ecpay-sandbox-$MODE" \
    --parameters "$COMMANDS_JSON" \
    --region "$AWS_REGION" \
    --query Command.CommandId \
    --output text
)"

printf "CommandId=%s\n" "$COMMAND_ID"

while :; do
  RESULT="$(
    aws ssm get-command-invocation \
      --command-id "$COMMAND_ID" \
      --instance-id "$INSTANCE_ID" \
      --region "$AWS_REGION" \
      --query '{Status:Status,ResponseCode:ResponseCode,Stdout:StandardOutputContent,Stderr:StandardErrorContent}' \
      --output json
  )"
  STATUS="$(printf "%s" "$RESULT" | python3 -c 'import json,sys; print(json.load(sys.stdin)["Status"])')"
  case "$STATUS" in
    Pending|InProgress|Delayed)
      sleep 5
      ;;
    *)
      printf "%s\n" "$RESULT"
      [ "$STATUS" = "Success" ]
      break
      ;;
  esac
done

if [ "$MODE" = "enable" ]; then
  EXPECTED_DEPLOY_COMMIT="$EXPECTED_DEPLOY_COMMIT" \
    API_BASE_URL="$API_BASE_URL" \
    STOREFRONT_URL="$STOREFRONT_URL" \
    scripts/check-ecpay-sandbox-readiness.sh
fi
