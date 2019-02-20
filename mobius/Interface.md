# Introduction
Mobius is a service which enables provisioning of resources on multiple cloud infrastructure. In the current implemenation, Exogeni, Chameleon and Open Science Grid is supported.

# API Operations
Source [Mobius](https://app.swaggerhub.com/apis/kthare10/mobius/1.0.0) specification on swaggerhub. 

## workflow
### POST
Create an empty workflow with workflowID provided if no existing workflow is associated with workflowID. Returns error if workflow is already associated to an exsiting workflow. 
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
Get workflow status. Mobius returns JSON object indicating all the slices at diffierent sites at different clouds. For each slice, all Compute and storage nodes are returned along with IP and state information.
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
Delete a workflow. It results in the deletion of all the slices at different sites on different clouds associated with the workflow.
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
Provision compute resources for a workflow. Compute resources are added as per SlicePolicy indicated in the request. For 'new' slicePolicy, compute resources are added in a new slice on site specified. For 'existing' slicePolicy, compute resources are added to existing slice specified by 'sliceName' field. For 'default' slicePolicy, compute resources are either added to an existing slice with same leaseEndTime if found or added to a new slice on site specified. Default value is 'default'
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
| ramPerCpus    | Integer       | RAM per cpu in MB  | M         |
| diskPerCpus   | Integer       | Disk per cpu in MB  | M         |
| leaseStart    | String        | Lease Start Time as Linux epoch | M         |
| leaseEnd      | String        | Lease End Time as Linux epoch | M         |
| coallocate    | Boolean       | flag indicating if CPUs should be allocated across multiple compute resources or not. Should be set to 'true' if CPUs should be coallocated on single compute resource. Default value is 'false' | M         |
| slicePolicy   | String        | Indicates Slice policy to be used. For 'new' slicePolicy, compute resources are added in a new slice on site specified. For 'existing' slicePolicy, compute resources are added to existing slice specified by 'sliceName' field. For 'default' slicePolicy, compute resources are either added to an existing slice with same leaseEndTime if found or added to a new slice on site specified. Default value is 'default'| M         |
| sliceName      | String       | Existing slice name to which compute resources should be added | O         |
| hostNamePrefix | String       | Prefix to be added to hostName | O         |
| ipAddress      | String       | IP address to assign. should be specified only if coallocate is set to 'true'. | O         |
| imageUrl      | String        | Image URL | O         |
| imageHash     | String        | Image Hash | O         |
| imageName     | String        | Image Name | O         |
| postBootScript | String       | Post Boot Script | O         |
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
Provision storage resources for a workflow. For add action, Storage resource is added to target node. If target not is not found, an error is returned. For delete action, all storage nodes attached to target node are deleted. For renew action, lease of the entire slice is renewed.
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
| action        | String        | Action to be taken i.e. add, delete, renew. For 'add' action, Storage resource is added to target node. If target not is not found, an error is returned. For 'delete' action, all storage nodes attached to target node are deleted. For 'renew' action, lease of the entire slice is renewed.  | M         |
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
