#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend-environment/local-fresh-backend"
SERVER_DIR="$BACKEND_DIR/local-fresh-server"

COMMIT="$(git -C "$ROOT_DIR" rev-parse --short=12 HEAD)"
BRANCH="$(git -C "$ROOT_DIR" rev-parse --abbrev-ref HEAD)"
OUTPUT_DIR="${OUTPUT_DIR:-$ROOT_DIR/output/backend-release/$COMMIT}"
ALLOW_DIRTY="${ALLOW_DIRTY:-false}"
SKIP_VERIFY="${SKIP_VERIFY:-false}"

if [ "$ALLOW_DIRTY" != "true" ] && [ -n "$(git -C "$ROOT_DIR" status --porcelain)" ]; then
  printf "FAIL: worktree has uncommitted changes. Commit first, or set ALLOW_DIRTY=true for a local dry run.\n" >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR"

if [ "$SKIP_VERIFY" = "true" ]; then
  printf "WARN: SKIP_VERIFY=true, reusing existing backend jar if available.\n"
else
  printf "Running backend verify before packaging release %s\n" "$COMMIT"
  (cd "$BACKEND_DIR" && mvn -pl local-fresh-server -am verify)
fi

SOURCE_JAR="$SERVER_DIR/target/local-fresh-server-1.0-SNAPSHOT.jar"
RELEASE_JAR="$OUTPUT_DIR/local-fresh-server-$COMMIT.jar"
RELEASE_ENV="$OUTPUT_DIR/release.env"
DEPLOY_COMMANDS="$OUTPUT_DIR/ec2-deploy-commands.txt"

if [ ! -f "$SOURCE_JAR" ]; then
  printf "FAIL: backend jar not found at %s\n" "$SOURCE_JAR" >&2
  printf "Run without SKIP_VERIFY=true to build it.\n" >&2
  exit 1
fi

cp "$SOURCE_JAR" "$RELEASE_JAR"

cat > "$RELEASE_ENV" <<EOF
SOURCE_COMMIT=$COMMIT
SOURCE_BRANCH=$BRANCH
BACKEND_JAR=$(basename "$RELEASE_JAR")
BUILT_AT_UTC=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
EOF

cat > "$DEPLOY_COMMANDS" <<EOF
# Replace <ec2-host> and paths with your EC2 values.
scp "$RELEASE_JAR" ubuntu@<ec2-host>:/tmp/$(basename "$RELEASE_JAR")

ssh ubuntu@<ec2-host> '
  set -eu
  sudo install -d /opt/local-fresh/releases/$COMMIT
  sudo mv /tmp/$(basename "$RELEASE_JAR") /opt/local-fresh/releases/$COMMIT/local-fresh-server.jar
  sudo ln -sfn /opt/local-fresh/releases/$COMMIT/local-fresh-server.jar /opt/local-fresh/current.jar
  sudo systemctl set-environment SOURCE_COMMIT=$COMMIT SOURCE_BRANCH=$BRANCH
  sudo systemctl restart local-fresh-server
  sudo systemctl status local-fresh-server --no-pager
'

curl -s https://localfresh-demo.duckdns.org/actuator/info
EXPECTED_DEPLOY_COMMIT=$COMMIT scripts/check-ecpay-sandbox-readiness.sh
EOF

printf "Backend release packaged.\n"
printf "  commit: %s\n" "$COMMIT"
printf "  branch: %s\n" "$BRANCH"
printf "  jar: %s\n" "$RELEASE_JAR"
printf "  metadata: %s\n" "$RELEASE_ENV"
printf "  deploy commands: %s\n" "$DEPLOY_COMMANDS"
