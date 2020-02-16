#!/usr/bin/env bash
set -e
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
cd $SCRIPT_DIR

# makes charts from the last benchmark run

cd charts/results
find ../*.r -not -path ../common.r -type f -exec echo "{}:" \; -exec Rscript  {} \;
