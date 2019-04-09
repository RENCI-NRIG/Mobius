import sys
import os
import time
import json
import argparse
import subprocess

from mobius import *
from comet_common_iface import *

defaultData={
        "cpus":"4",
        "gpus":"0",
        "ramPerCpus":"3072",
        "diskPerCpus":"19200",
        "hostNamePrefix":"master",
        "coallocate":"true",
        "imageUrl":"http://geni-images.renci.org/images/kthare10/ubuntu/dynamo/master/ubuntu-dynamo-master.xml",
        "imageHash":"584564de63d3325eadbae9dc90b271d439921e2d",
        "imageName":"ubuntu-dynamo-master",
        "leaseEnd":"1557733832"
        }

def main():
     parser = argparse.ArgumentParser(description='Python client to provision cloud resources by invoking Mobius REST Commands.\n')

     parser.add_argument(
         '-s',
         '--site',
         dest='site',
         type = str,
         help='Site',
         required=False
     )
     parser.add_argument(
         '-m',
         '--mobiushost',
         dest='mobiushost',
         type = str,
         help='Mobius Host e.g. http://localhost:8080/mobius',
         required=False,
         default='http://localhost:8080/mobius'
     )
     parser.add_argument(
        '-o',
        '--operation',
        dest='operation',
        type = str,
        help='Operation allowed values: create|get|delete',
        required=True
     )
     parser.add_argument(
         '-w',
         '--workflowId',
         dest='workflowId',
         type = str,
         help='workflowId',
         required=True
     )
     parser.add_argument(
         '-d',
         '--data',
         dest='data',
         type = str,
         help='data',
         required=True
     )

     args = parser.parse_args()

     mb=MobiusInterface()

     if args.operation == 'get':
        response=mb.get_workflow(args.mobiushost, args.workflowId)
     elif args.operation == 'delete':
        response=mb.delete_workflow(args.mobiushost, args.workflowId)
     elif args.operation == 'create':
         if args.site is None :
             parser.print_help()
             sys.exit(1)
         mdata=defaultData
         if args.data is not None:
             mdata=args.data
         else:
            mdata["site"]=args.site

         response=mb.create_workflow(args.mobiushost, args.workflowId)
         if response.json()["status"] == 200:
            response=mb.create_compute(args.mobiushost, args.workflowId, mdata)
            if response.json()["status"] != 200:
                response=mb.delete_workflow(args.mobiushost, args.workflowId)
                return
     else:
        parser.print_help()
        sys.exit(1)

     sys.exit(0)

if __name__ == '__main__':
    main()
