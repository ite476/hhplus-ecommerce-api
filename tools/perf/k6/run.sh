#!/usr/bin/env bash
set -euo pipefail

SCENARIO="${1:-smoke}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
VU="${VU:-5}"
DURATION="${DURATION:-2m}"
INFLUX_URL="${INFLUX_URL:-}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT_PATH="${SCRIPT_DIR}/scenarios/${SCENARIO}.js"
RESULTS_DIR="${SCRIPT_DIR}/results"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
SUMMARY_FILE="${RESULTS_DIR}/${SCENARIO}-${TIMESTAMP}.json"

if [[ ! -f "${SCRIPT_PATH}" ]]; then
  echo "Scenario script not found: ${SCRIPT_PATH}" >&2
  exit 1
fi

mkdir -p "${RESULTS_DIR}"

echo "Running k6 scenario: ${SCENARIO}"
echo "BASE_URL=${BASE_URL} VU=${VU} DURATION=${DURATION} INFLUX_URL=${INFLUX_URL}"

k6 run \
  -e BASE_URL="${BASE_URL}" \
  -e VU="${VU}" \
  -e DURATION="${DURATION}" \
  --summary-export "${SUMMARY_FILE}" \
  $( [[ -n "${INFLUX_URL}" ]] && echo "--out influxdb=${INFLUX_URL}" ) \
  "${SCRIPT_PATH}"

echo "Summary exported: ${SUMMARY_FILE}"


