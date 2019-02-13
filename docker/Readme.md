# Running the code
This repository is designed to be run in Docker out of the box using docker-compose. Optionally the user can make minor configuration changes to run portions of the project on their local machine for easier programmatic interaction with Mobius directly.

## Running everything in docker. 
### User specific configuration
1. Update docker/config/application.properties to specify user specific values for following properties
```
 mobius.exogeni.user=kthare10
 mobius.exogeni.user=kthare10
 mobius.exogeni.KeyPath=./ssh/
 mobius.exogeni.certKeyFile=geni-kthare10.pem
 mobius.exogeni.sshKeyFile=id_rsa.pub
 ```
 2. Update docker/config/application.properties to specify exogeni controller url
```
 mobius.exogeni.controllerUrl=https://geni.renci.org:11443/orca/xmlrpc
```
3. If connecting to pegasus, specify amqp credentials. Alternatively, amqp notificationSink can be used as shown below. 
```
 #mobius.amqp.server.host=panorama.isi.edu
 #mobius.amqp.server.port=5672
 #mobius.amqp.use.ssl=false
 #mobius.amqp.user.name=anirban
 #mobius.amqp.user.password=75O2g%oy3DM$b8uz
 #mobius.amqp.virtual.host=panorama
 mobius.amqp.exchange.name=notifications
 mobius.amqp.exchange.routing.key=workflows
 mobius.amqp.server.host=localhost
 mobius.amqp.server.port=5672
 mobius.amqp.use.ssl=false
 mobius.amqp.user.name=
 mobius.amqp.user.password=
 mobius.amqp.virtual.host=
```
4. Update docker-compose.yml for mobius_server to point the below parameters to user specific locations.
```
        # point to user specific keys below
         volumes:
         - "./logs/:/var/log/"
         - "./config/application.properties:/code/config/application.properties"
         - "./mobius-sync:/code/mobius-sync"
         - "./ssh/geni-kthare10.pem:/code/ssh/geni-kthare10.pem"
         - "./ssh/id_rsa.pub:/code/ssh/id_rsa.pub"
```
### Run Docker
Run docker-compose up -d

```
$ docker-compose up -d
Creating database ... done
Creating rabbitmq ... done
Creating mobius   ... done
Creating notification ... done
```
After a few moments the docker containers will have stood up and configured themselves. User can now trigger requests to Mobius. Refer to [Interface](./mobius/Interface.md) to see the REST API
