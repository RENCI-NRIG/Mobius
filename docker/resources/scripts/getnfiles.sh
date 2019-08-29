#!/bin/bash
for i in `seq 1 ${1}`;
do 
wget ftp://${2}:${3}@${4}/${5}
done
