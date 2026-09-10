#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_JAR="${1:-$SCRIPT_DIR/target/evserver.jar}"
LIB_DIR="${2:-$SCRIPT_DIR/target/lib}"
SERVICE_FILE="${3:-$SCRIPT_DIR/evserver.service}"

if [[ ! -f "$SOURCE_JAR" ]]; then
  echo "JAR not found: $SOURCE_JAR" >&2
  echo "Run 'mvn package' first, or pass the JAR path as the first argument." >&2
  exit 1
fi

if [[ ! -d "$LIB_DIR" ]]; then
  echo "Dependency lib directory not found: $LIB_DIR" >&2
  echo "Run 'mvn package' first, or pass the lib directory as the second argument." >&2
  exit 1
fi

if [[ ! -f "$SERVICE_FILE" ]]; then
  echo "Service file not found: $SERVICE_FILE" >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "Java is not installed or is not on PATH." >&2
  exit 1
fi

sudo install -d /opt/evserver /var/lib/evserver/data
sudo cp "$SOURCE_JAR" /opt/evserver/
sudo cp -a "$LIB_DIR" /opt/evserver/
sudo cp "$SERVICE_FILE" /etc/systemd/system/evserver.service
sudo systemctl daemon-reload
sudo systemctl enable --now evserver

sudo systemctl status evserver --no-pager || true

echo "Install complete. Data directory: /var/lib/evserver/data"
echo "Health check: curl http://localhost:8080/health"
