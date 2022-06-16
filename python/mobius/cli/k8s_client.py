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
import argparse
import logging
import sys
import traceback
from logging.handlers import RotatingFileHandler

from mobius.k8s_client.get_workflow import GetWorkflow
from mobius.k8s_client.list_workflow import ListWorkflow
from mobius.k8s_client.create_workflow import CreateWorkflow
from mobius.k8s_client.delete_workflow import DeleteWorkflow


class K8sClient:
    GetOp = 'get'
    ListOp = 'list'
    DeleteOp = 'delete'
    CreateOp = 'create'
    ModifyOp = 'modify'

    def __init__(self):
        self.parser = self.__set_up_parser()
        self.args = self.parser.parse_args()
        self.logger = logging.getLogger(__name__)
        file_handler = RotatingFileHandler('./k8s_client.log', backupCount=5, maxBytes=50000)
        logging.basicConfig(level=logging.DEBUG,
                            format="%(asctime)s [%(filename)s:%(lineno)d] [%(levelname)s] %(message)s",
                            handlers=[logging.StreamHandler(), file_handler])

    def __set_up_parser(self):
        parser = argparse.ArgumentParser(
            description=f'Python client to create Condor cluster using client.\nUses master.json, submit.json and '
                        f'worker.json for compute requests present in data directory specified.\nCurrently only '
                        f'supports provisioning compute resources. Other resources can be provisioned via '
                        f'mobius_client.\nCreates COMET contexts for Chameleon resources and thus enables '
                        f'exchanging keys and hostnames within workflow')

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
            '--mocsite',
            dest='mocsite',
            type=str,
            help='Mass Open Cloud Site at which resources must be provisioned; must be specified for create operation',
            required=False
        )
        parser.add_argument(
            '-n1',
            '--exoworkers',
            dest='exoworkers',
            type=int,
            help='Number of workers to be provisioned on Exogeni; must be specified for create operation',
            required=False
        )
        parser.add_argument(
            '-n2',
            '--chworkers',
            dest='chworkers',
            type=int,
            help='Number of workers to be provisioned on Chameleon; must be specified for create operation',
            required=False
        )
        parser.add_argument(
            '-n3',
            '--jtworkers',
            dest='jtworkers',
            type=int,
            help='Number of workers to be provisioned on Jetstream; must be specified for create operation',
            required=False
        )
        parser.add_argument(
            '-n4',
            '--mocworkers',
            dest='mocworkers',
            type=int,
            help='Number of workers to be provisioned on Mass Open Cloud; must be specified for create operation',
            required=False
        )
        parser.add_argument(
            '-c',
            '--comethost',
            dest='comethost',
            type=str,
            help=f'Comet Host default(https://comet-hn1.exogeni.net:8111/) used only for provisioning '
                 f'resources on chameleon',
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
            help=f'Chameleon Start IP Address of the range of IPs to be used for VMs; 1st IP is assigned to master '
                 f'and subsequent IPs are assigned to submit node and workers; can be specified for create operation',
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
            '--mocipStart',
            dest='mocipStart',
            type=str,
            help=f'Mass Open Cloud Start IP Address of the range of IPs to be used for VMs; 1st IP is assigned to '
                 f'master and subsequent IPs are assigned to submit node and workers; can be specified for create '
                 f'operation',
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
            '--mocdatadir',
            dest='mocdatadir',
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

    def main(self):
        try:
            if self.args.operation == self.GetOp:
                g = GetWorkflow(args=self.args, logger=self.logger)
                g.get()
            elif self.args.operation == self.ListOp:
                l = ListWorkflow(args=self.args, logger=self.logger)
                l.list()
            elif self.args.operation == self.CreateOp:
                c = CreateWorkflow(args=self.args, logger=self.logger)
                c.create()
            elif self.args.operation == self.DeleteOp:
                d = DeleteWorkflow(args=self.args, logger=self.logger)
                d.delete()
            else:
                raise Exception()
        except Exception as e:
            self.logger.error(e)
            self.logger.error(traceback.format_exc())
            #self.parser.print_help()
            sys.exit(1)

        sys.exit(0)


if __name__ == '__main__':
    client = K8sClient()
    client.main()
