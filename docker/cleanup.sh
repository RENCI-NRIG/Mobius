#!/bin/sh
docker stop $(docker ps -aq)
docker rm $(docker ps -aq)
docker rmi -f $(docker images -q)
docker network rm $(docker network ls --format "{{.ID}}")
ocker volume rm $(docker volume ls -qf dangling=true)
