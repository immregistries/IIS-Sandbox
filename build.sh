#!/bin/bash

set -e
set -o pipefail

DEPENDENCIES_LOCATION="$(mktemp -d)"

./dependencies.sh build "$DEPENDENCIES_LOCATION"

mvn clean install