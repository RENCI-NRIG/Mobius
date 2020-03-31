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
     - [List workflows](#listworkflow)
   - [Condor Client Examples](#condor_client_examples)
     - [Create a condor cluster](#create)
     - [Get status of condor cluster](#get)
     - [Delete condor cluster](#delete)
     - [List workflows](#list)
 
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
usage: condor_client.py [-h] [-s1 EXOGENISITE] [-s2 CHAMELEONSITE]
                        [-s3 JETSTREAMSITE] [-s4 MOSSITE] [-n1 EXOWORKERS]
                        [-n2 CHWORKERS] [-n3 JTWORKERS] [-n4 MOSWORKERS]
                        [-c COMETHOST] [-t CERT] [-k KEY] [-m MOBIUSHOST] -o
                        OPERATION -w WORKFLOWID [-i1 EXOIPSTART]
                        [-i2 CHIPSTART] [-i3 JTIPSTART] [-i4 MOSIPSTART]
                        [-l LEASEEND] [-d1 EXODATADIR] [-d2 CHDATADIR]
                        [-d3 JTDATADIR] [-d4 MOSDATADIR] [-kh KAFKAHOST]

Python client to create Condor cluster using mobius. Uses master.json,
submit.json and worker.json for compute requests present in data directory
specified. Currently only supports provisioning compute resources. Other
resources can be provisioned via mobius_client. Creates COMET contexts for
Chameleon resources and thus enables exchanging keys and hostnames within
workflow

optional arguments:
  -h, --help            show this help message and exit
  -s1 EXOGENISITE, --exogenisite EXOGENISITE
                        Exogeni Site at which resources must be provisioned;
                        must be specified for create operation
  -s2 CHAMELEONSITE, --chameleonsite CHAMELEONSITE
                        Chameleon Site at which resources must be provisioned;
                        must be specified for create operation
  -s3 JETSTREAMSITE, --jetstreamsite JETSTREAMSITE
                        Jetstream Site at which resources must be provisioned;
                        must be specified for create operation
  -s4 MOSSITE, --mossite MOSSITE
                        Mass Open Cloud Site at which resources must be
                        provisioned; must be specified for create operation
  -n1 EXOWORKERS, --exoworkers EXOWORKERS
                        Number of workers to be provisioned on Exogeni; must
                        be specified for create operation
  -n2 CHWORKERS, --chworkers CHWORKERS
                        Number of workers to be provisioned on Chameleon; must
                        be specified for create operation
  -n3 JTWORKERS, --jtworkers JTWORKERS
                        Number of workers to be provisioned on Jetstream; must
                        be specified for create operation
  -n4 MOSWORKERS, --mosworkers MOSWORKERS
                        Number of workers to be provisioned on Mass Open
                        Cloud; must be specified for create operation
  -c COMETHOST, --comethost COMETHOST
                        Comet Host default(https://comet-
                        hn1.exogeni.net:8111/) used only for provisioning
                        resources on chameleon
  -t CERT, --cert CERT  Comet Certificate default(certs/inno-
                        hn_exogeni_net.pem); used only for provisioning
                        resources on chameleon
  -k KEY, --key KEY     Comet Certificate key default(certs/inno-
                        hn_exogeni_net.key); used only for provisioning
                        resources on chameleon
  -m MOBIUSHOST, --mobiushost MOBIUSHOST
                        Mobius Host e.g. http://localhost:8080/mobius
  -o OPERATION, --operation OPERATION
                        Operation allowed values: create|get|delete
  -w WORKFLOWID, --workflowId WORKFLOWID
                        workflowId
  -i1 EXOIPSTART, --exoipStart EXOIPSTART
                        Exogeni Start IP Address of the range of IPs to be
                        used for VMs; 1st IP is assigned to master and
                        subsequent IPs are assigned to submit node and
                        workers; can be specified for create operation
  -i2 CHIPSTART, --chipStart CHIPSTART
                        Chameleon Start IP Address of the range of IPs to be
                        used for VMs; 1st IP is assigned to master and
                        subsequent IPs are assigned to submit node and
                        workers; can be specified for create operation
  -i3 JTIPSTART, --jtipStart JTIPSTART
                        Jetstream Start IP Address of the range of IPs to be
                        used for VMs; 1st IP is assigned to master and
                        subsequent IPs are assigned to submit node and
                        workers; can be specified for create operation
  -i4 MOSIPSTART, --mosipStart MOSIPSTART
                        Mass Open Cloud Start IP Address of the range of IPs
                        to be used for VMs; 1st IP is assigned to master and
                        subsequent IPs are assigned to submit node and
                        workers; can be specified for create operation
  -l LEASEEND, --leaseEnd LEASEEND
                        Lease End Time; can be specified for create operation
  -d1 EXODATADIR, --exodatadir EXODATADIR
                        Exogeni Data directory where to look for master.json,
                        submit.json and worker.json; must be specified for
                        create operation
  -d2 CHDATADIR, --chdatadir CHDATADIR
                        Chameleon Data directory where to look for
                        master.json, submit.json and worker.json; must be
                        specified for create operation
  -d3 JTDATADIR, --jtdatadir JTDATADIR
                        Jetstream Data directory where to look for
                        master.json, submit.json and worker.json; must be
                        specified for create operation
  -d4 MOSDATADIR, --mosdatadir MOSDATADIR
                        Mass Open Cloud Data directory where to look for
                        master.json, submit.json and worker.json; must be
                        specified for create operation
  -kh KAFKAHOST, --kafkahost KAFKAHOST
                        Kafka Host - monitoring server; must be specified for
                        delete operation    
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
    "imageUrl":"http://geni-images.renci.org/images/kthare10/mobius/mb-centos-7/mb-centos-7.xml",
    "imageName":"mb-centos-7",
    "imageHash":"2dc5f35c91712845f9b6fec6bad1f6f33c64df39",
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
#### <a name="listworkflow"></a>List workflows
```
python3 mobius_client.py -o get
```

### <a name="condor_client_examples"></a>Condor Client Examples
#### <a name="create"></a>Create a condor cluster
Create a condor cluster with 1 master, 1 submit and 1 worker node. 
NOTE: Comet context for each node is created and neuca tools are also installed on each node. This results in hostnames and keys to be exchanged between all nodes in condor cluster

##### Chameleon:
- Master, Worker, Submit and Storage nodes on Chameleon (if json for either of the nodes is not present they are not instantiated)
```
python3 condor_client.py  -s2 Chameleon:CHI@UC -d2 ./chameleon/ -l `date -v +2d +%s` -i2 "192.168.100.5" -o create -w abcd-1114 -n2 1
```
##### Chameleon(with Monitoring):
```
python3 condor_client.py  -s2 Chameleon:CHI@UC -d2 ./chameleon_mon/ -l `date -v +2d +%s` -i2 "192.168.100.5" -o create -w ch-abcd-1114 -n2 1
```

NOTE: Start IP address passed via -i2 should match the network specified in JSON for the nodes.
##### Exogeni
- Master, Worker, Submit and Storage nodes on Exogeni (if json for either of the nodes is not present they are not instantiated)
- Stitch.json if present in the directory would be used to stitch
```
python3 condor_client.py -s1 'Exogeni:UH (Houston, TX USA) XO Rack'  -d1 ./exogeni/ -l `date -v +2d +%s` -i1 "172.16.0.1" -o create -w abcd-1114 -n1 1
```
##### Exogeni(with Monitoring)
```
python3 condor_client.py -s1 'Exogeni:UFL (Gainesville, FL USA) XO Rack'  -d1 ./exogeni_mon/ -l `date -v +2d +%s` -i1 "172.16.0.1" -o create -w abcd-1114 -n1 1
```

##### Hybrid Model: 
- Master, Worker and Storage nodes on Exogeni
- Storage node stitched to UNT and Chameleon
- Storage node acts a forwarder to transfer traffic from Exogeni to Chameleon and viceversa
- Storage node acts a forwarder to transfer traffic from UNT to Exogeni and viceversa
###### Submit and Master node running together
```
python3 condor_client.py -s1 'Exogeni:UH (Houston, TX USA) XO Rack'  -d1 ./hybrid/exogeni-casa/ -s2 Chameleon:CHI@UC -d2 ./hybrid/chameleon-casa/ -l `date -v +2d +%s` -i1 "172.16.0.1" -i2 "192.168.10.5" -o create -w abcd-1114 -n1 1 -n2 1
```
###### Separate submit node
```
python3 condor_client.py -s1 'Exogeni:UH (Houston, TX USA) XO Rack'  -d1 ./hybrid/exogeni/ -s2 Chameleon:CHI@UC -d2 ./hybrid/chameleon/ -l `date -v +2d +%s` -i1 "172.16.0.1" -i2 "192.168.10.5" -o create -w abcd-1114 -n1 1 -n2 1
```
###### Submit and Master node running together with monitoring enabled
```
python3 condor_client.py -s1 'Exogeni:UH (Houston, TX USA) XO Rack'  -d1 ./hybrid/exogeni-casa-mon/ -s2 Chameleon:CHI@UC -d2 ./hybrid/chameleon-casa-mon/ -l `date -v +2d +%s` -i1 "172.16.0.1" -i2 "192.168.10.5" -o create -w abcd-1114 -n1 1 -n2 1
```
NOTE: Start IP address passed via -i2 should match the network specified in JSON for the nodes.
NOTE: Nodes for hybrid model on exogeni if instantitaed on UH rack, chameleon nodes should be instantiated on UC as stitchport from UH rack to Chameleon TACC site is not allowed 

##### Cluster spanning 4 clouds
- Master and submit node on Exogeni
- Worker on Chameleon, Jetstream and Mass Open Cloud
```
python3 condor_client.py -o create -w merit-w1 -s1 'Exogeni:UH (Houston, TX USA) XO Rack'  -d1 ./merit/exogeni/ -s2 Chameleon:CHI@UC -d2 ./merit/chameleon/ -s3 'Jetstream:TACC' -d3 ./merit/jetstream/ -s4 'Mos:moc-kzn' -d4 ./merit/mos/ -l `date -v +2d +%s` -n1 1 -n2 1 -n3 1 -n4 1
```
#### <a name="get"></a>Get status of condor cluster
```
python3 condor_client.py -o get -w abcd-1114
```

#### <a name="delete"></a>Delete condor cluster
```
python3 condor_client.py -o delete -w abcd-1114 
```
#### <a name="list"></a>List workflows
```
python3 condor_client.py -o list
```
