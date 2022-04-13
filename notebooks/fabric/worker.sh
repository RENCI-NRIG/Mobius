#!/bin/bash
echo "192.168.1.2   poseidon-submit" >> /etc/hosts

# this script should run as root

yum -y update
yum groupinstall -y "Development Tools"
yum install -y wget

######################
### INSTALL CONDOR ###
######################

curl -fsSL https://get.htcondor.org | sudo /bin/bash -s -- --no-dry-run

cat << EOF > /etc/condor/config.d/50-main.config
use feature : GPUs
DAEMON_LIST = MASTER, STARTD
CONDOR_HOST = poseidon-submit 
USE_SHARED_PORT = TRUE
NETWORK_INTERFACE = 192.168.1.*
# the nodes have shared filesystem
UID_DOMAIN = \$(CONDOR_HOST)
TRUST_UID_DOMAIN = TRUE
FILESYSTEM_DOMAIN = \$(FULL_HOSTNAME)
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
SLOT_TYPE_1 = cpus=100%,disk=100%,swap=100%,gpus=100%
SLOT_TYPE_1_PARTITIONABLE = TRUE
NUM_SLOTS = 1
NUM_SLOTS_TYPE_1 = 1
EOF

condor_store_cred -f /etc/condor/pool_password -p p0s31d0n

systemctl enable condor
systemctl restart condor

##########################
### INSTALL SINGULARITY ##
##########################

export VERSION=3.8.7 && # adjust this as necessary \
    wget https://github.com/apptainer/singularity/releases/download/v${VERSION}/singularity-${VERSION}.tar.gz && \
    tar -xzf singularity-${VERSION}.tar.gz && \
    rm singularity-${VERSION}.tar.gz && \
    cd singularity-${VERSION}

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

