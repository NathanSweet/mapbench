#!/usr/bin/env bash
set -e
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
cd $SCRIPT_DIR

# copies JARs into lib folder for run.sh

mvn dependency:copy-dependencies -DoutputDirectory=lib
