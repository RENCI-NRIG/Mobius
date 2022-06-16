#!/bin/bash

# this script should run as root

yum -y update
yum install -y nfs-utils

systemctl enable nfs-server
systemctl start nfs-server

mkdir -p /nfs/shared
chown -R nfsnobody:nfsnobody /nfs
chmod -R 755 /nfs

echo "/nfs/shared REPLACE(rw,sync,no_subtree_check)" >> /etc/exports
exportfs -a

groupadd casa
useradd -c "" -d /home/ldm -G casa ldm

mkdir /nfs/shared/ldm
chown -R ldm:ldm /nfs/shared/ldm
chmod -R 755 /nfs/shared/ldm
