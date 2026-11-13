#!/usr/bin/env bash
# Runs a backend Spring Boot module locally with the variables from the root .env loaded.
# Usage: scripts/run-backend.sh <photo-api|image-worker> [extra maven args]
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env"
MODULE="${1:-}"

case "${MODULE}" in
  photo-api|image-worker) ;;
  *)
    echo "Usage: $0 <photo-api|image-worker> [extra maven args]" >&2
    exit 1
    ;;
esac
shift

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}. Copy .env.example to .env and adjust it first." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

echo "Loaded ${ENV_FILE}; starting ${MODULE}..."
cd "${ROOT_DIR}/backend"
exec ./mvnw spring-boot:run -pl "${MODULE}" "$@"
