#!/bin/bash
for i in `seq 1 100`;
do 
echo $i
wget ftp://ftpuser:ftp@192.168.20.2/evil.txt
done
