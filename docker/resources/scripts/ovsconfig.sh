#!/bin/bash
ovsbr1="00006eb27eeef44e"
#ovsbr2="0000d2e89483594b"
ovsbr3="00000ad958c3e04b"

#clean
#curl -X DELETE -d '{"route_id":"all"}' 152.3.136.36:8080/logRouter/$ovsbr1
#echo "\n"
#echo "\n"
##curl -X DELETE -d '{"route_id":"all"}' 152.3.136.36:8080/logRouter/$ovsbr2
#echo "\n"
#echo "\n"
#curl -X DELETE -d '{"route_id":"all"}' 152.3.136.36:8080/logRouter/$ovsbr3
#echo "\n"
#echo "\n"
#curl -X DELETE -d '{"address_id":"all"}' 152.3.136.36:8080/logRouter/$ovsbr1
#echo "\n"
#echo "\n"
#curl -X DELETE -d '{"address_id":"all"}' 152.3.136.36:8080/logRouter/$ovsbr2
#echo "\n"
#echo "\n"
#curl -X DELETE -d '{"address_id":"all"}' 152.3.136.36:8080/logRouter/$ovsbr3
#echo "\n"
curl -X POST -d '{"address":"192.168.1.1/24"}' 152.3.136.36:8080/logRouter/$ovsbr1
echo "\n"
echo "\n"
echo "\n"
echo "\nclear\n"
curl -X POST -d '{"address":"192.168.2.1/24"}' 152.3.136.36:8080/logRouter/$ovsbr1
echo "\n"
echo "\n"


curl -X POST -d '{"address":"192.168.2.2/24"}' 152.3.136.36:8080/logRouter/$ovsbr3
#echo "\n"
#echo "\n"
curl -X POST -d '{"address":"192.168.3.1/24"}' 152.3.136.36:8080/logRouter/$ovsbr3
##echo "\n"
##echo "\n"
###curl -X POST -d '{"destination":"192.168.4.2/24","gateway":"192.168.3.2"}' 152.3.136.36:8080/logRouter/$ovsbr2
###echo "\n"
###echo "\n"
##curl -X POST -d '{"destination":"192.168.1.1/24","gateway":"192.168.2.1"}' 152.3.136.36:8080/logRouter/$ovsbr2
##echo "\n"
##echo "\n"
#
#curl -X POST -d '{"address":"192.168.2.2/24"}' 152.3.136.36:8080/logRouter/$ovsbr3
#echo "\n"
#echo "\n"
#curl -X POST -d '{"address":"192.168.3.1/24"}' 152.3.136.36:8080/logRouter/$ovsbr3
##echo "\n"
echo "\n"
curl -X POST -d '{"destination":"192.168.1.1/24","gateway":"192.168.2.1"}' 152.3.136.36:8080/logRouter/$ovsbr3
##echo "\n"
curl -X POST -d '{"destination":"192.168.3.2/24","gateway":"192.168.2.2"}' 152.3.136.36:8080/logRouter/$ovsbr1
#echo "\n"
echo "\n"
echo "\n"
curl localhost:8080/logRouter/$ovsbr1
#echo "\n"
echo "\n"
curl localhost:8080/logRouter/$ovsbr3
