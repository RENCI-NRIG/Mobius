#!/bin/bash
echo $(date)
for i in `seq 1 ${1}`;
do
   T="0"
   for j in `seq 1 5`;
        do
        P=$(top -bn1 | grep '%Cpu(s)' | awk -F',' '{printf "%.4f", $4}')
        T=$(echo "100 - $P + $T" | bc -l)
        done
    echo $(date)>> out.txt
    echo "$T*4/5" | bc -l >>out.txt
    echo "$T*4/5" | bc -l
done

echo $(date)
