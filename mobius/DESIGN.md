# Design

Mobius is Spring-Boot based REST Webserver with the ability to provision compute, network or storage resources on multiple clouds. In the first release, following 3 cloud providers will be supported:
- Exogeni
- Chameleon
- Open Science Grid

It has 2 main processing components:
- Mobius Controller
- Periodic Processing Thread

## Mobius Controller
Mobius controller is responsible for handling Rest APIs. It is a Singleton class and maintains all the workflows in workflowHashMap with workFlowId as the key.
### Workflow
MobiusController handles create, get and delete operations for workflow as explained below. 
#### Create
- Checks if Periodic processing thread is running
- If not, Uses java.util.UUID.randomUUID() to generate and return UUID; in case of error in UUID generation, INTERNAL_SERVER_ERROR is returned
- Otherwise, returns SERVICE_UNAVAILABLE; System busy error
- URL :  POST -i "<ip/hostname>:8080/mobius/workflow" -H "accept: application/json"
#### Get
- Checks if Periodic processing thread is running
- If not, performs lookup on workflowHapMap to find the workflow 
- If workflow not found, return NOT_FOUND
- Otherwise return workflow staus; ; in case of error in status generation, INTERNAL_SERVER_ERROR is returned
- Otherwise, returns SERVICE_UNAVAILABLE; System busy error
- URL :  GET -i "<ip/hostname>:8080/mobius/workflow?workflowID=<workflowID>" -H "accept: application/json"
#### DELETE
URL :  DELETE -i "<ip/hostname>:8080/mobius/workflow?workflowID=<workflowID>" -H "accept: application/json"
