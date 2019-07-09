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
DAEMON_LIST = MASTER, COLLECTOR, NEGOTIATOR, SCHEDD
CONDOR_HOST=`hostname` 
USE_SHARED_PORT = TRUE
NETWORK_INTERFACE = 
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

##############################################
### INSTALL CONTAINERS ##
##############################################

docker pull casaelyons/nowcastcontainer
docker save -o /nfs/shared/nowcastcontainer.tar casaelyons/nowcastcontainer
chmod 644 /nfs/shared/nowcastcontainer.tar
docker rmi casaelyons/nowcastcontainer

#######################
### SETUP LDM USER ####
#######################

groupadd casa
useradd -c "" -d /home/ldm -G casa,docker ldm
/bin/su - ldm -c "/usr/bin/wget https://emmy8.casa.umass.edu/dynamoNowcast/ldm-6.11.7.tar.gz"
/bin/su - ldm -c "/bin/tar -xzf ldm-6.11.7.tar.gz"
/bin/su - ldm -c "cd /home/ldm/ldm-6.11.7; ./configure --disable-root-actions --prefix=/home/ldm; /usr/bin/make; /usr/bin/make install"
cd /home/ldm/ldm-6.11.7
/usr/bin/make root-actions
/bin/su - ldm -c "/bin/echo export LDMHOME=/home/ldm >> /home/ldm/.bashrc";
/bin/su - ldm -c "/bin/echo export PATH=/home/ldm/bin:\$PATH >> /home/ldm/.bashrc";
/bin/su - ldm -c "/usr/bin/wget https://emmy8.casa.umass.edu/dynamoNowcast/ldmconfig_stitch.tar"
/bin/su - ldm -c "/bin/mv ldmconfig_stitch.tar etc/; cd etc/; /bin/tar -xf ldmconfig_stitch.tar"
/bin/su - ldm -c "/bin/echo export NOWCAST_WORKFLOW_DIR=/home/ldm/nowcastworkflow >> /home/ldm/.bashrc";
/bin/su - ldm -c "/bin/echo export PERL5LIB=/home/ldm/nowcastworkflow/perl:\$PERL5LIB >> /home/ldm/.bashrc";
/bin/su - ldm -c "/usr/bin/git clone https://github.com/CASAelyons/nowcastworkflow.git"
/bin/su - ldm -c "/bin/mkdir /home/ldm/nowcastworkflow/bin; /usr/bin/wget https://emmy8.casa.umass.edu/dynamoNowcast/casa_image.tar; /bin/mv casa_image.tar /home/ldm/nowcastworkflow/bin"
/bin/su - ldm -c "/bin/mkdir /home/ldm/data;  /bin/mkdir /home/ldm/data/forecast; /bin/mkdir /home/ldm/nowcastworkflow/output"
/bin/su - ldm -c "export PERL5LIB=/home/ldm/nowcastworkflow/perl:\$PERL5LIB; cd /home/ldm/nowcastworkflow/perl; /usr/bin/perl nowcastv2_mon.pl /home/ldm/data/forecast"
/bin/su - ldm -c "export PERL5LIB=/home/ldm/nowcastworkflow/perl:\$PERL5LIB; cd /home/ldm/nowcastworkflow/perl; /usr/bin/perl post_nowcastv2.pl /home/ldm/nowcastworkflow/output"
/bin/su - ldm -c "/home/ldm/bin/ldmadmin mkqueue"
/bin/su - ldm -c "/home/ldm/bin/ldmadmin start"
