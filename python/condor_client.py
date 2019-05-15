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
import socket


from mobius import *
from comet_common_iface import *

pubKeysVal={"val_":"[{\"publicKey\":\"\"}]"}
hostNameVal={"val_":"[{\"hostName\":\"REPLACE\",\"ip\":\"IPADDR\"}]"}


def is_valid_ipv4_address(address):
    try:
        socket.inet_pton(socket.AF_INET, address)
    except AttributeError:  # no inet_pton here, sorry
        try:
            socket.inet_aton(address)
        except socket.error:
            return False
        return address.count('.') == 3
    except socket.error:  # not a valid address
        return False

    return True

def can_ip_satisfy_range(ip, n):
    octets = ip.split('.')
    octets[3] = str(int(octets[3]) + n)
    print ("Last IP: " + '.'.join(octets))
    return is_valid_ipv4_address('.'.join(octets))

def get_next_ip(ip):
    octets = ip.split('.')
    octets[3] = str(int(octets[3]) + 1)
    print ("Next IP: " + '.'.join(octets))
    return '.'.join(octets)

def main():
     parser = argparse.ArgumentParser(description='Python client to create Condor cluster using mobius.\nUses master.json, submit.json and worker.json for compute requests present in data directory specified.\nCurrently only supports provisioning compute resources. Other resources can be provisioned via mobius_client.\nCreates COMET contexts for Chameleon resources and thus enables exchanging keys and hostnames within workflow')

     parser.add_argument(
         '-s',
         '--site',
         dest='site',
         type = str,
         help='Site at which resources must be provisioned; must be specified for create operation',
         required=False
     )
     parser.add_argument(
         '-n',
         '--workers',
         dest='workers',
         type = int,
         help='Number of workers to be provisioned; must be specified for create operation',
         required=False
     )
     parser.add_argument(
         '-c',
         '--comethost',
         dest='comethost',
         type = str,
         help='Comet Host e.g. https://18.218.34.48:8111/; used only for provisioning resources on chameleon',
         required=False
     )
     parser.add_argument(
         '-t',
         '--cert',
         dest='cert',
         type = str,
         help='Comet Certificate; used only for provisioning resources on chameleon',
         required=False
     )
     parser.add_argument(
         '-k',
         '--key',
         dest='key',
         type = str,
         help='Comet Certificate key; used only for provisioning resources on chameleon',
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
         '-i',
         '--ipStart',
         dest='ipStart',
         type = str,
         help='Start IP Address of the range of IPs to be used for VMs; 1st IP is assigned to master and subsequent IPs are assigned to submit node and workers; can be specified for create operation',
         required=False
     )
     parser.add_argument(
         '-l',
         '--leaseEnd',
         dest='leaseEnd',
         type = str,
         help='Lease End Time; can be specified for create operation',
         required=False
     )
     parser.add_argument(
         '-d',
         '--datadir',
         dest='datadir',
         type = str,
         help='Data directory where to look for master.json, submit.json and worker.json; must be specified for create operation',
         required=False
     )

     args = parser.parse_args()

     mb=MobiusInterface()

     if args.operation == 'get':
        print ("Getting status of workflow")
        response=mb.get_workflow(args.mobiushost, args.workflowId)
     elif args.operation == 'delete':
        print ("Deleting workflow")
        response=mb.delete_workflow(args.mobiushost, args.workflowId)
        if args.comethost is not None:
           print ("Cleaning up COMET context for workflow")
           comet=CometInterface(args.comethost, None, args.cert, args.key, None)
           response=comet.delete_families(args.comethost, args.workflowId, None, args.workflowId, args.workflowId)
     elif args.operation == 'create':
         ipMap = dict()
         if args.site is None or args.workers is None or args.datadir is None:
             print ("ERROR: site name, number of workers and data directory must be specified for create operation")
             parser.print_help()
             sys.exit(1)
         if args.ipStart is not None:
             if is_valid_ipv4_address(args.ipStart) == False :
                 print ("ERROR: Invalid start ip address specified")
                 parser.print_help()
                 sys.exit(1)
             if can_ip_satisfy_range(args.ipStart, args.workers + 1) == False:
                 print ("ERROR: Invalid start ip address specified; cannot accomdate the ip for all nodes")
                 parser.print_help()
                 sys.exit(1)
         if args.comethost is not None:
            if args.cert is None or args.key is None:
             print ("ERROR: comet certificate and key must be specified when comethost is indicated")
             parser.print_help()
             sys.exit(1)
         mdata = None
         sdata = None
         wdata = None
         m = None
         s = None
         w = None
         if "Chameleon" in args.site or "Exogeni" in args.site :
             m = args.datadir + "/master.json"
             s = args.datadir + "/submit.json"
             w = args.datadir + "/worker.json"
         else:
             print ("ERROR: Invalid site specified")
             parser.print_help()
             sys.exit(1)
         if os.path.exists(m):
             print ("Using " + m + " file for master data")
             m_f = open(m, 'r')
             mdata = json.load(m_f)
             m_f.close()
         else:
             print ("ERROR: json file for master node could not be found")
             parser.print_help()
             sys.exit(1)
         if os.path.exists(s):
             print ("Using " + s + " file for submit data")
             s_f = open(s, 'r')
             sdata = json.load(s_f)
             s_f.close()
         else:
             print ("ERROR: json file for submit node could not be found")
             parser.print_help()
             sys.exit(1)
         if os.path.exists(w):
             print ("Using " + w + " file for worker data")
             w_f = open(w, 'r')
             wdata = json.load(w_f)
             w_f.close()
         else:
             print ("ERROR: json file for worker node could not be found")
             parser.print_help()
             sys.exit(1)
         print ("Creating workflow")
         response=mb.create_workflow(args.mobiushost, args.workflowId)
         if response.json()["status"] == 200:
            mdata["site"]=args.site
            sdata["site"]=args.site
            wdata["site"]=args.site
            print ("Provisioning master node")
            nodename="Node0"
            if mdata["hostNamePrefix"] is not None:
               nodename=mdata["hostNamePrefix"]+"0"
            if args.ipStart is not None :
               mdata["ipAddress"] = args.ipStart
               ipMap[nodename] = args.ipStart
            if args.leaseEnd is not None:
               print ("Setting leaseEnd to " + args.leaseEnd)
               mdata["leaseEnd"] = args.leaseEnd
            if mdata["postBootScript"] is not None:
               s=mdata["postBootScript"]
               s=s.replace("WORKFLOW", args.workflowId)
               s=s.replace("NODENAME", nodename)
               mdata["postBootScript"]=s
            response=mb.create_compute(args.mobiushost, args.workflowId, mdata)
            if response.json()["status"] != 200:
                print ("Deleting workflow")
                response=mb.delete_workflow(args.mobiushost, args.workflowId)
                return
            print ("Provisioning submit node")
            nodename="Node1"
            if sdata["hostNamePrefix"] is not None:
               nodename=sdata["hostNamePrefix"]+"1"
            if args.ipStart is not None :
               args.ipStart = get_next_ip(args.ipStart)
               sdata["ipAddress"] = args.ipStart
               ipMap[nodename] = args.ipStart
            if args.leaseEnd is not None:
               print ("Setting leaseEnd to " + args.leaseEnd)
               sdata["leaseEnd"] = args.leaseEnd
            if sdata["postBootScript"] is not None:
               s=sdata["postBootScript"]
               s=s.replace("WORKFLOW", args.workflowId)
               s=s.replace("NODENAME",nodename)
               sdata["postBootScript"]=s
            response=mb.create_compute(args.mobiushost, args.workflowId, sdata)
            if response.json()["status"] != 200:
                print ("Deleting workflow")
                response=mb.delete_workflow(args.mobiushost, args.workflowId)
                return
            if args.leaseEnd is not None:
                print ("Setting leaseEnd to " + args.leaseEnd)
                wdata["leaseEnd"] = args.leaseEnd
            if wdata["postBootScript"] is not None:
                s=wdata["postBootScript"]
                s=s.replace("WORKFLOW", args.workflowId)
                wdata["postBootScript"]=s
            prefix = wdata["hostNamePrefix"]
            oldname = None
            for x in range(args.workers):
                print ("Provisioning worker: " + str(x))
                nodename="Node" + str(x+2)
                if prefix is not None:
                   nodename= prefix + str(x+2)
                if wdata["postBootScript"] is not None:
                   if oldname is None:
                       print ("Replacing NODENAME to " + nodename)
                       s=s.replace("NODENAME", nodename)
                   else:
                       print ("Replacing " + oldname + " to " + nodename)
                       s=s.replace(oldname, nodename)
                   oldname = nodename
                   wdata["postBootScript"]=s
                if args.ipStart is not None :
                    args.ipStart = get_next_ip(args.ipStart)
                    wdata["ipAddress"] = args.ipStart
                    ipMap[nodename] = args.ipStart
                response=mb.create_compute(args.mobiushost, args.workflowId, wdata)
                if response.json()["status"] != 200:
                    print ("Deleting workflow")
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
                            if "Chameleon" in args.site :
                                hostname=n["name"] + ".novalocal"
                            else :
                                hostname=n["name"]
                            hostVal = hostVal.replace("REPLACE", hostname)
                            if hostname in ipMap:
                                print ("Replacing IPADDR with " + ipMap[hostname])
                                hostVal = hostVal.replace("IPADDR", ipMap[hostname])
                            else:
                                print ("Replacing IPADDR with empty string")
                                hostVal = hostVal.replace("IPADDR", "")
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
