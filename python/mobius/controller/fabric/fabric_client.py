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
# Author Komal Thareja (kthare10@renci.org)
import logging
import os
import traceback

from fabrictestbed_extensions.fablib.fablib import fablib
from fabrictestbed_extensions.fablib.resources import Resources
from fabrictestbed_extensions.fablib.slice import Slice

from mobius.controller.api.api_client import ApiClient
from mobius.controller.util.config import Config


class FabricClient(ApiClient):
    def __init__(self, *, slice_name: str, logger: logging.Logger, fabric_config: dict, runtime_config: dict):
        """ Constructor """
        self.slice_name = slice_name
        self.logger = logger
        self.slice_id = None
        self.slice_object = None
        self.fabric_config = fabric_config
        self.runtime_config = runtime_config
        self.node_counter = 0

    def get_resources(self) -> Slice:
        try:
            print("get slice_id")
            if self.slice_id is not None:
                print("slice_id: " + str(self.slice_id))
                return fablib.get_slice(slice_id=self.slice_id)
            else:
                print("slice id is none. slice name: " + self.slice_name)
                return fablib.get_slice(name=self.slice_name)
        except Exception as e:
            print(f"Exception: {e}")

    def get_available_resources(self) -> Resources:
        try:
            available_resources = fablib.get_available_resources()
            print(f"Available Resources: {available_resources}")
            return available_resources
        except Exception as e:
            print(f"Error: {e}")

    def add_resources(self, *, resource: dict):
        if resource.get(Config.RES_COUNT) < 1:
            return
        # Create Slice
        self.logger.debug(f"Adding {resource} to {self.slice_name}")
        if self.slice_object is None:
            self.slice_object = fablib.new_slice(self.slice_name)

        interface_list = []
        node_count = resource.get(Config.RES_COUNT)
        node_name_prefix = resource.get(Config.RES_NAME_PREFIX)
        image = resource.get(Config.RES_IMAGE)
        nic_model = resource.get(Config.RES_NIC_MODEL)
        network_type = resource.get(Config.RES_NETWORK)[Config.RES_TYPE]
        site = resource.get(Config.RES_SITE)
        cores = resource.get(Config.RES_FLAVOR)[Config.RES_FLAVOR_CORES]
        ram = resource.get(Config.RES_FLAVOR)[Config.RES_FLAVOR_RAM]
        disk = resource.get(Config.RES_FLAVOR)[Config.RES_FLAVOR_DISK]
        if site == Config.FABRIC_RANDOM:
            site = fablib.get_random_site()

        # Add node
        for i in range(node_count):
            node_name = f"{node_name_prefix}{self.node_counter}"
            self.node_counter += 1
            node = self.slice_object.add_node(name=node_name, image=image, site=site, cores=cores, ram=ram, disk=disk)

            iface = node.add_component(model=nic_model, name=f"{node_name}-nic1").get_interfaces()[0]
            interface_list.append(iface)

        # Layer3 Network (provides data plane internet access)
        net1 = self.slice_object.add_l3network(name=f"{site}-network", interfaces=interface_list,
                                               type=network_type)

    def create_resources(self) -> bool:
        try:
            if self.slice_object is None:
                raise Exception("Add Resources to the Slice, before calling create")
            # Check if the slice has more than one site then add a layer2 network
            # Submit Slice Request
            self.logger.debug("Submit slice request")
            self.slice_id = self.slice_object.submit(wait=False)
            self.logger.debug("Waiting for the slice to Stable")
            self.slice_object.wait(progress=True)
            self.slice_object.update()
            self.slice_object.post_boot_config()
            self.logger.debug("Slice provisioning successful")
            return True
        except Exception as e:
            self.logger.error(f"Exception occurred: {e}")
            self.logger.error(traceback.format_exc())
        return False

    def delete_resources(self):
        try:
            if self.slice_id is not None:
                slice_object = fablib.get_slice(slice_id=self.slice_id)
            else:
                slice_object = fablib.get_slice(self.slice_name)
            slice_object.delete()
        except Exception as e:
            print(f"Fail: {e}")
