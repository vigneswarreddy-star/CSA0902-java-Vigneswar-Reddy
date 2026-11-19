#!/bin/bash
# Runs the three versions on the clean and the failing input, then compares the reports.
set -u
cd "$(dirname "$0")"
java -cp . GenerateStores
for v in ReportManualNoFinally ReportManual ReportTwr; do
  echo "== $v on data_ok"; java -cp . $v data_ok results/report_ok_$v.txt 2>&1 | grep -v JAVA_TOOL
  echo "== $v on data_fail"; java -cp . $v data_fail results/report_fail_$v.txt 2>&1 | grep -v JAVA_TOOL
done
echo "== same report for clean input?"
for v in ReportManual ReportTwr; do cmp -s results/report_ok_ReportManualNoFinally.txt results/report_ok_$v.txt && echo "$v identical to version 1" || echo "$v DIFFERS"; done
