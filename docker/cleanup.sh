#!/bin/sh
docker stop database notification mobius rabbitmq sdxserver
docker rm database notification mobius rabbitmq sdxserver
docker rmi postgres:10 rencinrig/mobius:1.0.0-SNAPSHOT rabbitmq:management  rencinrig/notification:1.0.0-SNAPSHOT rencinrig/sdxserver:0.1-SNAPSHOT
#docker rmi rencinrig/mobius:1.0.0-SNAPSHOT rabbitmq:management  rencinrig/notification:1.0.0-SNAPSHOT
#docker network rm $(docker network ls --format "{{.ID}}")
docker volume rm $(docker volume ls -qf dangling=true)
