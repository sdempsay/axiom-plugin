#!/bin/sh
set -e
if [ -f target/axiom/catalog.yaml ]; then
  echo "Did not expect a stamped catalog when the source file is missing"
  exit 1
fi
echo "Optional catalog skipped"
