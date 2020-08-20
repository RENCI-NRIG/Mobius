# Table of contents

 - [Running the code](#run1)
   - [Running everything in docker](#run2)
     - [Clone git repo](#clone)
     - [User specific configuration](#config)
     - [Run Docker](#docker)
     - [Examples](#examples)
 
# <a name="run1"></a>Running the code
This repository is designed to be run in Docker out of the box using docker-compose. Optionally the user can make minor configuration changes to run portions of the project on their local machine for easier programmatic interaction with Mobius directly.

## <a name="run2"></a>Running everything in docker
### <a name="clone"></a>Clone git repo
```
git clone https://github.com/RENCI-NRIG/Mobius.git
cd ./Mobius/docker
```
### <a name="config"></a>User specific configuration
Once images are ready, update configuration in docker as indicated below:
1. Update docker/config/application.properties to specify user specific values for each cloud credentials (exogeni, chameleon, jetstream and mass open cloud) 
```
mobius.exogeni.user=<User Name>
mobius.exogeni.certKeyFile=<Exogeni Pem File Name(geni-kthare10.pem)>
mobius.chameleon.user=<User Name>
mobius.chameleon.user.password=<Password>
mobius.jetstream.user.domain=<domain>
mobius.jetstream.user=<User Name>
mobius.jetstream.user.password=<Password>
mobius.mos.user=<User Name>
mobius.mos.user.password=<Password>
```
2. Using Mobius without SDX (network requests would not work in this case)
Update docker-compose.yml for mobius_server to point the below parameters to user specific locations. 
User needs to modify the values before the colon to map to location on host machine.
```
        # point to user specific keys below
         volumes:
         - "./logs:/code/logs"
         - "./mobius-sync:/code/mobius-sync"
         - "./config/application.properties:/code/config/application.properties"
         - "./config/log4j.properties:/code/config/log4j.properties"
         - "<./ssh/geni-kthare10.pem:/code/ssh/geni-kthare10.pem"
         - "./ssh/id_rsa.pub:/code/ssh/id_rsa.pub"
         - "./ssh/id_rsa:/code/ssh/id_rsa"
```

3. Using Mobius with SDX 
Update following fields docker/config/sdx.conf
```
  slicename="<Slice Name>"
  exogenipem="./ssh/<Exogeni Certificate File name(geni-kthare10.pem)"
```
Update docker-compose_sdx.yml for mobius_server to point the below parameters to user specific locations. User needs to modify the values before the colon to map to location on host machine.
```
        # point to user specific keys below
         volumes:
         - "./logs:/code/logs"
         - "./mobius-sync:/code/mobius-sync"
         - "./config/application.properties:/code/config/application.properties"
         - "./config/log4j.properties:/code/config/log4j.properties"
         - "<Absolute Path of Exogeni Certficate File>:/code/ssh/<Exogeni Certificate File Name(geni-kthare10.pem)>"
         - "<Absolute path to User public key>/id_rsa.pub:/code/ssh/id_rsa.pub"
         - "<Absolute path to User private key>/id_rsa:/code/ssh/id_rsa"
```
Update docker-compose_sdx.yml for sdxserver to point the below parameters to user specific locations. User needs to modify the values before the colon to map to location on host machine.
```
         # point to user specific keys below
         volumes:
         - "./sdxlog:/code/log"
         - "./config/sdx.conf:/code/config/sdx.conf"
         - "<Absolute Path of Exogeni Certficate File>:/code/ssh/<Exogeni Certificate File Name>"
         - "<Absolute path to User public key>/id_rsa.pub:/code/ssh/id_rsa.pub"
         - "<Absolute path to User private key>/id_rsa:/code/ssh/id_rsa"
         - "./resources:/code/resources"
```

### <a name="run3"></a>Run Docker
#### Mobius without SDX
Run docker-compose up -d from Mobius/docker directory when running without SDX.

```
$ docker-compose up -d
Creating database ... done
Creating rabbitmq ... done
Creating mobius   ... done
Creating notification ... done
```
#### Mobius with SDX
Run docker-compose -f docker-compose_sdx.yml up -d from Mobius/docker directory when running with SDX.
```
$ docker-compose -f docker-compose_sdx.yml up -d
Creating database ... done
Creating rabbitmq ... done
Creating mobius   ... done
Creating notification ... done
Creating sdxserver ... done
```
After a few moments the docker containers will have stood up and configured themselves. User can now trigger requests to Mobius. Refer to [Interface](../mobius/Interface.md) to see the REST API

#### <a name="example"></a>Few example commands
- Create a workflow
```
curl -X POST -i "localhost:8080/mobius/workflow?workflowID=abcd-5678" -H "accept: application/json"
```
- Get workflow status
```
curl -X GET -i "localhost:8080/mobius/workflow?workflowID=abcd-5678" -H "accept: application/json"
```
- Provision Compute node
```
curl -X POST -i "localhost:8080/mobius/compute?workflowID=abcd-5678" -H "accept: application/json" -H "Content-Type: application/json"  -d @compute.json
```
- Provision Storage node
```
curl -X POST -i "localhost:8080/mobius/storage?workflowID=abcd-5678" -H "accept: application/json" -H "Content-Type: application/json"  -d @storage.json
```
- Extablish a network link
```
curl -X POST -i "localhost:8080/mobius/network?workflowID=abcd-5678" -H "accept: application/json" -H "Content-Type: application/json"  -d @network.json
```
- Delete workflow
```
curl -X DELETE -i "localhost:8080/mobius/workflow?workflowID=abcd-5678" -H "accept: application/json"
```

Example json files available at [test](https://github.com/RENCI-NRIG/Mobius/tree/master/mobius/test) directory.
- List workflows
```
curl -X GET -i "localhost:8080/mobius/listWorkflows"
```
