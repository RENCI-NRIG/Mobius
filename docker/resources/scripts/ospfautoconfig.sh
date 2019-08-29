#!/bin/bash

while true ; do
  resifconfig=$(ifconfig)
  if [[ $resifconfig == *$1* ]]; then
      echo "It's there!"
      /bin/bash ~/ospfautoconfig.sh
      break
  fi
  sleep 2
done

