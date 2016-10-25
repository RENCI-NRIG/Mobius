#!/bin/bash

/etc/init.d/rpcbind restart

echo 'export LD_LIBRARY_PATH=/opt/lib' >> /root/.bashrc
echo 'export PATH=$PATH:/opt/bin' >> /root/.bashrc


echo 'export LD_LIBRARY_PATH=/opt/lib' >> /home/adamant/.bashrc
echo 'export PATH=$PATH:/opt/bin' >> /home/adamant/.bashrc
