# Introduction
Mobius is a service which enables provisioning of resources on multiple cloud infrastructure. In the current implemenation, Exogeni, Chameleon and Open Science Grid is supported.

# API Operations
Source [Mobius](https://app.swaggerhub.com/apis/kthare10/mobius/1.0.0) specification on swaggerhub. 

## workflow
### POST
Create a workflow ID. 
#### URL
POST -i "<ip/hostname>:8080/mobius/workflow?workflowID=< workflowID >" -H "accept: application/json"
#### Parameters
| Name          | Type          | Description                          | Occurence |
| ------------- |:-------------:| ------------------------------------:| ---------:|
| workflowID    | String        | Workflow ID identifying the workflow | M         |
#### Response
| Name          | Type          | Description                          | Occurence |
| ------------- |:-------------:| ------------------------------------:| ---------:|
| status        | Integer       | HTTP Status code                     | M         |
| message       | String        | Message                              | M         |
| Value         | Object        | null   | M         |
| Version       | String        | Mobius API version number            | M         |
#### Return Code
| HTTP Status Code | Description                          |
| ----------------:| ------------------------------------:|
| 503              | Service Unavailable                  |
| 500              | Internal Server Error                |
| 400              | Bad Request                          |
| 200              | Success                              |
### GET
Get workflow status
#### URL
GET -i "<ip/hostname>:8080/mobius/workflow?workflowID=< workflowID >" -H "accept: application/json"
#### Parameters
| Name          | Type          | Description                          | Occurence |
| ------------- |:-------------:| ------------------------------------:| ---------:|
| workflowID    | String        | Workflow ID identifying the workflow | M         |
#### Response
| Name          | Type          | Description                          | Occurence |
| ------------- |:-------------:| ------------------------------------:| ---------:|
| status        | Integer       | HTTP Status code                     | M         |
| message       | String        | Message                              | M         |
| Value         | Object        | JSON Object representing status of workflow   | M         |
| Version       | String        | Mobius API version number            | M         |
#### Return Code
| HTTP Status Code | Description                          |
| ----------------:| ------------------------------------:|
| 503              | Service Unavailable                  |
| 500              | Internal Server Error                |
| 400              | Bad Request                          |
| 404              | Not found                           |
| 200              | Success                              |
```
EXAMPLE RESPONSE:
{
  "status":200,
  "message":"Success",
  "value":
          "{
             \"workflowStatus\":
                                \"[
                                    {
                                        \\\"slices\\\":
                                                        [
                                                          {
                                                              \\\"nodes\\\":
                                                                           [
                                                                             {
                                                                                \\\"name\\\":\\\"dataNode3\\\",
                                                                                \\\"publicIP\\\":\\\"152.54.14.30\\\",
                                                                                \\\"state\\\":\\\"Active\\\",
                                                                                \\\"ip1\\\":\\\"172.16.0.1\\\"
                                                                             },          
                                                                             {
                                                                             \\\"name\\\":\\\"dataNode7\\\",
                                                                             \\\"publicIP\\\":\\\"152.54.14.28\\\",
                                                                             \\\"state\\\":\\\"Active\\\",
                                                                             \\\"ip1\\\":\\\"172.16.0.1\\\"
                                                                             }
                                                                           ],
                                                              \\\"slice\\\":\\\"Mobius-Exogeni-kthare10-f0106b20-2795-45a6-a0ce-7f7a7e1be2d3\\\"
                                                          }
                                                        ],
                                        \\\"site\\\":\\\"Exogeni:RENCI (Chapel Hill, NC USA) XO Rack\\\"
                                    },
                                    {
                                        \\\"slices\\\":
                                                        [
                                                          {
                                                              \\\"nodes\\\":
                                                                           [
                                                                              {
                                                                                \\\"name\\\":\\\"dataNode0storage\\\",
                                                                                \\\"state\\\":\\\"Active\\\"
                                                                              }, 
                                                                              {
                                                                                \\\"name\\\":\\\"dataNode0\\\",
                                                                                \\\"publicIP\\\":\\\"162.244.229.107\\\",
                                                                                \\\"ip2\\\":\\\"10.104.0.6\\\",
                                                                                \\\"state\\\":\\\"Active\\\",
                                                                                \\\"ip1\\\":\\\"172.16.0.1\\\"
                                                                              },{
                                                                                \\\"name\\\":\\\"dataNode1\\\",
                                                                                \\\"publicIP\\\":\\\"162.244.229.108\\\",
                                                                                \\\"state\\\":\\\"Active\\\",
                                                                                \\\"ip1\\\":\\\"172.16.0.2\\\"
                                                                              }
                                                                           ],
                                                              \\\"slice\\\":\\\"Mobius-Exogeni-kthare10-a2e73094-d8d8-4924-9bed-33d5f05afea7\\\"
                                                          }
                                                        ],
                                        \\\"site\\\":\\\"Exogeni:CIENA2 (Hanover, MD) XO Rack\\\"
                                    }
                                ]\"
          }",
  "version":"0.1"
}
```
### DELETE
Delete a workflow
#### URL
DELETE -i "<ip/hostname>:8080/mobius/workflow?workflowID=< workflowID >" -H "accept: application/json"
#### Parameters
| Name          | Type          | Description                          | Occurence |
| ------------- |:-------------:| ------------------------------------:| ---------:|
| workflowID    | String        | Workflow ID identifying the workflow | M         |
#### Response
| Name          | Type          | Description                          | Occurence |
| ------------- |:-------------:| ------------------------------------:| ---------:|
| status        | Integer       | HTTP Status code                     | M         |
| message       | String        | Message                              | M         |
| Value         | Object        | null  | M         |
| Version       | String        | Mobius API version number            | M         |
#### Return Code
| HTTP Status Code | Description                          |
| ----------------:| ------------------------------------:|
| 503              | Service Unavailable                  |
| 500              | Internal Server Error                |
| 400              | Bad Request                          |
| 404              | Not found                           |
| 200              | Success                              |
## compute
Provision compute resources for a workflow
#### URL
POST "<ip/hostname>:8080/mobius/compute?workflowID=< workflowId >" -H "accept: application/json" -H "Content-Type: application/json" -d @compute.json 
#### Parameters
| Name          | Type          | Description                          | Occurence |
| ------------- |:-------------:| ------------------------------------:| ---------:|
| workflowID    | String        | Workflow ID identifying the workflow | M         |
#### Body
| Name          | Type          | Description                          | Occurence |
| ------------- |:-------------:| ------------------------------------:| ---------:|
| site          | String        | Site name  | M         |
| cpus          | Integer       | Number of cpus requested  | M         |
| gpus          | Integer       | Number of gpus requested  | M         |
| ramPerCpus    | Integer       | RAM per cpu in bytes  | M         |
| diskPerCpus   | Integer       | Disk per cpu in bytes  | M         |
| leaseStart    | String        | Lease Start Time as Linux epoch | M         |
| leaseEnd      | String        | Lease End Time as Linux epoch | M         |
| coallocate    | Boolean       | flag indicating if CPUs should be allocated across multiple compute resources or not. Should be set to 'true' if CPUs should be coallocated on single compute resource. Default value is 'false' | M         |
| imageUrl      | String        | Image URL | O         |
| imageHash     | String        | Image Hash | O         |
| imageName     | String        | Image Name | O         |
#### Response
| Name          | Type          | Description                          | Occurence |
| ------------- |:-------------:| ------------------------------------:| ---------:|
| status        | Integer       | HTTP Status code                     | M         |
| message       | String        | Message                              | M         |
| Value         | Object        | null  | M         |
| Version       | String        | Mobius API version number            | M         |
#### Return Code
| HTTP Status Code | Description                          |
| ----------------:| ------------------------------------:|
| 503              | Service Unavailable                  |
| 500              | Internal Server Error                |
| 400              | Bad Request                          |
| 404              | Not found                           |
| 200              | Success                              |
## storage
Provision storage resources for a workflow
#### URL
POST "<ip/hostname>:8080/mobius/storage?workflowID=< workflowId >" -H "accept: application/json" -H "Content-Type: application/json" -d @storage.json 
#### Parameters
| Name          | Type          | Description                          | Occurence |
| ------------- |:-------------:| ------------------------------------:| ---------:|
| workflowID    | String        | Workflow ID identifying the workflow | M         |
#### Body
| Name          | Type          | Description                          | Occurence |
| ------------- |:-------------:| ------------------------------------:| ---------:|
| mountPoint    | String        | Mount Point  | M         |
| target        | Integer       | Target Node Name  | M         |
| size          | Integer       | Size in GB  | M         |
| action        | String        | Action to be taken i.e. add, delete, renew  | M         |
| leaseStart    | String        | Lease Start Time as Linux epoch | M         |
| leaseEnd      | String        | Lease End Time as Linux epoch | M         |
#### Response
| Name          | Type          | Description                          | Occurence |
| ------------- |:-------------:| ------------------------------------:| ---------:|
| status        | Integer       | HTTP Status code                     | M         |
| message       | String        | Message                              | M         |
| Value         | Object        | null  | M         |
| Version       | String        | Mobius API version number            | M         |
#### Return Code
| HTTP Status Code | Description                          |
| ----------------:| ------------------------------------:|
| 503              | Service Unavailable                  |
| 500              | Internal Server Error                |
| 400              | Bad Request                          |
| 404              | Not found                           |
| 200              | Success                              |
## network
Provision network resources for a workflow
#### URL
POST "<ip/hostname>:8080/mobius/network?workflowID=< workflowId >" -H "accept: application/json" -H "Content-Type: application/json" -d @network.json 
#### Parameters
| Name          | Type          | Description                          | Occurence |
| ------------- |:-------------:| ------------------------------------:| ---------:|
| workflowID    | String        | Workflow ID identifying the workflow | M         |
#### Body
| Name          | Type          | Description                          | Occurence |
| ------------- |:-------------:| ------------------------------------:| ---------:|
| source    | String        | Source Hostname  | M         |
| destination        | Integer       | Destination Hostname  | M         |
| linkSpeed          | Integer       | Link Speed  | M         |
| qos        | String        | Qos  | M         |
| action        | String        | Action to be taken i.e. add, delete, update  | M         |
| leaseStart    | String        | Lease Start Time as Linux epoch | M         |
| leaseEnd      | String        | Lease End Time as Linux epoch | M         |
#### Response
| Name          | Type          | Description                          | Occurence |
| ------------- |:-------------:| ------------------------------------:| ---------:|
| status        | Integer       | HTTP Status code                     | M         |
| message       | String        | Message                              | M         |
| Value         | Object        | null  | M         |
| Version       | String        | Mobius API version number            | M         |
#### Return Code
| HTTP Status Code | Description                          |
| ----------------:| ------------------------------------:|
| 503              | Service Unavailable                  |
| 500              | Internal Server Error                |
| 400              | Bad Request                          |
| 404              | Not found                           |
| 200              | Success                              |
