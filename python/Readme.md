# Table of contents

 - [Description](#descr)
 - [Installation](#install)
   - [Mobius Client](#mbclient)
     - [Usage](#usage1)
   - [Condor Client](#condorclient)
     - [Usage](#usage2)   
   - [Json Data](#json)
   - [Certificates](#certs)
   - [Mobius Client Examples](#mobius_client_examples)
     - [Create a workflow](#createworkflow)
     - [Create a compute node in a workflow](#createcompute)
     - [Create a stitch port in a workflow](#createstitchport)
     - [Get status of a workflow](#getworkflow)
     - [Delete a workflow](#deleteworkflow)
   - [Condor Client Examples](#condor_client_examples)
     - [Create a condor cluster](#create)
     - [Get status of condor cluster](#get)
     - [Delete condor cluster](#delete)
 
# <a name="descr"></a>Description
Python based clients
 - Mobius client to trigger Moobius REST commands.
 - Python client to create Condor clusters by invoking various supported Mobius REST commands.

# <a name="install"></a>Installation
`https://github.com/RENCI-NRIG/Mobius.git`

`cd Mobius/python/`

You are now ready execute python client.

Pre-requisites: requires python 3 or above version and python requests package installed

## <a name="mbclient"></a>Mobius Client
Mobius client to trigger Moobius REST commands.

### <a name="usage1"></a>Usage
```
usage: mobius_client.py [-h] [-s SITE] [-m MOBIUSHOST] -o OPERATION -w
                        WORKFLOWID [-d DATA] [-r RESOURCETYPE] [-t TARGET]

Python client to provision cloud resources by invoking Mobius REST Commands.

optional arguments:
  -h, --help            show this help message and exit
  -s SITE, --site SITE  Site
  -m MOBIUSHOST, --mobiushost MOBIUSHOST
                        Mobius Host e.g. http://localhost:8080/mobius
  -o OPERATION, --operation OPERATION
                        Operation allowed values: post|get|delete; post -
                        provision workflow or compute or storage or
                        stitchport; get - get a workflow; delete - delete a
                        workflow
  -w WORKFLOWID, --workflowId WORKFLOWID
                        workflowId
  -d DATA, --data DATA  data, JSON data to send; if not specified; default
                        data is used; only used with post; must not be
                        specified if target is indicated; must be specified
                        for stitchport
  -r RESOURCETYPE, --resourcetype RESOURCETYPE
                        resourcetype allowed values:
                        workflow|compute|storage|stitchport; only used with
                        post; must be specified
  -t TARGET, --target TARGET
                        target hostname of the server to which to attach
                        storage; only used with resourcetype storage
```
## <a name="condor"></a>Condor Client
Python client to create Condor clusters by invoking various supported Mobius REST commands.

### <a name="usage2"></a>Usage
```
usage: condor_client.py [-h] [-s SITE] [-n WORKERS] [-c COMETHOST] [-t CERT]
                        [-k KEY] [-m MOBIUSHOST] -o OPERATION -w WORKFLOWID
                        [-i IPSTART] [-l LEASEEND] [-d DATADIR]

Python client to create Condor cluster using mobius. Uses master.json,
submit.json and worker.json for compute requests present in data directory
specified. Currently only supports provisioning compute resources. Other
resources can be provisioned via mobius_client. Creates COMET contexts for
Chameleon resources and thus enables exchanging keys and hostnames within
workflow

optional arguments:
  -h, --help            show this help message and exit
  -s SITE, --site SITE  Site at which resources must be provisioned; must be
                        specified for create operation
  -n WORKERS, --workers WORKERS
                        Number of workers to be provisioned; must be specified
                        for create operation
  -c COMETHOST, --comethost COMETHOST
                        Comet Host e.g. https://18.218.34.48:8111/; used only
                        for provisioning resources on chameleon
  -t CERT, --cert CERT  Comet Certificate; used only for provisioning
                        resources on chameleon
  -k KEY, --key KEY     Comet Certificate key; used only for provisioning
                        resources on chameleon
  -m MOBIUSHOST, --mobiushost MOBIUSHOST
                        Mobius Host e.g. http://localhost:8080/mobius
  -o OPERATION, --operation OPERATION
                        Operation allowed values: create|get|delete
  -w WORKFLOWID, --workflowId WORKFLOWID
                        workflowId
  -i IPSTART, --ipStart IPSTART
                        Start IP Address of the range of IPs to be used for
                        VMs; 1st IP is assigned to master and subsequent IPs
                        are assigned to submit node and workers; can be
                        specified for create operation
  -l LEASEEND, --leaseEnd LEASEEND
                        Lease End Time; can be specified for create operation
  -d DATADIR, --datadir DATADIR
                        Data directory where to look for master.json,
                        submit.json and worker.json; must be specified for
                        create operation
```
### <a name="json"></a>JSON Data
Json Data for Master, Submit and Worker Nodes is read from Mobius/python/data directory.

### <a name="certs"></a>Certificates
Example Comet Certficates are present in Mobius/python/certs directory.

### <a name="mobius_client_examples"></a>Mobius Client Examples
#### <a name="createworkflow"></a>Create a workflow
```
python3 mobius_client.py -o post -r workflow -w abcd-1234
```
#### <a name="createcompute"></a>Create a compute node in a workflow
The following example also shows IP address assignment controlled by user.
```
python3 mobius_client.py -o post -r compute -w abcd-1234 -d '{
    "site":"Exogeni:UH (Houston, TX USA) XO Rack",
    "cpus":"4",
    "gpus":"0",
    "ramPerCpus":"3072",
    "diskPerCpus":"19200",
    "hostNamePrefix":"master",
    "ipAddress": "172.16.0.1",
    "coallocate":"true",
    "imageUrl":"http://geni-images.renci.org/images/standard/centos-comet/centos7.4-v1.0.3-comet/centos7.4-v1.0.3-comet.xml",
    "imageHash":"3dd17be8e0c24dd34b4dbc0f0d75a0b3f398c520",
    "imageName":"centos7.4-v1.0.3-comet",
    "leaseEnd":"1557733832",
    "postBootScript":"curl http://geni-images.renci.org/images/cwang/Condor/scripts/exogeni-scripts/master.sh -o /root/master.sh; sh /root/master.sh"
}'

python3 mobius_client.py -o post -r compute -w abcd-1234 -f ../mobius/test/computeMaster.json
```
#### <a name="createstitchport"></a>Create a stitch port in a workflow
```
python3 mobius_client.py -o post -w abcd-1234 -r stitchPort -d '{
     "target":"master0",
     "portUrl":"http://geni-orca.renci.org/owl/uhNet.rdf#UHNet/IBM/G8052/TengigabitEthernet/1/1/ethernet",
     "tag":"2001",
     "stitchIP": "72.16.0.1",
     "bandwidth":"10000000"
}'

python3 mobius_client.py -o post -w abcd-1234 -r stitchPort -f ../mobius/test/stitch.json
```
#### <a name="getworkflow"></a>Get status of a workflow
```
python3 mobius_client.py -o get -w abcd-1234
```
#### <a name="deleteworkflow"></a>Delete a workflow
```
python3 mobius_client.py -o delete -w abcd-1234
```

### <a name="condor_client_examples"></a>Condor Client Examples
#### <a name="create"></a>Create a condor cluster
Create a condor cluster with 1 master, 1 submit and 1 worker node. 
NOTE: Comet context for each node is created and neuca tools are also installed on each node. This results in hostnames and keys to be exchanged between all nodes in condor cluster

##### Chameleon:
- Master, Worker, Submit and Storage nodes on Chameleon (if json for either of the nodes is not present they are not instantiated)
```
python3 condor_client.py  -c https://18.221.238.74:8111/ -t certs/inno-hn_exogeni_net.pem -k certs/inno-hn_exogeni_net.key -s2 Chameleon:CHI@TACC -d2 ./hybrid/chameleon/ -l `date -v +2d +%s` -i2 "192.168.10.5" -o create -w abcd-1114 -n 1
```
##### Exogeni: For controller where COMET is not enabled (EXOSOM)
- Master, Worker, Submit and Storage nodes on Exogeni (if json for either of the nodes is not present they are not instantiated)
- Stitch.json is present in the directory would be used to stitch
```
python3 condor_client.py  -c https://18.221.238.74:8111/ -t certs/inno-hn_exogeni_net.pem -k certs/inno-hn_exogeni_net.key -s1 'Exogeni:UH (Houston, TX USA) XO Rack'  -d1 ./hybrid/exogeni/ -l `date -v +2d +%s` -i1 "172.16.0.1" -o create -w abcd-1114 -n 1
```
##### Hybrid Model: 
- Master, Worker and Storage nodes on Exogeni
- Storage node stitched to UNT and Chameleon
- Storage node acts a forwarder to transfer traffic from Exogeni to Chameleon and viceversa
- Storage node acts a forwarder to transfer traffic from UNT to Exogeni and viceversa
```
python3 condor_client.py  -c https://18.221.238.74:8111/ -t certs/inno-hn_exogeni_net.pem -k certs/inno-hn_exogeni_net.key -s1 'Exogeni:UH (Houston, TX USA) XO Rack'  -d1 ./hybrid/exogeni/  -s2 Chameleon:CHI@TACC -d2 ./hybrid/chameleon/ -l `date -v +2d +%s` -i1 "172.16.0.1" -i2 "192.168.10.5" -o create -w abcd-1114 -n 1
```

#### <a name="get"></a>Get status of condor cluster
```
python3 condor_client.py -o get -w abcd-1114
```

#### <a name="delete"></a>Delete condor cluster
```
python3 condor_client.py -o delete -w abcd-1114 
```
Delete cluster when created by specifying COMET
```
python3 condor_client.py -o delete -w abcd-1114 -c https://18.221.238.74:8111/ -t certs/inno-hn_exogeni_net.pem -k certs/inno-hn_exogeni_net.key
```
