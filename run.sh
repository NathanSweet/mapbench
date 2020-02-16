#!/usr/bin/env bash
set -e
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
cd $SCRIPT_DIR

# runs benchmarks and makes charts
# run deps.sh and build.sh first

#rm -rf charts/results
#mkdir charts/results

args="-f 1 -wm BULK_INDI -wi 6 -wbs 200 -i 20 -t 10 -w 6s -r 6s -bs 400 -rf csv -rff"
#args="-f 4 -wi 1000 -i 20 -t 11 -w 6s -r 6s -bs 500 -rf csv -rff"
#args="-f 2 -wi 2500 -i 400 -t 11 -w 16s -r 16s -bs 1000 -rf csv -rff"
jmh="$JAVA_HOME/bin/java -cp target/classes;lib/* com.esotericsoftware.mapbench.Benchmarks $args"

set -x

$jmh charts/results/remove.csv RemoveBenchmark
$jmh charts/results/add.csv AddBenchmark
$jmh charts/results/contains.csv ContainsBenchmark

cd charts/results
find ../*.r -not -path ../common.r -type f -exec echo "{}:" \; -exec Rscript  {} \;
