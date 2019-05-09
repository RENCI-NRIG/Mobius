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

defaultComputeChameleonData={
    "cpus":"4",
    "gpus":"0",
    "ramPerCpus":"4096",
    "diskPerCpus":"5000",
    "coallocate":"true",
    "hostNamePrefix":"master",
    "imageName":"CC-CentOS7"
}
defaultComputeExogeniData={
    "cpus":"4",
    "gpus":"0",
    "ramPerCpus":"3072",
    "diskPerCpus":"19200",
    "hostNamePrefix":"master",
    "coallocate":"true",
    "imageUrl":"http://geni-images.renci.org/images/standard/centos-comet/centos7.4-v1.0.3-comet/centos7.4-v1.0.3-comet.xml",
    "imageHash":"3dd17be8e0c24dd34b4dbc0f0d75a0b3f398c520",
    "imageName":"centos7.4-v1.0.3-comet",
    "leaseEnd":"1557733832"
}
defaultStorageChameleonData= {
     "mountPoint":"/mnt/",
     "target":"master0",
     "size":"1",
     "action":"add"
}
defaultStorageExogeniData= {
     "mountPoint":"/mnt/",
     "target":"master0",
     "size":"1",
     "action":"add"
}

def processCompute(args):
    if args.site is None:
        if args.data is None and args.file is None:
            print ("ERROR: data or file must be specified")
            return None
        if args.data is not None and args.file is not None:
            print ("ERROR: Either data or file must be specified")
            return None
    elif "Chameleon" in args.site :
        mdata=defaultComputeChameleonData
    elif "Exogeni" in args.site :
        mdata=defaultComputeExogeniData
    else:
        print ("ERROR: invalid site specified")
        return None

    if args.data is not None:
        mdata=json.loads(args.data)
    if args.file is not None:
        if os.path.exists(args.file):
            print ("Using file " + args.file)
            m_f = open(args.file, 'r')
            mdata=json.load(m_f)
            m_f.close()
        else:
            print ("ERROR: file does not exist")
            return None

    if args.data is None and args.file is None:
        mdata["site"]=args.site

    mb=MobiusInterface()
    response=mb.create_compute(args.mobiushost, args.workflowId, mdata)
    return response

def processStorage(args):
    if args.site is None:
        print ("ERROR: site must be specified")
        return None
    elif "Chameleon" in args.site :
        mdata=defaultStorageChameleonData
    elif "Exogeni" in args.site :
        mdata=defaultStorageExogeniData
    else:
        print ("ERROR: invalid site specified")
        return None

    if args.target is None and args.data is None and args.file is None:
        print ("ERROR: Either target or data or file must be specified")
        return None
    if args.target is not None and args.data is not None and args.file is not None:
        print ("ERROR: Either target or data or file must be specified")
        return None
    if args.data is not None and args.file is not None:
        print ("ERROR: Either data or file must be specified")
        return None
    if args.target is not None:
        mdata["target"]=args.target
    if args.data is not None:
        mdata=json.loads(args.data)
    if args.file is not None:
        if os.path.exists(args.file):
            f = open(args.file, 'r')
            mdata=json.load(f)
            f.close()
        else:
            print ("ERROR: file does not exist")
            return None
    mb=MobiusInterface()
    response=mb.create_storage(args.mobiushost, args.workflowId, mdata)
    return response

def processStitchPort(args):
    if args.data is None and args.file is None:
        print ("ERROR: data or file must be specified")
        return None
    if args.data is not None and args.file is not None:
        print ("ERROR: Either data or file must be specified")
        return None
    if args.data is not None:
        mdata=json.loads(args.data)
    if args.file is not None:
        if os.path.exists(args.file):
            f = open(args.file, 'r')
            mdata=json.load(f)
            f.close()
        else:
            print ("ERROR: file does not exist")
            return None

    mb=MobiusInterface()
    response=mb.create_stitchport(args.mobiushost, args.workflowId, mdata)
    return response

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
        help='Operation allowed values: post|get|delete; post - provision workflow or compute or storage or stitchport; get - get a workflow; delete - delete a workflow',
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
         help='data, JSON data to send; if not specified; default data is used; only used with post; must not be specified if target or file is indicated; must be specified for stitchport',
         required=False
     )
     parser.add_argument(
         '-f',
         '--file',
         dest='file',
         type = str,
         help='file, JSON file to send; if not specified; default data is used; only used with post; must not be specified if target or data is indicated; must be specified for stitchport',
         required=False
     )
     parser.add_argument(
         '-r',
         '--resourcetype',
         dest='resourcetype',
         type = str,
         help='resourcetype allowed values: workflow|compute|storage|stitchport; only used with post; must be specified',
         required=False
     )
     parser.add_argument(
         '-t',
         '--target',
         dest='target',
         type = str,
         help='target hostname of the server to which to attach storage; only used with resourcetype storage',
         required=False
     )

     args = parser.parse_args()


     if args.operation == 'get':
        mb=MobiusInterface()
        response=mb.get_workflow(args.mobiushost, args.workflowId)
     elif args.operation == 'delete':
        mb=MobiusInterface()
        response=mb.delete_workflow(args.mobiushost, args.workflowId)
     elif args.operation == 'post':
         if args.resourcetype is None:
            print ("ERROR: resourcetype must be specified")
            parser.print_help()
            sys.exit(1)

         if args.resourcetype == "workflow" :
             mb=MobiusInterface()
             response=mb.create_workflow(args.mobiushost, args.workflowId)
         elif args.resourcetype == "compute" :
             response = processCompute(args)
         elif args.resourcetype == "storage" :
             response = processStorage(args)
         elif args.resourcetype == "stitchPort" :
             response = processStitchPort(args)
         else :
            print ("ERROR:Not supported resourcetype")
            parser.print_help()
            sys.exit(1)
         if response is None:
            parser.print_help()
            sys.exit(1)
     else:
        print ("ERROR: invalid operation")
        parser.print_help()
        sys.exit(1)

     sys.exit(0)

if __name__ == '__main__':
    main()
