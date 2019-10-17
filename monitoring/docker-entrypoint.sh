#!/bin/sh
#python monitor_client.py -t $databasehost -k $kafkahost
python -u monitor_client.py -t $databasehost -d $database -u $user -p $password -m $mobiushost -k $kafkahost -c $tc -f $td -r $tm -b $bucketcount -l $leasedays
