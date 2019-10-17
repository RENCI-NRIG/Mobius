# Table of contents

 - [Description](#descr)
 - [Installation](#install)
 - [Monitoring Client](#mclient)
   - [Usage](#usage)
# <a name="descr"></a>Description
The Monitoring and Control module is designed to transparently maintain the quality of service of the provisioned end-to-end infrastructure through continuous monitoring and control. Based on policies and thresholds defined, the goal is to identify the appropriate actions to be taken to ensure the infrastructure QoS at all times. The actions include enabling compute, storage and network elasticity i.e. growing and shrinking compute or storage resource pools and increasing or decreasing network properties of links. 

This module is currently under development. Its supports following features so far:
- Growth of the compute pools when certain configured thresholds are crossed for configured number of monitoring buckets
- Thresholds are:
  - Idle CPU Usage Threshold (Default 15%)
  - Disk Usage Threshold (Default 85%)
  - RAM Usage Threshold (Default 85%)  

# <a name="install"></a>Installation
Monitoring client run inside a docker container. It can be either brought up using the following docker-compose.yml file.
Before bringing up the container, user is expected to modify the configurable parameters in docker-compose.yml.

Parameters are explained here:
- kafkahost : IP Address of the server/container running the kafka where monitoring data is pushed by the workflow resources. Users are required to modify this value.
- databasehost : hostname of the container running Postgres database where the workflow information is maintained. Default value of 'database' is configured.
- database : Name of the Postgres database where the workflow information is maintained. Default value of 'mobius' is configured.
- user : Postgres database user name. Default value of 'mobius' is configured.
- password : Postgres database user password. Default value of 'mobius' is configured.
- mobiushost : hostname of the container running Mobius. Default value of 'mobius' is configured.
- tc : Idle CPU Threshold percentage which when deceeded for last n monitoring buckets below result in Monitoring client will trigger provisioning of a similar compute resource.
- td : Disk usage threshold percentage which when exceeded for last n monitoring buckets below result in Monitoring client will trigger provisioning of a similar compute resource.
- tm : RAM usage threshold percentage which when exceeded for last n monitoring buckets below result in Monitoring client will trigger provisioning of a similar compute resource.
- bucketcount : Number of the monitoring buckets to check before the threshold crossed action is triggered.
- leasedays : Number of the days for which the lease is requested for the resources provisioned by Monitoring client.
```
version: '3.6'

services:
    monitoring:
        container_name: monitoring
        hostname: monitoring
        image: rencinrig/monitoring:latest
        restart: always
        environment:
        - kafkahost=18.223.195.153:9092
        - databasehost=database
        - database=mobius
        - user=mobius
        - password=mobius
        - mobiushost=http://mobius:8080/mobius
        - tc=15
        - td=85
        - tm=85
        - bucketcount=10
        - leasedays=2
```
# <a name="mclient"></a>Monitoring client
Python based monitoring client which periodically checks all the workflows and their respective compute resources for CPU, RAM and Disk usage. If any of the CPU, Disk or RAM usage exceeds the configured thresholds, monitioring client provisions similar resources in the workflow by sending REST requests to Mobius.

## <a name="usage"></a>Usage
```
usage: monitor_client.py [-h] [-t DBHOST] [-d DATABASE] [-u USER]
                         [-p PASSWORD] -k KAFKAHOST [-m MOBIUSHOST]
                         [-c CPUTHRESHOLD] [-f DISKTHRESHOLD]
                         [-r MEMTHRESHOLD] [-b BUCKETCOUNT] [-l LEASEDAYS]

Python client to monitor workflows and provision additional resources if
required

optional arguments:
  -h, --help            show this help message and exit
  -t DBHOST, --dbhost DBHOST
                        Database Host from which to read the Workflow Names
  -d DATABASE, --database DATABASE
                        Database Name
  -u USER, --user USER  Database User Name
  -p PASSWORD, --password PASSWORD
                        Database Password
  -k KAFKAHOST, --kafkahost KAFKAHOST
                        Kafka Host
  -m MOBIUSHOST, --mobiushost MOBIUSHOST
                        Mobius Host e.g. http://localhost:8080/mobius
  -c CPUTHRESHOLD, --cputhreshold CPUTHRESHOLD
                        Threshold for idle cpu usage in percent; when idle cpu
                        usage is less than the threshold, additional compute
                        nodes are requested via Mobius
  -f DISKTHRESHOLD, --diskthreshold DISKTHRESHOLD
                        Threshold for disk usage in percent; when disk usage
                        exceeds the threshold, additional compute nodes are
                        requested via Mobius
  -r MEMTHRESHOLD, --memthreshold MEMTHRESHOLD
                        Threshold for memory usage in percent; when memory
                        usage exceeds the threshold, additional compute nodes
                        are requested via Mobius
  -b BUCKETCOUNT, --bucketcount BUCKETCOUNT
                        Number of buckets for which the threshold should be
                        exceeded to trigger provisioning
  -l LEASEDAYS, --leasedays LEASEDAYS
                        Number of days for which lease should be requested
```
