#!/usr/bin/env bash
# Falla si el informe JSON de ZAP tiene alertas de riesgo alto (riskcode 3). RNF-SEG-04.
set -euo pipefail
altos=$(jq '[.site[]?.alerts[]? | select(.riskcode == "3")] | length' "$1")
echo "Alertas altas en $1: $altos"
jq -r '.site[]?.alerts[]? | select(.riskcode == "3") | "- \(.name) (\(.count) instancias)"' "$1"
[ "$altos" -eq 0 ]
