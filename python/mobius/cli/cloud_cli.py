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
import os
import sys
import traceback
from logging.handlers import RotatingFileHandler
from typing import List

from mobius.controller.fabric.fabric_client import FabricClient
from mobius.controller.fabric.node import Node
from mobius.controller.util.config import Config


class CloudCli:
    GetOp = 'get'
    ListOp = 'list'
    DeleteOp = 'delete'
    CreateOp = 'create'
    ModifyOp = 'modify'

    def __init__(self, *, path: str = "./client/config/config_template.yml"):
        self.config = None
        self.logger = None
        self.parser = self.__set_up_parser()
        self.args = self.parser.parse_args()
        self.__load_config(path=path)
        self.fab_client = FabricClient(logger=self.logger)

    def __load_config(self, *, path: str):
        self.config = Config(path=path)

        logger_name = self.config.get_log_config().get(Config.PROPERTY_CONF_LOGGER, __name__)
        log_file = self.config.get_log_config().get(Config.PROPERTY_CONF_LOG_FILE, './client.log')
        log_level = self.config.get_log_config().get(Config.PROPERTY_CONF_LOG_LEVEL,  logging.DEBUG)
        log_retain = self.config.get_log_config().get(Config.PROPERTY_CONF_LOG_RETAIN, 5)
        log_size = self.config.get_log_config().get(Config.PROPERTY_CONF_LOG_SIZE, 5000000)

        self.logger = logging.getLogger(logger_name)
        file_handler = RotatingFileHandler(log_file, backupCount=log_retain, maxBytes=log_size)
        logging.basicConfig(level=log_level,
                            format="%(asctime)s [%(filename)s:%(lineno)d] [%(levelname)s] %(message)s",
                            handlers=[file_handler])
                            #handlers=[logging.StreamHandler(), file_handler])

    @staticmethod
    def __set_up_parser() -> argparse.ArgumentParser:
        parser = argparse.ArgumentParser(
            description=f'Python client to provision resources on Fabric.'
                        f'Uses core.json, submit.json and worker.json describing profile for 3 kinds of nodes in the'
                        f'data directory specified')

        parser.add_argument(
            '-fs',
            '--fabricsite',
            dest='fabricsite',
            type=str,
            help='Fabric Site at which resources must be provisioned; must be specified for create operation',
            required=False
        )
        parser.add_argument(
            '-fw',
            '--fabricworkers',
            dest='fabricworkers',
            type=int,
            help='Number of workers to be provisioned on Fabric; must be specified for create operation',
            required=False
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
            '-fip',
            '--fabricipstart',
            dest='fabricipstart',
            type=str,
            help=f'Fabric Start IP Address of the range of IPs to be used for VMs; 1st IP is assigned to master and '
                 f'subsequent IPs are assigned to submit node and workers; can be specified for create operation',
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
            '-fd',
            '--fabricdatadir',
            dest='fabricdatadir',
            type=str,
            help=f'Fabric Data directory where to look for master.json, submit.json and worker.json; '
                 f'must be specified for create operation',
            required=False
        )
        return parser

    def load_data_files(self, site: str, path: str, count: int) -> List[Node]:
        nodes = []
        names = ["core.json", "submit.json", "worker.json"]
        for n in names:
            d = f"{path}/{n}"
            if os.path.exists(d):
                node = Node()
                d_f = open(d, 'r')
                json_data = d_f.read()
                node = node.from_json(json_string=json_data)
                d_f.close()
                if n == "worker.json":
                    node.count = count
                else:
                    node.count = 1
                node.site = site
                self.logger.info(f"Added Node: {node}")
                nodes.append(node)
        return nodes

    def __validate_args_create(self):
        if self.args.fabricsite is None or self.args.fabricdatadir is None or self.args.fabricworkers is None or \
                self.args.workflowId is None or self.args.fabricipstart is None:
            self.logger.error(f"ERROR: site name, number of workers and data directory must be specified "
                              f"for create operation Site: {self.args.fabricsite} Data Dir: {self.args.fabricdatadir} "
                              f" Worker: {self.args.fabricworkers} WorkflowId: {self.args.workflowId} "
                              f"Start IP: {self.args.fabricipstart}")
            self.logger.error(self.args)
            raise Exception()

    def get_workflow(self):
        self.fab_client.get_slice(slice_name=self.args.workflowId)

    def list_workflow(self):
        self.fab_client.list_slices()

    def create_workflow(self):
        self.__validate_args_create()
        nodes = self.load_data_files(site=self.args.fabricsite, path=self.args.fabricdatadir,
                                     count=self.args.fabricworkers)
        self.fab_client.create_slice(workflow_name=self.args.workflowId, nodes=nodes, start_ip=self.args.fabricipstart)

    def delete_workflow(self):
        self.fab_client.delete_slice(slice_name=self.args.workflowId)

    def test(self):
        slice = self.fab_client.get_slice(slice_name=self.args.workflowId)
        for n in slice.get_nodes():
            print(f"Management IP for node: {n.get_name()} is {n.get_management_os_interface()}")

    def main(self):
        try:
            if self.args.operation == self.GetOp:
                self.get_workflow()
            elif self.args.operation == self.ListOp:
                self.list_workflow()
            elif self.args.operation == self.CreateOp:
                self.create_workflow()
            elif self.args.operation == self.DeleteOp:
                self.delete_workflow()
            else:
                raise Exception()
        except Exception as e:
            self.logger.error(e)
            self.logger.error(traceback.format_exc())
            self.parser.print_help()
            sys.exit(1)

        sys.exit(0)


if __name__ == '__main__':
    client = CloudCli()
    client.main()