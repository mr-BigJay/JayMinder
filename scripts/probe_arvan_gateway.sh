#!/usr/bin/env bash
# Probes ArvanCloud AI Gateway using ARVAN_API_KEY from the environment (never commit keys).
set -euo pipefail

BASE_URL="${ARVAN_BASE_URL:-https://api.arvancloudai.ir/v1}"
BASE_URL="${BASE_URL%/}"
API_KEY="${ARVAN_API_KEY:-}"

if [[ -z "$API_KEY" ]]; then
  echo "ERROR: Set ARVAN_API_KEY in the environment." >&2
  exit 1
fi

auth_header=(-H "Authorization: Bearer ${API_KEY}" -H "Accept: application/json")

echo "=== GET ${BASE_URL}/models ==="
models_body="$(mktemp)"
models_code="$(curl -sS -o "$models_body" -w "%{http_code}" "${auth_header[@]}" "${BASE_URL}/models")"
echo "HTTP ${models_code}"
cat "$models_body"
echo

if [[ "$models_code" != "200" ]]; then
  echo "Models request failed." >&2
  exit 2
fi

MODEL="${ARVAN_MODEL:-}"
if [[ -z "$MODEL" ]]; then
  MODEL="$(python3 - <<'PY' "$models_body"
import json, sys
data = json.load(open(sys.argv[1]))
items = data.get("data") or []
print(items[0]["id"] if items else "")
PY
)"
fi

if [[ -z "$MODEL" ]]; then
  echo "ERROR: No models returned; set ARVAN_MODEL manually." >&2
  exit 3
fi

echo "=== Using model id: ${MODEL} ==="

echo "=== POST ${BASE_URL}/chat/completions ==="
chat_payload="$(cat <<EOF
{"model":"${MODEL}","messages":[{"role":"user","content":"سلام"}]}
EOF
)"
chat_body="$(mktemp)"
chat_code="$(curl -sS -o "$chat_body" -w "%{http_code}" \
  "${auth_header[@]}" \
  -H "Content-Type: application/json" \
  -d "$chat_payload" \
  "${BASE_URL}/chat/completions")"
echo "HTTP ${chat_code}"
cat "$chat_body"
echo

if [[ "$chat_code" != "200" ]]; then
  echo "=== POST ${BASE_URL}/responses (fallback probe) ==="
  responses_code="$(curl -sS -o "$chat_body" -w "%{http_code}" \
    "${auth_header[@]}" \
    -H "Content-Type: application/json" \
    -d "$chat_payload" \
    "${BASE_URL}/responses")" || true
  echo "HTTP ${responses_code}"
  cat "$chat_body"
  echo
fi

rm -f "$models_body" "$chat_body"
