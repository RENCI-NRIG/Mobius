#!/bin/bash

echo 'export LD_LIBRARY_PATH=/opt/lib' >> /root/.bashrc
echo 'export PATH=$PATH:/opt/bin' >> /root/.bashrc

echo '/var/nfs *(rw,sync,no_root_squash)' >> /etc/exports
/etc/init.d/rpcbind restart
/etc/init.d/nfs restart

mkdir /shared
echo '172.16.1.1:/var/nfs /shared nfs vers=3,proto=tcp,sync,hard,intr,nolock,timeo=600,retrans=2,wsize=32768,rsize=32768 0 0' >> /etc/fstab

mount -a

sleep 10

echo 'export LD_LIBRARY_PATH=/opt/lib' >> /root/.bash_profile
echo 'export PATH=$PATH:/opt/bin' >> /root/.bash_profile


echo 'export LD_LIBRARY_PATH=/opt/lib' >> /home/adamant/.bashrc
echo 'export PATH=$PATH:/opt/bin' >> /home/adamant/.bashrc

rm /etc/condor/config.d/90-master 
echo "" >  /etc/condor/condor_config.local
cat >  /etc/condor/config.d/90-master << EOF
DAEMON_LIST = COLLECTOR, MASTER, NEGOTIATOR, SCHEDD
CONDOR_HOST = master.expnet
ALLOW_WRITE = *
HOSTALLOW_READ = *
HOSTALLOW_WRITE = *
UID_DOMAIN=adamant
EOF
 
/etc/init.d/condor restart


workers=1

let reqslots=workers*4

READY=false
for ((i=0;i<60;i+=1));
do
      numslots=`condor_status |grep Total |tail -1 | awk '{print $2}'`
      if [ $numslots -eq $reqslots ]; then
           READY=true
           break
       fi
       sleep 10
done

echo 'READY' > /tmp/status

