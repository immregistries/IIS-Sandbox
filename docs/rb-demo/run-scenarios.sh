#!/bin/bash
# Runs the four RB demo scenarios against a local IIS Sandbox using the smm-tester SendHl7 CLI.
# 1) sends each scenario's VXU so the patient exists, 2) sends a Z45 query built from the VXU.
# Output: docs/rb-demo/rsp/<scenario>.txt (request and response)
#
# usage: run-scenarios.sh [soap-url]
#   SENDHL7 = path of the smm-tester sendhl7.sh wrapper (java -cp smm-web WEB-INF/lib ... SendHl7)
HERE="$(cd "$(dirname "$0")" && pwd)"
URL="${1:-http://localhost:8080/iis-sandbox/soap}"
SENDHL7="${SENDHL7:-$HOME/projects/_coder/20261029-rb-demo/smm-cli/sendhl7.sh}"
USER=rbdemo; PASS=rbdemo; FAC=RBDEMO
mkdir -p "$HERE/rsp"
run() { # name vxu conditions
  "$SENDHL7" "$URL" $USER $PASS $FAC "$HERE/vxu/vxu-$2.hl7" > /dev/null
  "$SENDHL7" "$URL" $USER $PASS $FAC "$HERE/vxu/vxu-$2.hl7" QBP-Z45 "$3" > "$HERE/rsp/$1.txt" 2>&1
  echo "== $1 ($3)"; sed -n '/--- RESPONSE/,$p' "$HERE/rsp/$1.txt" | grep -E '^(MSH|MSA|ERR|QAK)|30982-3|30956-7|59783-1|59779-9|99VRC|Earliest|Latest' 
}
run pregnancy-tdap        pregnant           "007=Y,170=20260701"
run rsv-infant            rsv-infant         "278=Y"
run healthcare-worker     healthcare-worker  "055=Y"
run unknown-code          unknown-code       "055=Y,999=Y"
