#
# Copyright (c) 2017 Renaissance Computing Institute, except where noted.
# All rights reserved.
#
# This software is released under GPLv2
#
# Renaissance Computing Institute,
# (A Joint Institute between the University of North Carolina at Chapel Hill,
# North Carolina State University, and Duke University)
# http://www.renci.org
#
# For questions, comments please contact software@renci.org
#
# Author: Komal Thareja(kthare10@renci.org)

import sys
import os
import time
import json
import argparse
import subprocess

from mobius import *
from comet_common_iface import *

pubKeysVal={"val_":"[{\"publicKey\":\"\"}]"}
hostNameVal={"val_":"[{\"hostName\":\"REPLACE\",\"ip\":\"\"}]"}

def main():
     parser = argparse.ArgumentParser(description='Python client to create Condor cluster using mobius.\nUses json object for compute requests present in data directory if present, otherwises uses the default.\nCurrently only supports provisioning compute resources.\nCreates COMET contexts for Chameleon resources and thus enables exchanging keys and hostnames within workflow')

     parser.add_argument(
         '-s',
         '--site',
         dest='site',
         type = str,
         help='Site',
         required=False
     )
     parser.add_argument(
         '-n',
         '--workers',
         dest='workers',
         type = str,
         help='Number of workers',
         required=False
     )
     parser.add_argument(
         '-c',
         '--comethost',
         dest='comethost',
         type = str,
         help='Comet Host e.g. https://18.218.34.48:8111/',
         required=False
     )
     parser.add_argument(
         '-t',
         '--cert',
         dest='cert',
         type = str,
         help='Comet Certificate',
         required=False
     )
     parser.add_argument(
         '-k',
         '--key',
         dest='key',
         type = str,
         help='Comet Certificate key',
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

     args = parser.parse_args()

     mb=MobiusInterface()

     if args.operation == 'get':
        response=mb.get_workflow(args.mobiushost, args.workflowId)
     elif args.operation == 'delete':
        response=mb.delete_workflow(args.mobiushost, args.workflowId)
        if args.comethost is not None:
           print ("Cleaning up COMET context for workflow")
           comet=CometInterface(args.comethost, None, args.cert, args.key, None)
           response=comet.delete_families(args.comethost, args.workflowId, None, args.workflowId, args.workflowId)
     elif args.operation == 'create':
         if args.site is None or args.workers is None:
             parser.print_help()
             sys.exit(1)
         if args.comethost is not None:
            if args.cert is None or args.key is None:
             parser.print_help()
             sys.exit(1)
         mdata = None
         sdata = None
         wdata = None
         m = None
         s = None
         w = None

         if "Chameleon" in args.site :
             m='./data/chmaster.json'
             s='./data/chsubmit.json'
             w='./data/chworker.json'
         elif "Exogeni" in args.site :
             m='./data/master.json'
             s='./dara/submit.json'
             w='./data/worker.json'
         else:
             parser.print_help()
             sys.exit(1)
         if os.path.exists(m):
             print ("Using " + m + " file for master data")
             m_f = open(m, 'r')
             mdata = json.load(m_f)
             m_f.close()
         else:
             parser.print_help()
             sys.exit(1)
         if os.path.exists(s):
             print ("Using " + s + " file for submit data")
             s_f = open(s, 'r')
             sdata = json.load(s_f)
             s_f.close()
         else:
             parser.print_help()
             sys.exit(1)
         if os.path.exists(w):
             print ("Using " + w + " file for worker data")
             w_f = open(w, 'r')
             wdata = json.load(w_f)
             w_f.close()
         else:
             parser.print_help()
             sys.exit(1)
         response=mb.create_workflow(args.mobiushost, args.workflowId)
         if response.json()["status"] == 200:
            mdata["site"]=args.site
            sdata["site"]=args.site
            wdata["site"]=args.site
            response=mb.create_compute(args.mobiushost, args.workflowId, mdata)
            if response.json()["status"] != 200:
                response=mb.delete_workflow(args.mobiushost, args.workflowId)
                return
            response=mb.create_compute(args.mobiushost, args.workflowId, sdata)
            if response.json()["status"] != 200:
                response=mb.delete_workflow(args.mobiushost, args.workflowId)
                return
            for x in args.workers:
                print ("Provisioning worker: " + x)
                response=mb.create_compute(args.mobiushost, args.workflowId, wdata)
                if response.json()["status"] != 200:
                    response=mb.delete_workflow(args.mobiushost, args.workflowId)
                    return
            response=mb.get_workflow(args.mobiushost, args.workflowId)
            if response.json()["status"] == 200 and args.comethost is not None:
                print ("Setting up COMET for exchanging host names and keys")
                comet=CometInterface(args.comethost, None, args.cert, args.key, None)
                readToken=args.workflowId + "read"
                writeToken=args.workflowId + "write"
                status=json.loads(response.json()["value"])
                requests = json.loads(status["workflowStatus"])
                for req in requests:
                    slices = req["slices"]
                    for s in slices:
                        nodes = s["nodes"]
                        for n in nodes :
                            print ("Create comet context for node " + n["name"])
                            response=comet.update_family(args.comethost, args.workflowId, n["name"],
                                    readToken, writeToken, "pubkeysall", pubKeysVal)
                            print ("Received Response Status Code: " + str(response.status_code))
                            print ("Received Response Message: " + response.json()["message"])
                            print ("Received Response Status: " + response.json()["status"])
                            if response.status_code == 200 :
                                print ("Received Response Value: " + str(response.json()["value"]))

                            hostVal = json.dumps(hostNameVal)
                            hostname=n["name"] + ".novalocal"
                            hostVal = hostVal.replace("REPLACE", hostname)
                            val = json.loads(hostVal)
                            response=comet.update_family(args.comethost, args.workflowId, n["name"],
                                    readToken, writeToken, "hostsall", val)
                            print ("Received Response Status Code: " + str(response.status_code))
                            print ("Received Response Message: " + response.json()["message"])
                            print ("Received Response Status: " + response.json()["status"])
                            if response.status_code == 200 :
                                print ("Received Response Value: " + str(response.json()["value"]))
     else:
        parser.print_help()
        sys.exit(1)

     sys.exit(0)

if __name__ == '__main__':
    main()
