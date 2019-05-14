# Table of contents

- [Mobius](#Mobius)
  - [Component Diagram](#component)
  - [Cloud API](#api)
    - [Ahab](#ahab)
    - [Apache Jclouds](#jclouds)
  - [Workflow Database](#db)
  - [Periodic Processor](#pp)
  - [Policy Monitor](#pm)
  - [Mobius Controller](#mc)
  - [To do list](#todo)
  - [How to use or launch Mobius?](#docker)
# <a name="Mobius"></a>Mobius

Mobius (application independent controller) is a spring framework based REST server. It consumes high level application resource requests in the form of REST APIs. It automatically provisions the required resources on appropriate cloud providers using various Cloud APIs. It also monitors these resources and automatically modify them so as to ensure they meet the requirements initially requested. We now describe components of Mobius.


- Design details can be found in [Design](./mobius/Readme.md)
- Interface specifications can be found in [Interface](./mobius/Interface.md)
- Code can be generated via swagger by referring to [HowToGenerateCodeFromSwagger](./mobius/HowToGenerateCodeFromSwagger.md)
## <a name="component"></a>Component Diagram
![Component Diagram](./mobius/plantuml/images/mobius.png)

## <a name="api"></a> Cloud API
Mobius translates application requests to cloud specific requests in this layer. As part of Mobius, we have implemented cloud specific controllers to provision and monitor resources.

### <a name="ahab"></a> AHAB 
Ahab is a collection of libraries for managing the state of slices created on ExoGENI. The native request and resource representation used by ExoGENI is based on declarative representations using NDL-OWL. Although, this abstraction works well for mapping virtual topologies and provisioning resources at the NIaaS layer, these are not suitable for higher level applications. The applications are seldom interested in low-level topologies, and often have a simplistic view of the kind of resources they need. For example, the application layer often expresses resource requirements in terms of requiring a “Condor pool”, “Hadoop cluster”, “distributed Condor pool with on-ramp to an external data-set”, “MPI cluster with low latency and high bandwidth”, etc. AHAB provides an abstraction to NIaaS provisioning constructs to shield the users and applications from the details of low level topology request. Mobius ExoGENI controller triggers AHAB libraries to provision resources in ExoGENI.

## <a name="jclouds"></a> Apache Jclouds
Apache jclouds is an open source multi-cloud toolkit for the Java platform that gives you the freedom to create applications that are portable across clouds while giving you full control to use cloud-specific features. Mobius has implemented Jclouds controller for Openstack to provision resources on Chameleon and continue to enhance it to provision resources on Jet Stream. We also plan to implement Jclouds controller for EC2 ti provision resources on AWS.

## <a name="db"></a> Workflow Database
Mobius controller maintains the information of all the resources provisioned for a workflow on different clouds and the corresponding application request parameters in Workflow Database.

## <a name="pp"></a> Periodic Processor
The periodic processor performs following actions:
a) Monitors for the status of all the resources provisioned for various workflows and triggers a notification to Pegasus via AMQP space.
b) Triggers the provisioning of the resources scheduled to be provisioned at a specific time.

## <a name="pm"></a> Policy Monitor
The policy monitor checks the status of various resources provisioned for various workflows. It compares the state of the resources against the applications requests (available via Workflow DB). Based on the policies and thresholds defined, identifies the appropriate actions to be taken to ensure that application requests are met at all times. The actions include enabling compute, storage and network elasticity i.e. growing and shrinking compute or storage resource pools and increasing or decreasing network qos.

## <a name="mc"></a> Mobius Controller
Mobius controller is software component which controls all the above components and processes the incoming REST requests to trigger appropriate components.
## <a name="todo"></a>TODO List
- Enable Mobius to pass HEAT Templates
## <a name="docker"></a>How to use or launch Mobius?
- Refer to [Docker](./docker/Readme.md) to launch Mobius
