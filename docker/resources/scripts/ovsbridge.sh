#!/bin/bash

#apt-get update;
#apt-get -y install openvswitch-switch;
#apt-get -y install iperf;
#/etc/init.d/neuca stop;
#check if there is a bridge, if yes skip
# if a new interface is not added to the ovsbridge, then add it and set controller again

string=$(ifconfig -a);
if [[ $string == *"br0"* ]]; then
    :
else
  sudo ovs-vsctl add-br br0
  #sudo ovs-vsctl set Bridge br0 protocols=OpenFlow13,OpenFlow15
  #sudo ovs-vsctl set Bridge br0 protocols=OpenFlow10,OpenFlow11,OpenFlow12,OpenFlow13,OpenFlow15
  sudo ovs-vsctl set Bridge br0 protocols=OpenFlow10,OpenFlow11,OpenFlow12,OpenFlow13
  sudo ovs-vsctl set-manager ptcp:6632
  sudo ovs-vsctl set-controller br0 tcp:$1
fi

manageif=$(ifconfig -a| grep -B1 "inet 10.\|inet addr:10." | awk '$1!="inet" && $1!="--" {print $1}')

interfaces=$(ifconfig -a|grep "ens\|eth"|grep -v "$manageif"|sed 's/[ :\t].*//;/^$/d')

brinterfaces=$(sudo ovs-ofctl show br0)
#when using -O OpenFlow13, it failed to show interfaces in ovs-ofctl show br0
vsinterfaces=$(sudo ovs-vsctl show)

vsis=$(sudo ovs-vsctl show|grep -E "Port.*(eth|ens)" |sed 's/Port.*"\(.*\)".*/\1/')

newport=0
while read -r line; do
  if [[ $brinterfaces == *"$line"* ]]; then
  :
  else
    sudo ifconfig $line up
    sudo ifconfig $line 0
    if [[ $vsinterfaces == *"$line"* ]]; then
        sudo ovs-vsctl del-port br0 $line
        sudo ovs-vsctl add-port br0 $line
    else
        sudo ovs-vsctl add-port br0 $line
    fi
  fi
done <<< "$interfaces"

while read -r line; do
  if [[ $interfaces == *"$line"* ]]; then
  :
  else
    sudo ovs-vsctl del-port br0 $line
  fi
done <<< "$vsis"

dpid=$(sudo ovs-ofctl show br0| grep "dpid:" |cut -d: -f3)
echo $dpid



