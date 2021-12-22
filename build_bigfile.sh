#!/bin/bash

for i in {1..10000000}; do
    echo "${i}: This is another line ..." >> src/test/resources/bigfile.txt;
done
