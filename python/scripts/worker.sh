#!/bin/bash

# this script should run as root

yum -y update
yum install -y gcc gcc-c++ make libarchive-devel

######################
### INSTALL CONDOR ###
######################

rpm --import https://research.cs.wisc.edu/htcondor/yum/RPM-GPG-KEY-HTCondor
wget -O /etc/yum.repos.d/htcondor-previous-rhel7.repo https://research.cs.wisc.edu/htcondor/yum/repo.d/htcondor-previous-rhel7.repo
yum install -y condor-all

cat << EOF > /etc/condor/config.d/50-main.config
DAEMON_LIST = MASTER, STARTD

CONDOR_HOST=`grep -oP '\w*master.*' /etc/hosts`

USE_SHARED_PORT = TRUE

NETWORK_INTERFACE=

# the nodes have shared filesystem
UID_DOMAIN = \$(CONDOR_HOST)
TRUST_UID_DOMAIN = TRUE
FILESYSTEM_DOMAIN = \$(FULL_HOSTNAME)

# Schedd and Negotiator run more often
NEGOTIATOR_INTERVAL=45
NEGOTIATOR_UPDATE_AFTER_CYCLE= TRUE

#--     Authentication settings
SEC_PASSWORD_FILE = /etc/condor/pool_password
SEC_DEFAULT_AUTHENTICATION = REQUIRED
SEC_DEFAULT_AUTHENTICATION_METHODS = FS,PASSWORD
SEC_READ_AUTHENTICATION = OPTIONAL
SEC_CLIENT_AUTHENTICATION = OPTIONAL
SEC_ENABLE_MATCH_PASSWORD_AUTHENTICATION = TRUE
DENY_WRITE = anonymous@*
DENY_ADMINISTRATOR = anonymous@*
DENY_DAEMON = anonymous@*
DENY_NEGOTIATOR = anonymous@*
DENY_CLIENT = anonymous@*

#--     Privacy settings
SEC_DEFAULT_ENCRYPTION = OPTIONAL
SEC_DEFAULT_INTEGRITY = REQUIRED
SEC_READ_INTEGRITY = OPTIONAL
SEC_CLIENT_INTEGRITY = OPTIONAL
SEC_READ_ENCRYPTION = OPTIONAL
SEC_CLIENT_ENCRYPTION = OPTIONAL

#-- With strong security, do not use IP based controls
HOSTALLOW_WRITE = *
ALLOW_NEGOTIATOR = *

# dynamic slots
SLOT_TYPE_1 = cpus=100%,disk=100%,swap=100%
SLOT_TYPE_1_PARTITIONABLE = TRUE
NUM_SLOTS = 1
NUM_SLOTS_TYPE_1 = 1
EOF

condor_store_cred -f /etc/condor/pool_password -p c454_c0nd0r_p00l

systemctl enable condor
systemctl restart condor

##########################
### INSTALL SINGULARITY ##
##########################

SINGULARITY_VERSION=2.6.0
parent_dir=`pwd`
wget https://github.com/sylabs/singularity/releases/download/${SINGULARITY_VERSION}/singularity-${SINGULARITY_VERSION}.tar.gz
tar xvf singularity-${SINGULARITY_VERSION}.tar.gz
cd singularity-${SINGULARITY_VERSION}
./configure --prefix=/usr/local
make && make install
cd $parent_dir
rm -r singularity-${SINGULARITY_VERSION}
rm singularity-${SINGULARITY_VERSION}.tar.gz

##########################
### INSTALL DOCKER      ##
##########################
yum install -y yum-utils \
  device-mapper-persistent-data \
  lvm2

yum-config-manager \
    --add-repo \
    https://download.docker.com/linux/centos/docker-ce.repo

yum install -y docker-ce docker-ce-cli containerd.io

groupadd docker
usermod -aG docker condor

systemctl enable docker
systemctl restart docker

#######################
### SETUP NFS ACCESS ##
#######################
yum install -y nfs-utils
mkdir -p /nfs/shared
echo "storage:/nfs/shared  /nfs/shared   nfs      defaults  0     0" >> /etc/fstab
mount -a

#######################
### SETUP LDM USER ####
#######################

groupadd casa
useradd -c "" -d /home/ldm -G casa,docker ldm
