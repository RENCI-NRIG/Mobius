#!/bin/bash

/etc/init.d/rpcbind restart

mkdir /shared
echo '172.16.1.1:/var/nfs /shared nfs vers=3,proto=tcp,sync,hard,intr,nolock,timeo=600,retrans=2,wsize=32768,rsize=32768 0 0' >> /etc/fstab

 while true; do
      echo attemping mount...   
      PING=`ping -c 1 172.16.1.1 > /dev/null 2>&1`
        if [ "$?" = "0" ]; then
           break;
        fi
        sleep 5
    done

sleep 10

mount -a


echo 'export LD_LIBRARY_PATH=/opt/lib' >> /root/.bashrc
echo 'export PATH=$PATH:/opt/bin' >> /root/.bashrc


echo 'export LD_LIBRARY_PATH=/opt/lib' >> /home/adamant/.bashrc
echo 'export PATH=$PATH:/opt/bin' >> /home/adamant/.bashrc

rm /etc/condor/config.d/90-master 
#Setup Condor
echo "" >  /etc/condor/condor_config.local
cat >  /etc/condor/config.d/90-worker << EOF
DAEMON_LIST = MASTER, STARTD
CONDOR_HOST = master.expnet
ALLOW_WRITE = *
HOSTALLOW_READ = *
HOSTALLOW_WRITE = *
ParallelSchedulingGroup     = "workers1"
DedicatedScheduler          = "DedicatedScheduler@master"
STARTD_ATTRS                = \$(STARTD_ATTRS), DedicatedScheduler, ParallelSchedulingGroup 
Scheduler =  "DedicatedScheduler@master"
SUSPEND   = False
CONTINUE  = True
PREEMPT   = False
KILL      = False
WANT_SUSPEND   = False
WANT_VACATE    = False
RANK      = Scheduler =?= \$(DedicatedScheduler)
UID_DOMAIN=adamant
EOF

# wait until the master is pingable
UNPINGABLE=true
for ((i=0;i<60;i+=1));
do
      echo "testing ping, try: $i " >> /tmp/bootscript.out
      PING=`ping -c 3 master > /dev/null 2>&1`
      if [ "$?" = "0" ]; then
           UNPINGABLE=false
           break
       fi
       sleep 10
done

#retry condor until successful
CONDOR_UP=false
for ((i=0;i<100;i+=1));
do
       echo 'condor_status' >> /tmp/bootscript.out
      condor_status >> /tmp/bootscript.out
      if [ "$?" = "0" ]; then
           CONDOR_UP=true
           break
       fi
       sleep 10
done
echo  "Restarting Condorm try: $i " >> /tmp/bootscript.out
echo '/etc/init.d/condor restart' >> /tmp/bootscript.out
/etc/init.d/condor restart