#!/bin/bash
# Recreates the blocking-file test: three valid files plus one named pipe (FIFO) that never gets data.
set -u
cd "$(dirname "$0")"
rm -rf data_fifo && mkdir data_fifo
cp data/score_00001.txt data/score_00002.txt data/score_00003.txt data_fifo/
mkfifo data_fifo/score_00004.txt
printf 'score_00001.txt\nscore_00002.txt\nscore_00003.txt\nscore_00004.txt\n' > manifest_fifo.txt
echo "Run: timeout 20 java -cp . ResultProcessor data_fifo manifest_fifo.txt 2 0 3   (expect exit 124: the JVM does not exit)"
