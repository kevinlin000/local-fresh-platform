#!/usr/bin/env sh
set -eu

PACKAGE_DIR="${1:-}"

fail() {
  printf "FAIL: %s\n" "$1" >&2
  exit 1
}

pass() {
  printf "PASS: %s\n" "$1"
}

usage() {
  printf "Usage: %s <release-package-dir>\n" "$0" >&2
  printf "Example: %s output/backend-release/<commit>\n" "$0" >&2
}

if [ -z "$PACKAGE_DIR" ]; then
  usage
  exit 2
fi

if [ ! -d "$PACKAGE_DIR" ]; then
  fail "package directory not found: $PACKAGE_DIR"
fi

RELEASE_DIR=""
if [ -f "$PACKAGE_DIR/SHA256SUMS" ]; then
  RELEASE_DIR="$PACKAGE_DIR"
else
  for candidate in "$PACKAGE_DIR"/*/SHA256SUMS "$PACKAGE_DIR"/*/*/SHA256SUMS; do
    if [ -f "$candidate" ]; then
      RELEASE_DIR="$(CDPATH= cd -- "$(dirname -- "$candidate")" && pwd)"
      break
    fi
  done
fi

if [ -z "$RELEASE_DIR" ]; then
  fail "could not find SHA256SUMS under $PACKAGE_DIR"
fi

cd "$RELEASE_DIR"

for required in release.env release-manifest.txt SHA256SUMS ec2-deploy-commands.txt; do
  [ -f "$required" ] || fail "missing required file: $required"
done

for required_template in \
  deploy-templates/local-fresh-server.env.example \
  deploy-templates/local-fresh-server.service \
  deploy-templates/nginx-localfresh-demo.conf; do
  [ -f "$required_template" ] || fail "missing deploy template: $required_template"
done

source_commit="$(sed -n 's/^SOURCE_COMMIT=//p' release.env)"
backend_jar="$(sed -n 's/^BACKEND_JAR=//p' release.env)"

[ -n "$source_commit" ] || fail "release.env is missing SOURCE_COMMIT"
[ -n "$backend_jar" ] || fail "release.env is missing BACKEND_JAR"
[ -f "$backend_jar" ] || fail "backend jar listed in release.env is missing: $backend_jar"

case "$backend_jar" in
  *"$source_commit"*) ;;
  *) fail "backend jar name does not include SOURCE_COMMIT=$source_commit: $backend_jar" ;;
esac

grep -q "SOURCE_COMMIT=$source_commit" release-manifest.txt || fail "manifest commit does not match release.env"
grep -q "BACKEND_JAR=$backend_jar" release-manifest.txt || fail "manifest jar does not match release.env"
grep -q "PAYMENT_CALLBACK_PATH=/payment/callback" release-manifest.txt || fail "manifest is missing payment callback path"

if command -v sha256sum >/dev/null 2>&1; then
  sha256sum -c SHA256SUMS
else
  shasum -a 256 -c SHA256SUMS
fi

pass "backend release package verified at $RELEASE_DIR"
