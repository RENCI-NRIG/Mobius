# Table of contents
- [Mobius](#mobius)
- [Block Diagram](#block)
  - [Rest Interface](#rest)
  - [Mobius Controller](#controller)
  - [Workflow](#workflow)
  - [CloudContext](#cloudcontext)
  - [ExogeniContext & SliceContext](#context)
  - [Periodic Processing](#periodic)  
  - [Notification Publisher](#notification)
  - [Stored Worflows](#stored)
 - [Mobius Controller](#controller)
   - [Workflow](#workflow)
     - [Create Workflow](#create)
     - [Get Workflow](#get)
     - [Delete workflow](#delete)
   - [Compute](#compute)
   - [Storage](#storage)
 - [Periodic Processing](#periodic)
 - [Class Diagram](#classdiagram)

# <a name="mobius"></a>Mobius

Mobius is Spring-Boot based REST Webserver with the ability to provision compute, network or storage resources on multiple clouds. In the first release, following 3 cloud providers will be supported:
- Exogeni
- Chameleon
- Open Science Grid

# <a name="block"></a>Block Diagram
![Component Diagram](../mobius/plantuml/images/component.png)
## <a name="rest"></a>Rest Interface
It represents Swagger generated REST API interface with Spring framework controllers through which Mobius controller logic is invoked.
## <a name="controller"></a>Mobius Controller
Is responsible for creating and maintaining workflows. It maintains a hashmap of workflows with workflowId as the key. User requests for a workflowID via 'POST /mobius/workflow' API which results in generation of workflowId and Workflow object. The pair is added to hashmap. All subsequent operations are performed on this workflow object identitied by workflowId.
## <a name="workflow"></a>Workflow
Represents Workflow which spans across clouds and maintains hashMap of CloudContext with siteName as key. Any compute request either finds an existing context or creates a new context to satisfy the request. Any storage/networks request finds existing context to process the request.
## <a name="cloudcontext"></a>CloudContext
Represents slice(s) on a specific site on a specific cloud. ExogeniContext, ChameleonContext represent Exogeni and chameleon specific instances of this class.
### <a name="context"></a>ExogeniContext & SliceContext
Represents slice(s) on Exogeni on a specific site. It maintains hashMap of SliceContext representing one slice with sliceName as the key. Any compute requests with the same leaseEnd time of any existing context result in a modifySlice. If no existing slice has the same leaseEnd time as in incoming request, a new Slice is created to handle the request and corresponding SliceContext object is added to hashMap. ExogeniContext also maintains futureRequest queue. Any incoming request with leaseStartTime in future is added to futureRequest Queue.
## <a name="periodic"></a>PeriodicProcessing 
Represents Periodic Processing Thread is responsible for periodically performing below actions:
- Handle future Compute/Storage/Network request from futureRequest Queue
- Check status of workflow and trigger a notification to Pegasus if there is any change in workflow status
## <a name="notification"></a>Notification Publisher
Responsible for triggering notfications to Pegasus on Workflow status change. It currently uses AMQP but is easily extendible to use other frameworks like kafka.
## <a name="stored"></a>Stored Worflows
Each workflow saves its workflowId and hashMap for CloudContexts in the Database. This information can be used to create Workflow context in case of abnormal restart of Mobius application.

## <a name="controller"></a>Mobius Controller
Mobius controller is responsible for handling Rest APIs. It is a Singleton class and maintains all the workflows in workflowHashMap with workFlowId as the key.
### <a name="workflow"></a>Workflow
Workflow is uniquely identified by its ID. Workflow can span across clouds and sites. It maintains a hashtable of CloudContext with siteName as the key. MobiusController handles create, get and delete operations for workflow as explained below. 
#### <a name="create"></a>Create
![createWorkflow](../mobius/plantuml/images/createWorkflow.png)
```
POST -i "<ip/hostname>:8080/mobius/workflow" -H "accept: application/json"
```
#### <a name="get"></a>Get
![getWorkflow](../mobius/plantuml/images/getWorkflow.png)
```
GET -i "<ip/hostname>:8080/mobius/workflow?workflowID=<workflowID>" -H "accept: application/json"

Example Output:
{
"status":200,
"message":"Success",
"value":"{"workflowStatus":"[{\"nodes\":[{\"name\":\"dataNode1\",\"publicIP\":\"152.54.14.14\",\"state\":\"Active\",\"ip1\":\"172.16.0.2\"},
{\"name\":\"dataNode0\",\"publicIP\":\"152.54.14.6\",\"state\":\"Active\",\"ip1\":\"172.16.0.1\"}],
\"slice\":\"Mobius-Exogeni-kthare10-afdc64d6-290f-4f35-bbad-169d848cce1f\"},
{\"nodes\":[{\"name\":\"dataNode3\",\"publicIP\":\"152.54.14.18\",\"state\":\"Active\",\"ip1\":\"172.16.0.1\"}],
\"slice\":\"Mobius-Exogeni-kthare10-5c4f6855-9333-4a46-905f-e82d414f0575\"}]"}",
"version":"0.1"
}
```
#### <a name="delete"></a>Delete
![deleteWorkflow](../mobius/plantuml/images/deleteWorkflow.png)
```
DELETE -i "<ip/hostname>:8080/mobius/workflow?workflowID=<workflowID>" -H "accept: application/json"
```
#### <a name="compute"></a>Compute
![compute](../mobius/plantuml/images/compute.png)
```
POST "<ip/hostname>:8080/mobius/compute?workflowID=<workflowId>" -H "accept: application/json" -H "Content-Type: application/json" -d @compute.json 

$ cat compute.json
{
    "site":"Exogeni:RENCI (Chapel Hill, NC USA) XO Rack",
    "cpus":"1",
    "gpus":"0",
    "ramPerCpus":"1",
    "diskPerCpus":"11",
    "leaseStart":"1545151122",
    "leaseEnd":"1545551122"
}
```
NOTE: For Exogeni, slice names are generated as 'Mobius-Exogeni-<user>-uuid'. Hostnames for VMs are dataNode<Number>
#### <a name="storage"></a>Storage
![storage](../mobius/plantuml/images/storage.png)
```
POST "<ip/hostname>:8080/mobius/compute?workflowID=<workflowId>" -H "accept: application/json" -H "Content-Type: application/json" -d @storage.json 
$ cat storage.json
{
    "mountPoint":"/mnt/",
    "target":"dataNode0",
    "size":"1",
    "leaseStart":"1545151122",
    "leaseEnd":"1545551122",
    "action":"add"
}
```
## <a name="periodic"></a>Periodic Processing Thread
![periodicProcessing](../mobius/plantuml/images/periodicProcessing.png)
```
Sending notification to Pegasus = 
[
    {
        "slices":
            [
                {
                    "nodes":
                        [
                            {
                                "name":"dataNode1storage",
                                "state":"Active"
                            },
                            {
                                "name":"dataNode7",
                                "publicIP":"152.54.14.30",
                                "state":"Active",
                                "ip1":"172.16.0.2"
                            },
                            {
                                "name":"dataNode1",
                                "publicIP":"152.54.14.28",
                                "ip2":"172.16.0.1",
                                "state":"Active",
                                "ip1":"10.104.0.6"
                            }
                        ],
                    "slice":"Mobius-Exogeni-kthare10-644c2ec3-784a-4f1d-8878-2862670f8c8c"
                }
            ],
        "site":"Exogeni:RENCI (Chapel Hill, NC USA) XO Rack"
    },
    {
        "slices":
            [
                {
                    "nodes":
                        [
                            {
                                "name":"dataNode0",
                                "publicIP":"162.244.229.107",
                                "state":"Active","ip1":"172.16.0.1"
                            },
                            {
                                "name":"dataNode3",
                                "publicIP":"162.244.229.108",
                                "state":"Active",
                                "ip1":"172.16.0.1"
                            }
                        ],
                    "slice":"Mobius-Exogeni-kthare10-e5ee3668-428e-4899-a4dd-f83f16f4f7d5"
                }
            ],
        "site":"Exogeni:CIENA2 (Hanover, MD) XO Rack"
    }
]
```
## <a name="classdiagram"></a>Class Diagram
![diagram1](../mobius/plantuml/images/diagram1.png)
![diagram2](../mobius/plantuml/images/diagram2.png)
