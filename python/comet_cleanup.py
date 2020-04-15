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


from comet_common_iface import *

def main():
    parser = argparse.ArgumentParser()

    parser.add_argument(
        '-c',
        '--comethost',
        dest='comethost',
        type = str,
        help='Comet Host default(https://comet-hn1.exogeni.net:8111/) used only for provisioning resources on chameleon',
        required=False,
        default='https://comet-hn1.exogeni.net:8111/'
    )
    parser.add_argument(
        '-t',
        '--cert',
        dest='cert',
        type = str,
        help='Comet Certificate default(certs/inno-hn_exogeni_net.pem); used only for provisioning resources on chameleon',
        required=False,
        default='certs/inno-hn_exogeni_net.pem'
    )
    parser.add_argument(
        '-k',
        '--key',
        dest='key',
        type = str,
        help='Comet Certificate key default(certs/inno-hn_exogeni_net.key); used only for provisioning resources on chameleon',
        required=False,
        default='certs/inno-hn_exogeni_net.key'
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

    if args.comethost is not None and args.workflowId is not None:
        print ("Cleaning up COMET context for workflow")
        comet=CometInterface(args.comethost, None, args.cert, args.key, None)
        readToken=args.workflowId + "read"
        writeToken=args.workflowId + "write"
        response=comet.delete_families(args.comethost, args.workflowId, None, readToken, writeToken)
    else:
        parser.print_help()
        sys.exit(1)

    sys.exit(0)


if __name__ == '__main__':
    main()
