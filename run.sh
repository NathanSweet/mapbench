#!/usr/bin/env bash
set -e
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
cd $SCRIPT_DIR

#rm -rf charts/results
#mkdir charts/results

args="-f 6 -wi 1000 -i 15 -t 8 -w 6s -r 6s -bs 750 -rf csv -rff" # ~35 min
#args="-f 3 -wi 250 -i 15 -t 8 -w 6s -r 6s -bs 250 -rf csv -rff" # ~3.5 min
#args="-f 1 -wi 50 -i 6 -t 4 -w 1s -r 1s -bs 50 -rf csv -rff" # ~25 sec
jmh="$JAVA_HOME/bin/java -cp target/classes;lib/* com.esotericsoftware.mapbench.Benchmarks $args"

set -x

$jmh charts/results/contains.csv ContainsBenchmark
$jmh charts/results/add.csv AddBenchmark
$jmh charts/results/remove.csv RemoveBenchmark

cd charts/results
find ../*.r -not -path ../common.r -type f -exec echo "{}:" \; -exec Rscript  {} \;
