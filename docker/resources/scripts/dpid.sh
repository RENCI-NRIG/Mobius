#!/bin/bash
dpid=$(sudo ovs-ofctl -O OpenFlow13 show br0| grep "dpid:" |cut -d: -f3)
# #1 the ip and port  number of controller Restful API $2 the id of logRouter
manageif=$(ifconfig -a | grep -B1 "inet addr:10." | awk '$1!="inet" && $1!="--" {print $1}')
num=$(ifconfig |grep  "ens"|grep -v "$manageif"|grep -c "ens")
#curl -X POST -d '{"routerid":"'"$2"'","interfaces":"'"$num"'"}' $1/logRouter/$dpid
echo "$num"' '"$dpid"
