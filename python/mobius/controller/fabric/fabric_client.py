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
import logging
import time
import traceback
from enum import Enum
from ipaddress import IPv4Address
from typing import List

from fabrictestbed_extensions.fablib.fablib import fablib
from fabrictestbed_extensions.fablib.slice import Slice

from mobius.cloud.fabric.node import Node


class NicModel(Enum):
    NIC_Basic = 1,
    NIC_ConnectX_5 = 2,
    NIC_ConnectX_6 = 3


class FabricClient:
    def __init__(self, *, logger: logging.Logger):
        self.logger = logger
        self.fab_lib = fablib()

    def __set_ip(self, slice_object: Slice, name: str, network_name:str, ip: str):
        fabric_node = slice_object.get_node(name=name)
        fabric_node_iface = fabric_node.get_interface(network_name=network_name)
        fabric_node_iface.set_ip(ip=ip, cidr="24")
        stdout, stderr = fabric_node.execute(f'ip addr list {fabric_node_iface.get_os_interface()}')
        self.logger.info(f"IP Address operation completed for node: {name} for network: {network_name}: "
                         f"{stdout} {stderr}")
        time.sleep(10)
        stdout, stderr = fabric_node.execute(f'ip addr list {fabric_node_iface.get_os_interface()}')
        self.logger.info(f"IP Address operation completed for node: {name} for network: {network_name}: "
                         f"{stdout} {stderr}")

    def __post_boot_script(self, slice_object: Slice, name: str, script: str, ip: str):
        if script is None or script == "":
            return
        fabric_node = slice_object.get_node(name=name)
        script = script.replace("REPLACEIP", ip)
        stdout, stderr = fabric_node.execute(script)
        self.logger.info(f"Script execution complete for node: {name}: {stdout} {stderr} ")

    def create_slice(self, *, workflow_name: str, nodes: List[Node], start_ip: str,
                     network_type: NicModel = NicModel.NIC_Basic):
        try:
            slice_object = self.fab_lib.new_slice(name=workflow_name)

            count = 0
            interfaces = []
            for n in nodes:
                for i in range(n.count):
                    print(f"Adding Node: {n}")
                    name = f"{n.name_prefix}-{count}"
                    fabric_node = slice_object.add_node(name=name, site=n.site)
                    fabric_node.set_capacities(cores=n.core, ram=n.ram, disk=n.disk)
                    fabric_node.set_image(image=n.image)
                    nic_name = f"{name}-nic"
                    interface = fabric_node.add_component(model=network_type.name, name=nic_name).get_interfaces()[0]
                    interfaces.append(interface)
                    count += 1

            network_name = f"{workflow_name}-net"
            slice_object.add_l2network(name=network_name, interfaces=interfaces)

            self.logger.info("Slice submitted for creation!")
            slice_object.submit(wait_progress=True)

            self.logger.info("Slice created successfully, setting up the IP Addresses and "
                             "executing any post boot scripts!")

            ip_address = IPv4Address(start_ip)
            count = 0
            for n in nodes:
                for i in range(n.count):
                    self.__set_ip(slice_object=slice_object, name=f"{n.name_prefix}-{count}", ip=str(ip_address),
                                  network_name=network_name)

                    #self.__post_boot_script(script=n.script, slice_object=slice_object,
                    #                        name=f"{n.name_prefix}-{count}", ip=str(ip_address))

                    ip_address += 1
                    count += 1

            return slice_object
        except Exception as e:
            print(f"Slice Fail: {e}")
            traceback.print_exc()

    def delete_slice(self, slice_name: str):
        try:
            slice = self.fab_lib.get_slice(name=slice_name)
            slice.delete()
        except Exception as e:
            print(f"Fail: {e}")
            traceback.print_exc()

    def get_slice(self, slice_name: str) -> Slice:
        try:
            slice = self.fab_lib.get_slice(name=slice_name)
            for node in slice.get_nodes():
                print("Node:")
                print(f"   Name              : {node.get_name()}")
                print(f"   Cores             : {node.get_cores()}")
                print(f"   RAM               : {node.get_ram()}")
                print(f"   Disk              : {node.get_disk()}")
                print(f"   Image             : {node.get_image()}")
                print(f"   Image Type        : {node.get_image_type()}")
                print(f"   Host              : {node.get_host()}")
                print(f"   Site              : {node.get_site()}")
                print(f"   Management IP     : {node.get_management_ip()}")
                print(f"   Reservation ID    : {node.get_reservation_id()}")
                print(f"   Reservation State : {node.get_reservation_state()}")
                print(f"   SSH Command       : {node.get_ssh_command()}")
                print(f"   Components        :  ")
                for component in node.get_components():
                    print(f"      Name             : {component.get_name()}")
                    print(f"      Details          : {component.get_details()}")
                    print(f"      Disk (G)         : {component.get_disk()}")
                    print(f"      Units            : {component.get_unit()}")
                    print(f"      PCI Address      : {component.get_pci_addr()}")
                    print(f"      Model            : {component.get_model()}")
                    print(f"      Type             : {component.get_type()}")
                print(f"   Interfaces        :  ")
                for interface in node.get_interfaces():
                    print(f"       Name                : {interface.get_name()}")
                    print(f"           Bandwidth           : {interface.get_bandwidth()}")
                    print(f"           VLAN                : {interface.get_vlan()}")
                    print(f"           OS Interface        : {interface.get_os_interface()}")
            return slice
        except Exception as e:
            print(f"Fail: {e}")
            traceback.print_exc()

    def list_slices(self):
        slices = self.fab_lib.get_slices()
        for s in slices:
            print(s)
