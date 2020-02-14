#!/usr/bin/env bash
set -e
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
cd $SCRIPT_DIR

cd charts/results
find ../*.r -not -path ../common.r -type f -exec echo "{}:" \; -exec Rscript  {} \;
