#!/bin/sh
set -e
stamped="target/axiom/catalog.yaml"
test -f "$stamped" || { echo "Missing stamped catalog $stamped"; exit 1; }
attached=$(ls target/catalog-fixture-*-agent-catalog.yaml)
test -f "$attached" || { echo "Missing attached classifier yaml"; exit 1; }
grep -q "version:" "$stamped" || { echo "Stamped catalog missing version"; exit 1; }
grep -q "id: external_failure" "$stamped" || { echo "Stamped catalog missing intent"; exit 1; }
test -f "target/axiom/report.md" || { echo "Missing report.md"; exit 1; }
echo "Fixture published $attached"
