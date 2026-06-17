#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

run_step() {
  name="$1"
  shift
  printf "\n==> %s\n" "$name"
  "$@"
}

run_step "Backend Maven tests" \
  sh -c "cd '$ROOT_DIR/backend-environment/local-fresh-backend' && mvn test"

if [ -f "$ROOT_DIR/frontend-environment/local-fresh-admin/package-lock.json" ]; then
  run_step "Admin frontend build" \
    sh -c "cd '$ROOT_DIR/frontend-environment/local-fresh-admin' && npm ci && npm run build"
fi

if [ -f "$ROOT_DIR/frontend-environment/local-fresh-user/pnpm-lock.yaml" ]; then
  run_step "User frontend build" \
    sh -c "cd '$ROOT_DIR/frontend-environment/local-fresh-user' && corepack pnpm@10.25.0 install --frozen-lockfile && corepack pnpm@10.25.0 run build"
fi

printf "\nAll pre-commit checks passed.\n"
