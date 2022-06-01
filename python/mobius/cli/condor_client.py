#!/usr/bin/env python3
# MIT License
#
# Copyright (c) 2020 RENCI NRIG
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#
#
# Author: Komal Thareja (kthare10@renci.org)
import sys
import os
import time
import json
import argparse
import socket
import glob
from ssl import create_default_context, Purpose

from kafka import *

resourcesVal={"val_":"[{\"cpu\":\"\"},{\"memory\":\"\"}, {\"disk\":\"\"}]"}
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
    print("Last IP: " + '.'.join(octets))
    return is_valid_ipv4_address('.'.join(octets))


def get_cidr(ip):
    octets = ip.split('.')
    octets[3] = '0/24'
    print("CIDR: " + '.'.join(octets))
    return '.'.join(octets)


def get_cidr_escape(ip):
    octets = ip.split('.')
    octets[3] = '0\/24'
    print("CIDR: " + '.'.join(octets))
    return '.'.join(octets)


def get_default_ip_for_condor(ip):
    octets = ip.split('.')
    octets[3] = '*'
    print("Default IP: " + '.'.join(octets))
    return '.'.join(octets)


def get_next_ip(ip):
    octets = ip.split('.')
    octets[3] = str(int(octets[3]) + 1)
    print("Next IP: " + '.'.join(octets))
    return '.'.join(octets)


def get_cidr_subnet_sdx(ip):
    octets = ip.split('.')
    octets[3] = '1/24'
    print("SDX CIDR: " + '.'.join(octets))
    return '.'.join(octets)


def get_sdx_controller_ip(ip):
    octets = ip.split('.')
    octets[3] = str(int(octets[3]) - 1) + '/24'
    print("Chameleon Controller IP: " + '.'.join(octets))
    return '.'.join(octets)


def validate(args, parser):
    if (args.exogenisite is None and args.chameleonsite is None and args.jetstreamsite is None and args.mossite is None)\
            or (args.exoworkers is None and args.chworkers is None and args.jtworkers is None and args.mosworkers is None)\
            or (args.exodatadir is None and args.chdatadir is None and args.jtdatadir is None and args.mosdatadir is None) :
        print("ERROR: site name, number of workers and data directory must be specified for create operation")
        parser.print_help()
        sys.exit(1)
    if args.exoipStart is not None:
        if not is_valid_ipv4_address(args.exoipStart):
            print("ERROR: Invalid start ip address specified")
            parser.print_help()
            sys.exit(1)
        if not can_ip_satisfy_range(args.exoipStart, args.exoworkers + 1):
            print("ERROR: Invalid start ip address specified; cannot accommodate the ip for all nodes")
            parser.print_help()
            sys.exit(1)
    if args.chipStart is not None:
        if not is_valid_ipv4_address(args.chipStart):
            print("ERROR: Invalid start ip address specified")
            parser.print_help()
            sys.exit(1)
        if not can_ip_satisfy_range(args.chipStart, args.chworkers + 1):
            print("ERROR: Invalid start ip address specified; cannot accommodate the ip for all nodes")
            parser.print_help()
            sys.exit(1)
    if args.jtipStart is not None:
        if not is_valid_ipv4_address(args.jtipStart):
            print("ERROR: Invalid start ip address specified")
            parser.print_help()
            sys.exit(1)
        if not can_ip_satisfy_range(args.jtipStart, args.jtworkers + 1):
            print("ERROR: Invalid start ip address specified; cannot accommodate the ip for all nodes")
            parser.print_help()
            sys.exit(1)
    if args.mosipStart is not None:
        if not is_valid_ipv4_address(args.mosipStart):
            print("ERROR: Invalid start ip address specified")
            parser.print_help()
            sys.exit(1)
        if not can_ip_satisfy_range(args.mosipStart, args.mosworkers + 1):
            print("ERROR: Invalid start ip address specified; cannot accommodate the ip for all nodes")
            parser.print_help()
            sys.exit(1)
    if args.comethost is not None:
        if args.cert is None or args.key is None:
            print("ERROR: comet certificate and key must be specified when comethost is indicated")
            parser.print_help()
            sys.exit(1)
    if args.chameleonsite is not None:
        if "Chameleon" not in args.chameleonsite :
            print("ERROR: Invalid site specified")
            parser.print_help()
            sys.exit(1)
    if args.exogenisite is not None:
        if "Exogeni" not in args.exogenisite :
            print("ERROR: Invalid site specified")
            parser.print_help()
            sys.exit(1)
    if args.jetstreamsite is not None:
        if "Jetstream" not in args.jetstreamsite :
            print("ERROR: Invalid site specified")
            parser.print_help()
            sys.exit(1)
    if args.mossite is not None:
        if "Mos" not in args.mossite :
            print("ERROR: Invalid site specified")
            parser.print_help()
            sys.exit(1)


def set_up_parser():
    parser = argparse.ArgumentParser(description=f'Python client to create Condor cluster using client.'
                                                 f'\nUses master.json, submit.json and worker.json for compute requests '
                                                 f'present in data directory specified.\nCurrently only supports '
                                                 f'provisioning compute resources. Other resources can be provisioned '
                                                 f'via mobius_client.\nCreates COMET contexts for Chameleon resources '
                                                 f'and thus enables exchanging keys and hostnames within workflow')

    parser.add_argument(
        '-s1',
        '--exogenisite',
        dest='exogenisite',
        type=str,
        help='Exogeni Site at which resources must be provisioned; must be specified for create operation',
        required=False
    )
    parser.add_argument(
        '-s2',
        '--chameleonsite',
        dest='chameleonsite',
        type=str,
        help='Chameleon Site at which resources must be provisioned; must be specified for create operation',
        required=False
    )
    parser.add_argument(
        '-s3',
        '--jetstreamsite',
        dest='jetstreamsite',
        type=str,
        help='Jetstream Site at which resources must be provisioned; must be specified for create operation',
        required=False
    )
    parser.add_argument(
        '-s4',
        '--mossite',
        dest='mossite',
        type=str,
        help='Mass Open Cloud Site at which resources must be provisioned; must be specified for create operation',
        required=False
    )
    parser.add_argument(
        '-n1',
        '--exoworkers',
        dest='exoworkers',
        type = int,
        help='Number of workers to be provisioned on Exogeni; must be specified for create operation',
        required=False
    )
    parser.add_argument(
        '-n2',
        '--chworkers',
        dest='chworkers',
        type = int,
        help='Number of workers to be provisioned on Chameleon; must be specified for create operation',
        required=False
    )
    parser.add_argument(
        '-n3',
        '--jtworkers',
        dest='jtworkers',
        type = int,
        help='Number of workers to be provisioned on Jetstream; must be specified for create operation',
        required=False
    )
    parser.add_argument(
        '-n4',
        '--mosworkers',
        dest='mosworkers',
        type = int,
        help='Number of workers to be provisioned on Mass Open Cloud; must be specified for create operation',
        required=False
    )
    parser.add_argument(
        '-c',
        '--comethost',
        dest='comethost',
        type=str,
        help='Comet Host default(https://comet-hn1.exogeni.net:8111/) used only for provisioning resources on chameleon',
        required=False,
        default='https://comet-hn1.exogeni.net:8111/'
    )
    parser.add_argument(
        '-t',
        '--cert',
        dest='cert',
        type=str,
        help='Comet Certificate default(certs/client.pem); used only for provisioning resources on chameleon',
        required=False,
        default='certs/client.pem'
    )
    parser.add_argument(
        '-k',
        '--key',
        dest='key',
        type=str,
        help='Comet Certificate key default(certs/client.key); used only for provisioning resources on chameleon',
        required=False,
        default='certs/client.key'
    )
    parser.add_argument(
        '-m',
        '--mobiushost',
        dest='mobiushost',
        type=str,
        help='Mobius Host e.g. http://localhost:8080/mobius',
        required=False,
        default='http://localhost:8080/mobius'
    )
    parser.add_argument(
        '-o',
        '--operation',
        dest='operation',
        type=str,
        help='Operation allowed values: create|get|delete|list|add',
        required=True
    )
    parser.add_argument(
        '-w',
        '--workflowId',
        dest='workflowId',
        type=str,
        help='workflowId',
        required=False
    )
    parser.add_argument(
        '-i1',
        '--exoipStart',
        dest='exoipStart',
        type=str,
        help=f'Exogeni Start IP Address of the range of IPs to be used for VMs; 1st IP is assigned to master and '
             f'subsequent IPs are assigned to submit node and workers; can be specified for create operation',
        required=False
    )
    parser.add_argument(
        '-i2',
        '--chipStart',
        dest='chipStart',
        type=str,
        help=f'Chameleon Start IP Address of the range of IPs to be used for VMs; 1st IP is assigned to master and '
             f'subsequent IPs are assigned to submit node and workers; can be specified for create operation',
        required=False
    )
    parser.add_argument(
        '-i3',
        '--jtipStart',
        dest='jtipStart',
        type=str,
        help=f'Jetstream Start IP Address of the range of IPs to be used for VMs; 1st IP is assigned to master and '
             f'subsequent IPs are assigned to submit node and workers; can be specified for create operation',
        required=False
    )
    parser.add_argument(
        '-i4',
        '--mosipStart',
        dest='mosipStart',
        type=str,
        help=f'Mass Open Cloud Start IP Address of the range of IPs to be used for VMs; 1st IP is assigned to master '
             f'and subsequent IPs are assigned to submit node and workers; can be specified for create operation',
        required=False
    )
    parser.add_argument(
        '-l',
        '--leaseEnd',
        dest='leaseEnd',
        type=str,
        help='Lease End Time; can be specified for create operation',
        required=False
    )
    parser.add_argument(
        '-d1',
        '--exodatadir',
        dest='exodatadir',
        type=str,
        help=f'Exogeni Data directory where to look for master.json, submit.json and worker.json; '
             f'must be specified for create operation',
        required=False
    )
    parser.add_argument(
        '-d2',
        '--chdatadir',
        dest='chdatadir',
        type=str,
        help=f'Chameleon Data directory where to look for master.json, submit.json and worker.json; '
             f'must be specified for create operation',
        required=False
    )
    parser.add_argument(
        '-d3',
        '--jtdatadir',
        dest='jtdatadir',
        type=str,
        help=f'Jetstream Data directory where to look for master.json, submit.json and worker.json; '
             f'must be specified for create operation',
        required=False
    )
    parser.add_argument(
        '-d4',
        '--mosdatadir',
        dest='mosdatadir',
        type=str,
        help=f'Mass Open Cloud Data directory where to look for master.json, submit.json and worker.json; '
             f'must be specified for create operation',
        required=False
    )
    parser.add_argument(
        '-kh',
        '--kafkahost',
        dest='kafkahost',
        type=str,
        help='Kafka Host - monitoring server; must be specified for delete operation',
        default='dynamo-broker1.exogeni.net:9093',
        required=False
    )
    return parser


def extract_exogeni_slice_name(response):
    if response.json()["status"] != 200:
        return None, 0
    status=json.loads(response.json()["value"])
    requests = json.loads(status["workflowStatus"])
    slice_name = None
    count = 0
    for req in requests:
        slices = req["slices"]
        for s in slices:
            if "Exogeni" in req["site"] and slice_name is None:
                slice_name = s["slice"]
            nodes = s["nodes"]
            for n in nodes :
                if n["name"] == "cmnw" :
                   continue
                count += 1
    return slice_name, count


def main():
    parser = set_up_parser()
    args = parser.parse_args()
    mb = MobiusInterface()

    if args.operation == 'get':
        print("Getting status of workflow")
        response = mb.get_workflow(args.mobiushost, args.workflowId)
    elif args.operation == 'list':
        print("List workflows")
        response = mb.list_workflows(args.mobiushost)
    elif args.operation == 'delete':
        print("Deleting workflow")
        getresponse = mb.get_workflow(args.mobiushost, args.workflowId)
        response = mb.delete_workflow(args.mobiushost, args.workflowId)
        cleanup_monitoring(args.kafkahost, getresponse)
    elif args.operation == 'create' or args.operation == 'add':
        ipMap = dict()
        validate(args, parser)
        response = None
        exxogeni_slice_name = None
        count = 0
        if args.operation == 'create':
            print("Creating workflow")
            response = mb.create_workflow(args.mobiushost, args.workflowId)
        else:
            print("Get workflow")
            response = mb.get_workflow(args.mobiushost, args.workflowId)
            exxogeni_slice_name, count = extract_exogeni_slice_name(response)
        networkdata = None
        stitchdata = None
        chstoragename = None
        exostoragename = None
        if response.json()["status"] == 200:
            submitSubnet=None
            sip=None
            # Determine Stitching IP for storage node to be used for configuring routes on chameleon
            if args.exogenisite is not None and args.exodatadir is not None:
                d = args.exodatadir + "/network.json"
                if os.path.exists(d):
                    d_f = open(d, 'r')
                    networkdata = json.load(d_f)
                    d_f.close()
                d = args.exodatadir + "/stitch.json"
                if os.path.exists(d):
                    d_f = open(d, 'r')
                    stitchdata = json.load(d_f)
                    d_f.close()
                    sip = stitchdata["stitchIP"]
                d = args.exodatadir + "/storage.json"
                if os.path.exists(d):
                    d_f = open(d, 'r')
                    submitdata = json.load(d_f)
                    d_f.close()
                    ip = submitdata["stitchIP"]
                    submitSubnet = get_cidr(ip)
            if args.chameleonsite is not None and args.chdatadir is not None:
                exogeniSubnet = None
                if args.exoipStart is not None:
                    exogeniSubnet = get_cidr(args.exoipStart)
                set_up_network_data(args.chipStart, networkdata, None, args.exoipStart)
                tempsip = None
                if networkdata is not None:
                    tempsip = networkdata["chameleonSdxControllerIP"]
                    tempsip = tempsip.split("/",1)[0]
                status, count, chstoragename = provision_storage(args, args.chdatadir, args.chameleonsite, ipMap, count,
                                                                 args.chipStart, submitSubnet, tempsip, exogeniSubnet,
                                                                 None)
                if status == False:
                    return
                if chstoragename is not None:
                    chstoragename = chstoragename + ".novalocal"
                set_up_network_data(args.chipStart, networkdata, chstoragename, args.exoipStart)
            if args.exogenisite is not None and args.exodatadir is not None:
                status, count, exostoragename = provision_storage(args, args.exodatadir, args.exogenisite, ipMap, count,
                                                                  args.exoipStart, submitSubnet, sip, None, None)
                if status == False :
                    return
            if args.chameleonsite is not None and args.chdatadir is not None:
                exogeniSubnet = None
                if args.exoipStart is not None:
                    exogeniSubnet = get_cidr(args.exoipStart)
                forwardIP = None
                if stitchdata is not None:
                    forwardIP = stitchdata["stitchIP"]
                if networkdata is not None:
                    forwardIP = networkdata["chameleonSdxControllerIP"]
                    forwardIP = forwardIP.split("/",1)[0]
                status, count = provision_condor_cluster(args, args.chdatadir, args.chameleonsite, ipMap, count,
                                                         args.chipStart, args.chworkers, chstoragename, exogeniSubnet,
                                                         forwardIP, submitSubnet, None)
                if not status:
                    return
                print("ipMap after chameleon: " + str(ipMap))
            if args.exogenisite is not None and args.exodatadir is not None:
                chSubnet = None
                forwardIP = None
                if args.chipStart is not None:
                    chSubnet = get_cidr(args.chipStart)
                if exostoragename is not None:
                    forwardIP = ipMap[exostoragename]
                status, count = provision_condor_cluster(args, args.exodatadir, args.exogenisite, ipMap, count,
                                                         args.exoipStart, args.exoworkers, exostoragename, chSubnet,
                                                         forwardIP, submitSubnet, exxogeni_slice_name)
                if status == False :
                    return
                print("ipMap after exogeni: "  + str(ipMap))
            if args.jetstreamsite is not None and args.jtdatadir is not None:
                status, count = provision_condor_cluster(args, args.jtdatadir, args.jetstreamsite, ipMap, count,
                                                         args.jtipStart, args.jtworkers, None, None, None, None, None)
                if status == False:
                    return
                print("ipMap after jetstream: "  + str(ipMap))
            if args.mossite is not None and args.mosdatadir is not None:
                status, count = provision_condor_cluster(args, args.mosdatadir, args.mossite, ipMap, count,
                                                         args.mosipStart, args.mosworkers, None, None, None, None, None)
                if status == False:
                    return
                print("ipMap after mos: "  + str(ipMap))
            response = mb.get_workflow(args.mobiushost, args.workflowId)
            stitcVlanToChameleon = None
            if response.json()["status"] == 200 and args.comethost is not None:
                status=json.loads(response.json()["value"])
                requests = json.loads(status["workflowStatus"])
                stitchNodeStatus = None
                for req in requests:
                    if "Chameleon" in req["site"]:
                        if "vlan" in req:
                            stitcVlanToChameleon = str(req["vlan"])
                    slices = req["slices"]
                    for s in slices:
                        nodes = s["nodes"]
                        for n in nodes :
                            if "Chameleon" in s["slice"] or "Jetstream" in s["slice"] or "mos" in s["slice"]:
                                hostname=n["name"] + ".novalocal"
                            else :
                                hostname=n["name"]
                                if stitchdata is not None:
                                    if stitchdata["target"] in n["name"]:
                                        print("Updating target in exogeni stitch request")
                                        stitchdata["target"]=n["name"]
                                        stitchNodeStatus = n["state"]
                                if networkdata is not None:
                                    if networkdata["source"] in n["name"]:
                                        print("Updating target in exogeni network request")
                                        networkdata["source"] = n["name"]

                            if n["name"] == "cmnw" :
                                continue
            requests = None
            if stitcVlanToChameleon is not None and args.exogenisite is not None and \
                    (stitchdata is not None or networkdata is not None):
                target = None
                if stitchdata is not None:
                    target = stitchdata["target"]
                if networkdata is not None:
                    target = networkdata["source"]
                while stitchNodeStatus != "Active":
                    print("Waiting for the " + target + " to become active")
                    response = mb.get_workflow(args.mobiushost, args.workflowId)
                    if response.json()["status"] == 200 :
                        status=json.loads(response.json()["value"])
                        requests = json.loads(status["workflowStatus"])
                        for req in requests:
                            if "Exogeni" in req["site"] :
                                slices = req["slices"]
                                for s in slices:
                                    nodes = s["nodes"]
                                    for n in nodes :
                                        if target == n["name"] :
                                            print("Updating state of " + target)
                                            stitchNodeStatus = n["state"]
                    print("Sleeping for 5 seconds")
                    time.sleep(5)
                if networkdata is not None:
                    chnode = networkdata["destination"]
                    stitchNodeStatus = "TEMP"
                    while stitchNodeStatus != "ACTIVE" :
                        print("Waiting for the " + chnode + " to become active")
                        response = mb.get_workflow(args.mobiushost, args.workflowId)
                        if response.json()["status"] == 200 :
                            status=json.loads(response.json()["value"])
                            requests = json.loads(status["workflowStatus"])
                            for req in requests:
                                if "Chameleon" in req["site"] :
                                    slices = req["slices"]
                                    for s in slices:
                                        nodes = s["nodes"]
                                        for n in nodes :
                                            if chnode == n["name"] :
                                                print("Updating state of " + chnode)
                                                stitchNodeStatus = n["state"]
                        print("Sleeping for 5 seconds")
                        time.sleep(5)

                time.sleep(60)

                print("stitcVlanToChameleon = " + stitcVlanToChameleon)
                print("perform stitching")
                if stitchdata is not None:
                    perform_stitch(mb, args, args.exodatadir, args.exogenisite, stitcVlanToChameleon, stitchdata)
                if networkdata is not None:
                    perform_network_request(mb, args, args.exodatadir, args.exogenisite, networkdata)

            if requests is None:
                response = mb.get_workflow(args.mobiushost, args.workflowId)
                if response.json()["status"] == 200 :
                    status = json.loads(response.json()["value"])
                    requests = json.loads(status["workflowStatus"])

            if args.exodatadir is not None:
                push_prefix(mb, args, args.exodatadir, requests)
                push_scripts(mb, args, args.exodatadir, requests)

            if args.chdatadir is not None:
                push_prefix(mb, args, args.chdatadir, requests)
                push_scripts(mb, args, args.chdatadir, requests)

            if args.jtdatadir is not None:
                push_scripts(mb, args, args.jtdatadir, requests)

            if args.mosdatadir is not None:
                push_scripts(mb, args, args.mosdatadir, requests)
    else:
        parser.print_help()
        sys.exit(1)

    sys.exit(0)


def perform_network_request(mb, args, datadir, site, data):
    if data is None:
        d = datadir + "/network.json"
        if os.path.exists(d):
            print("Using " + d + " file for stitch data")
            d_f = open(d, 'r')
            data = json.load(d_f)
            d_f.close()
    if data is not None:
        print("payload for network request" + str(data))
        response = mb.create_network(args.mobiushost, args.workflowId, data)


def perform_stitch(mb, args, datadir, site, vlan, data):
    if data is None:
        d = datadir + "/stitch.json"
        if os.path.exists(d):
            print("Using " + d + " file for stitch data")
            d_f = open(d, 'r')
            data = json.load(d_f)
            d_f.close()
    if data is not None:
        data["tag"] = vlan
        print("payload for stitch request" + str(data))
        response = mb.create_stitchport(args.mobiushost, args.workflowId, data)


def push_prefix(mb, args, datadir, requests):
    path = datadir + "/prefix*.json"
    for filepath in glob.iglob(path):
        print("Processing file: " + filepath)
        d_f = open(filepath, 'r')
        data = json.load(d_f)
        d_f.close()
        base_val = data["source"]
        for req in requests:
            slices = req["slices"]
            for s in slices:
                nodes = s["nodes"]
                for n in nodes :
                    if n["name"] == "cmnw" :
                        continue
                    if base_val in n["name"]:
                        data["source"] = n["name"]
                        print("payload for prefix request" + str(data))
                        response = mb.add_prefix(args.mobiushost, args.workflowId, data)


def push_scripts(mb, args, datadir, requests):
    path = datadir + "/script*.json"
    for filepath in glob.iglob(path):
        print("Processing file: " + filepath)
        d_f = open(filepath, 'r')
        data = json.load(d_f)
        d_f.close()
        base_val = data["target"]
        for req in requests:
            slices = req["slices"]
            for s in slices:
                nodes = s["nodes"]
                for n in nodes :
                    if n["name"] == "cmnw" :
                        continue
                    if base_val in n["name"]:
                        data["target"] = n["name"]
                        print("payload for script request" + str(data))
                        response = mb.push_script(args.mobiushost, args.workflowId, data)


def provision_storage(args, datadir, site, ipMap, count, ipStart, submitSubnet, sip, exogeniSubnet, exxogeni_slice_name):
    '''
    Provisions storage on specific controller
    @params datadir: data directory from where to pick up storage.json
    @params site: Site on the controller where to provision the node
    @params ipMap: IP MAP contains mapping of Hostname to IP Address
    @params count: Current Node counter
    @params ipStart: Starting IP Address
    @params submitSubnet: Submit Node subnet added to the route
    @params sip: Stitchport IP assigned to Exogeni Master
    @params exogeniSubnet: Exogeni Subnet added to the route if provisioned on Chameleon
    '''
    stdata = None
    st = datadir + "/storage.json"
    # Update the site information
    if os.path.exists(st):
        print("Using " + st + " file for compute storage data")
        st_f = open(st, 'r')
        stdata = json.load(st_f)
        st_f.close()
        stdata["site"]=site

    if stdata is None:
        return True, count, None

    if "slicePolicy" in stdata and stdata["slicePolicy"] == "existing" and exxogeni_slice_name is not None:
        stdata["slicePolicy"] = "existing"
        stdata["sliceName"] = exxogeni_slice_name
        print("Updated slicepolicy")
        print(stdata)

    # Update the postboot script for storage node to use CIDR for IPs on Cloud where node is provisioned
    if stdata["postBootScript"] is not None:
        cidr=get_cidr_escape(ipStart)
        s=stdata["postBootScript"]
        s=s.replace("CIDR",cidr)
        # Specify Stitch Port IP in storage.sh script when storage node is provisioned on exogeni
        if sip is not None:
            s=s.replace("SIP", str(sip))
        stdata["postBootScript"] = s

    mb=MobiusInterface()
    if stdata is not None:
        print("Provisioning compute storage node")
        nodename="Node" + str(count)
        oldnodename = "NODENAME"
        response, nodename = create_compute(mb, args.mobiushost, nodename, ipStart, args.leaseEnd, args.workflowId,
                                            stdata, count, ipMap, oldnodename, site, submitSubnet, None, exogeniSubnet,
                                            None)
        print(nodename + " after create_compute")
        if response.json()["status"] != 200:
            print("Deleting workflow")
            response = mb.delete_workflow(args.mobiushost, args.workflowId)
            return False, count, None
        count = count + 1
    return True, count, nodename

def provision_condor_cluster(args, datadir, site, ipMap, count, ipStart, workers, storagename, subnet, forwardIP,
                             submitSubnet, exxogeni_slice_name):
    mdata = None
    sdata = None
    wdata = None
    m = datadir + "/master.json"
    s = datadir + "/submit.json"
    w = datadir + "/worker.json"
    if os.path.exists(m):
        print("Using " + m + " file for master data")
        m_f = open(m, 'r')
        mdata = json.load(m_f)
        m_f.close()
        mdata["site"]=site
        if "slicePolicy" in mdata and mdata["slicePolicy"] == "existing" and exxogeni_slice_name is not None:
            mdata["slicePolicy"] = "existing"
            mdata["sliceName"] = exxogeni_slice_name
            print("Updated slicepolicy")
            print(mdata)
    if os.path.exists(s):
        print("Using " + s + " file for submit data")
        s_f = open(s, 'r')
        sdata = json.load(s_f)
        s_f.close()
        sdata["site"]=site
        if "slicePolicy" in sdata and sdata["slicePolicy"] == "existing" and exxogeni_slice_name is not None:
            sdata["slicePolicy"] = "existing"
            sdata["sliceName"] = exxogeni_slice_name
            print("Updated slicepolicy")
            print(sdata)
    if os.path.exists(w):
        print("Using " + w + " file for worker data")
        w_f = open(w, 'r')
        wdata = json.load(w_f)
        w_f.close()
        wdata["site"]=site
        if "slicePolicy" in wdata and wdata["slicePolicy"] == "existing" and exxogeni_slice_name is not None:
            wdata["slicePolicy"] = "existing"
            wdata["sliceName"] = exxogeni_slice_name
            print("Updated slicepolicy")
            print(wdata)

    mb=MobiusInterface()
    if mdata is not None:
        print("Provisioning master node")
        nodename="Node" + str(count)
        oldnodename = "NODENAME"
        if ipStart is not None and storagename is not None:
            ipStart = get_next_ip(ipStart)
        response, nodename = create_compute(mb, args.mobiushost, nodename, ipStart, args.leaseEnd, args.workflowId,
                                            mdata, count, ipMap, oldnodename, site, submitSubnet, storagename,
                                            subnet, forwardIP)
        print(nodename + " after create_compute")
        if response.json()["status"] != 200:
            print("Deleting workflow")
            response = mb.delete_workflow(args.mobiushost, args.workflowId)
            return False, count
        count = count + 1
    if sdata is not None:
        print("Provisioning submit node")
        nodename="Node" + str(count)
        oldnodename = "NODENAME"
        if ipStart is not None:
            ipStart = get_next_ip(ipStart)
        response, nodename = create_compute(mb, args.mobiushost, nodename, ipStart, args.leaseEnd, args.workflowId,
                                            sdata, count, ipMap, oldnodename, site, submitSubnet, storagename,
                                            subnet, forwardIP)
        print(nodename + " after create_compute")
        if response.json()["status"] != 200:
            print("Deleting workflow")
            response = mb.delete_workflow(args.mobiushost, args.workflowId)
            return False, count
        count = count + 1
    if wdata is not None:
        oldnodename = "NODENAME"
        for x in range(workers):
            print("Provisioning worker: " + str(x))
            nodename="Node" + str(count)
            if ipStart is not None:
                ipStart = get_next_ip(ipStart)
            response, nodename = create_compute(mb, args.mobiushost, nodename, ipStart, args.leaseEnd, args.workflowId,
                                                wdata, count, ipMap, oldnodename, site, submitSubnet, storagename,
                                                subnet, forwardIP)
            print(nodename + " after create_compute")
            oldnodename = nodename
            if response.json()["status"] != 200:
                print("Deleting workflow")
                response = mb.delete_workflow(args.mobiushost, args.workflowId)
                return False, count
            count = count + 1
    return True, count


def wait_for_network_to_be_active(mb, host, workflowId, site):
    networkStatus = ""
    while networkStatus != "Active":
        shouldSleep = False
        response = mb.get_workflow(host, workflowId)
        if response.json()["status"] == 200:
            status=json.loads(response.json()["value"])
            requests = json.loads(status["workflowStatus"])
            for req in requests:
                if site in req["site"] :
                    slices = req["slices"]
                    for s in slices:
                        if "monitoring" in s["slice"]:
                            continue
                        shouldSleep = True
                        nodes = s["nodes"]
                        for n in nodes :
                            if "cmnw" == n["name"] :
                                print("Updating state of " + n["name"])
                                networkStatus = n["state"]
        if shouldSleep:
            if networkStatus != "Active":
                print("Sleeping for 5 seconds")
                time.sleep(5)
        else:
            break


def create_compute(mb, host, nodename, ipStart, leaseEnd, workflowId,
                   mdata, count, ipMap, oldnodename, site,
                   submitSubnet, storagename, subnet, forwardIP):
    """
    Send Mobius Compute Request
    @params mb: Mobius Interface object
    @params host: Mobius Host
    @params nodename: Node Name for the node being provisioned
    @params ipStart: IP Assigned to Node
    @params leaseEnd: lease end time
    @params workflowId: workflow id
    @params mdata: json containing compute request
    @params count: Count indicating the number of the node being provisioned
    @params ipMap: Map for IP Address to HostName mapping
    @params oldnodename: Previous Node name
    @params site: site at which node is being provisioned
    @params submitSubnet: Submit node Subnet to added to the routes
    @params storagename: Storage Node name to be replaces in workers to mount NFS
    @params subnet:
    @params forwardIP:
    """
    if "Exogeni" in site:
        wait_for_network_to_be_active(mb, host, workflowId, site)
    if 'hostNamePrefix' in mdata and mdata["hostNamePrefix"] is not None:
        nodename = workflowId + mdata["hostNamePrefix"] + str(count)
    defIP = None
    if ipStart is not None:
        mdata["ipAddress"] = ipStart
        ipMap[nodename] = ipStart
        print("Setting " + nodename + " to " + ipStart)
        defIP = get_default_ip_for_condor(ipStart)
    if leaseEnd is not None:
        print("Setting leaseEnd to " + leaseEnd)
        mdata["leaseEnd"] = leaseEnd
    if mdata["postBootScript"] is not None:
        s=mdata["postBootScript"]
        s=s.replace("WORKFLOW", workflowId)
        #s=s.replace(oldnodename, nodename)
        s=s.replace("SUBMIT", str(submitSubnet))
        print("replacing " + oldnodename + " to " + nodename)
        if forwardIP is not None:
            s=s.replace("IPADDR", forwardIP)
            if subnet != None:
                s=s.replace("SUBNET", subnet)
        if ipStart is not None:
            s=s.replace("IPADDR", ipStart)
            if subnet != None:
                s=s.replace("SUBNET", subnet)
        if defIP is not None:
            s=s.replace("REPLACEIP", defIP)
        if storagename is not  None:
            s=s.replace("STORAGENODE", storagename)
        mdata["postBootScript"]=s
        print("==========================================================")
        print("postBootScript: " + str(mdata["postBootScript"]))
        print("==========================================================")
    response = mb.create_compute(host, workflowId, mdata)
    return response, nodename


def set_up_network_data(ch_storage_node_ip, networkdata, ch_storage_node_name, exo_start_ip):
    if networkdata is not None:
        networkdata["destinationIP"] = ch_storage_node_ip
        networkdata["destinationSubnet"] = get_cidr_subnet_sdx(ch_storage_node_ip)
        networkdata["chameleonSdxControllerIP"] = get_sdx_controller_ip(ch_storage_node_ip)
        if ch_storage_node_name is not None:
            temp = ch_storage_node_name.replace('.novalocal','')
            networkdata["destination"] = temp
        networkdata["sourceLocalSubnet"] = get_cidr(exo_start_ip)
        print("KOMAL--- debug {}".format(networkdata))


def cleanup_monitoring(kafkahost, response):
    if response.json()["status"] == 200 :
        status=json.loads(response.json()["value"])
        requests = json.loads(status["workflowStatus"])
        for req in requests:
            slices = req["slices"]
            for s in slices:
                nodes = s["nodes"]
                for n in nodes :
                    if "Chameleon" in s["slice"] or "Jetstream" in s["slice"] or "Mos" in s["slice"]:
                        hostname=n["name"] + ".novalocal"
                    else :
                        hostname=n["name"]
                    if n["name"] == "cmnw" :
                        continue
                    delete_prometheus_target(kafkahost, n["publicIP"])


def delete_prometheus_target(kafkahost, ip):
    try:
        topic_name = 'client-promeithus'
        context = _create_ssl_context(cafile="certs/DigiCertCA.crt", certfile="certs/client.pem",
                                      keyfile="certs/client.key", password="fabric")
        context.check_hostname = False
        context.verify_mode = False
        producer_instance = KafkaProducer(bootstrap_servers=[kafkahost],
                                          security_protocol='SSL',
                                          ssl_context=context)
        ip = ip + ":9100"
        key = 'delete'
        key_bytes = key.encode(encoding='utf-8')
        value_bytes = ip.encode(encoding='utf-8')
        producer_instance.send(topic_name, key=key_bytes, value=value_bytes)
        producer_instance.flush()
    except Exception as e:
        print("Exception occured while deleting topics e=" + str(e))
        print(" ")


def _create_ssl_context(cafile=None, capath=None, cadata=None,
                       certfile=None, keyfile=None, password=None,
                       crlfile=None):
    """
    Simple helper, that creates an SSLContext based on params similar to
    those in ``kafka-python``, but with some restrictions like:

            * ``check_hostname`` is not optional, and will be set to True
            * ``crlfile`` option is missing. It is fairly hard to test it.

    .. _load_verify_locations: https://docs.python.org/3/library/ssl.html\
        #ssl.SSLContext.load_verify_locations
    .. _load_cert_chain: https://docs.python.org/3/library/ssl.html\
        #ssl.SSLContext.load_cert_chain

    Arguments:
        cafile (str): Certificate Authority file path containing certificates
            used to sign broker certificates. If CA not specified (by either
            cafile, capath, cadata) default system CA will be used if found by
            OpenSSL. For more information see `load_verify_locations`_.
            Default: None
        capath (str): Same as `cafile`, but points to a directory containing
            several CA certificates. For more information see
            `load_verify_locations`_. Default: None
        cadata (str/bytes): Same as `cafile`, but instead contains already
            read data in either ASCII or bytes format. Can be used to specify
            DER-encoded certificates, rather than PEM ones. For more
            information see `load_verify_locations`_. Default: None
        certfile (str): optional filename of file in PEM format containing
            the client certificate, as well as any CA certificates needed to
            establish the certificate's authenticity. For more information see
            `load_cert_chain`_. Default: None.
        keyfile (str): optional filename containing the client private key.
            For more information see `load_cert_chain`_. Default: None.
        password (str): optional password to be used when loading the
            certificate chain. For more information see `load_cert_chain`_.
            Default: None.

    """
    if cafile or capath:
        print('Loading SSL CA from %s', cafile or capath)
    elif cadata is not None:
        print('Loading SSL CA from data provided in `cadata`')
        print('`cadata`: %r', cadata)
    # Creating context with default params for client sockets.
    context = create_default_context(
        Purpose.SERVER_AUTH, cafile=cafile, capath=capath, cadata=cadata)
    # Load certificate if one is specified.
    if certfile is not None:
        print('Loading SSL Cert from %s', certfile)
        if keyfile:
            if password is not None:
                print('Loading SSL Key from %s with password', keyfile)
            else:  # pragma: no cover
                print('Loading SSL Key from %s without password', keyfile)
        # NOTE: From docs:
        # If the password argument is not specified and a password is required,
        # OpenSSLs built-in password prompting mechanism will be used to
        # interactively prompt the user for a password.
        context.load_cert_chain(certfile, keyfile, password)
    return context


if __name__ == '__main__':
    main()
