#!/bin/bash
num=$(ifconfig |grep  "eno"|grep -v "eno0"|grep -c "eno1")
echo $num
