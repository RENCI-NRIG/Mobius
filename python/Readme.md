# Table of contents

 - [Description](#descr)
 - [Installation](#install)
   - [Mobius Client](#mbclient)
     - [Usage](#usage1)
   - [Condor Client](#condorclient)
     - [Usage](#usage2)   
   - [Json Data](#json)
   - [Certificates](#certs)
   - [Examples](#examples)
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
python3 mobius_client.py -h
usage: mobius_client.py [-h] [-s SITE] [-m MOBIUSHOST] -o OPERATION -w
                        WORKFLOWID -d DATA

Python client to provision cloud resources by invoking Mobius REST Commands.

optional arguments:
  -h, --help            show this help message and exit
  -s SITE, --site SITE  Site
  -m MOBIUSHOST, --mobiushost MOBIUSHOST
                        Mobius Host e.g. http://localhost:8080/mobius
  -o OPERATION, --operation OPERATION
                        Operation allowed values: create|get|delete
  -w WORKFLOWID, --workflowId WORKFLOWID
                        workflowId
  -d DATA, --data DATA  data
```
## <a name="condor"></a>Condor Client
Python client to create Condor clusters by invoking various supported Mobius REST commands.

### <a name="usage2"></a>Usage
```
$ python3 condor_client.py  -h
usage: condor_client.py [-h] [-s SITE] [-n WORKERS] [-c COMETHOST] [-t CERT]
                        [-k KEY] [-m MOBIUSHOST] -o OPERATION -w WORKFLOWID

Python client to create Condor cluster using mobius. Uses json object for
compute requests present in data directory if present, otherwises uses the
default. Currently only supports provisioning compute resources. Creates COMET
contexts for Chameleon resources and thus enables exchanging keys and
hostnames within workflow

optional arguments:
  -h, --help            show this help message and exit
  -s SITE, --site SITE  Site
  -n WORKERS, --workers WORKERS
                        Number of workers
  -c COMETHOST, --comethost COMETHOST
                        Comet Host e.g. https://18.218.34.48:8111/
  -t CERT, --cert CERT  Comet Certificate
  -k KEY, --key KEY     Comet Certificate key
  -m MOBIUSHOST, --mobiushost MOBIUSHOST
                        Mobius Host e.g. http://localhost:8080/mobius
  -o OPERATION, --operation OPERATION
                        Operation allowed values: create|get|delete
  -w WORKFLOWID, --workflowId WORKFLOWID
                        workflowId
```
### <a name="json"></a>JSON Data
Json Data for Master, Submit and Worker Nodes is read from Mobius/python/data directory.

### <a name="certs"></a>Certificates
Example Comet Certficates are present in Mobius/python/certs directory.

### <a name="examples"></a>Examples
#### <a name="create"></a>Create a condor cluster
Create a condor cluster with 1 master, 1 submit and 1 worker node. 
NOTE: Comet context for each node is created and neuca tools are also installed on each node. This results in hostnames and keys to be exchanged between all nodes in condor cluster

`python3 condor_client.py -s Chameleon:CHI@UC -n 1 -c https://18.218.34.48:8111/ -t certs/inno-hn_exogeni_net.pem -k certs/inno-hn_exogeni_net.key -o create -w abcd-5678`

#### <a name="get"></a>Get status of condor cluster
`python3 condor_client.py -o get -w abcd-5678`

#### <a name="delete"></a>Delete condor cluster
`python3 condor_client.py -o delete -w abcd-5678`
