#!/bin/sh
set -e
stamped="target/axiom/catalog.yaml"
test -f "$stamped" || { echo "Missing stamped catalog $stamped"; exit 1; }
attached=$(ls target/catalog-fixture-*-agent-catalog.yaml)
test -f "$attached" || { echo "Missing attached classifier yaml"; exit 1; }
grep -q "version:" "$stamped" || { echo "Stamped catalog missing version"; exit 1; }
grep -q "id: external_failure" "$stamped" || { echo "Stamped catalog missing intent"; exit 1; }
test -f "target/axiom/report.md" || { echo "Missing report.md"; exit 1; }
test -f "target/classes/META-INF/axiom/catalog.yaml" || { echo "Missing embedded META-INF catalog"; exit 1; }
examples=$(ls target/catalog-fixture-*-agent-catalog-examples.zip)
test -f "$examples" || { echo "Missing attached examples zip"; exit 1; }
echo "Fixture published $attached and $examples"
