#!/usr/bin/env bash
# Tests, builds and deploys the backend jar to the VM configured in .env.
set -euo pipefail
cd "$(dirname "$0")"

[ -f .env ] || { echo "Missing .env (copy .env.example)"; exit 1; }
set -a; . ./.env; set +a
: "${VM_HOST:?}" "${VM_USER:?}" "${SSH_KEY_PATH:?}"
SSH_KEY_PATH="${SSH_KEY_PATH/#\~/$HOME}"
[ -n "${BUILD_JAVA_HOME:-}" ] && export JAVA_HOME="$BUILD_JAVA_HOME"

SSH_OPTS=(-i "$SSH_KEY_PATH" -o BatchMode=yes -o StrictHostKeyChecking=accept-new)
REMOTE="$VM_USER@$VM_HOST"

echo "==> Test and package"
./mvnw -B -q clean verify

echo "==> Upload jar"
scp "${SSH_OPTS[@]}" target/salary-backend.jar "$REMOTE:/opt/salary/salary-backend.jar.new"

echo "==> Restart service"
ssh "${SSH_OPTS[@]}" "$REMOTE" \
  "mv /opt/salary/salary-backend.jar.new /opt/salary/salary-backend.jar && sudo systemctl restart salary-backend"

echo "==> Wait for health"
for attempt in $(seq 1 30); do
  if ssh "${SSH_OPTS[@]}" "$REMOTE" "curl -fs http://127.0.0.1:8080/api/actuator/health | grep -q UP"; then
    echo "Deployed: http://$VM_HOST/api/actuator/health is UP"
    exit 0
  fi
  sleep 3
done
echo "Health check failed. Recent logs:"
ssh "${SSH_OPTS[@]}" "$REMOTE" "sudo journalctl -u salary-backend -n 40 --no-pager" || true
exit 1
