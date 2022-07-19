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
import json
import os
import socket
import time
import traceback

from delete_workflow import DeleteWorkflow
from get_workflow import GetWorkflow
from mobius.client.mobius import MobiusInterface


class JSONData:
    def __init__(self):
        self.core = None
        self.worker = None
        self.site = None
        self.worker_count = 0
        self.start_ip = None
        self.drone = None
        self.base_station = None

    def has_core(self) -> bool:
        return self.core is not None

    def has_workers(self) -> bool:
        return self.worker is not None

    def has_drone(self) -> bool:
        return self.drone is not None

    def has_base_station(self) -> bool:
        return self.base_station is not None

    def __str__(self):
        return f"Core: {self.core} worker: {self.worker} site: {self.site} worker_count: {self.worker_count} " \
               f"start_ip: {self.start_ip} drone: {self.drone} base_station: {self.base_station}"


class CreateWorkflow:
    CORE = "core"
    WORKER = "worker"

    EXOGENI = "Exogeni"
    CHAMELEON = "Chameleon"
    MOC = "Moc"
    AWS = "Aws"
    JETSTREAM = "Jetstream"

    TACC = "TACC"
    UC = "UC"

    def __init__(self, args, logger):
        self.logger = logger
        self.args = args
        self.site_data = {}
        self.site_has_core = None

    def __validate_args(self):
        if (self.args.exogenisite is None and self.args.chameleonsite is None and self.args.jetstreamsite is None and
            self.args.mocsite is None) or \
            (self.args.exoworkers is None and self.args.chworkers is None and self.args.jtworkers is None and
             self.args.mocworkers is None) or \
                (self.args.exodatadir is None and self.args.chdatadir is None and self.args.jtdatadir is None and
                 self.args.mocdatadir is None):
            self.logger.error(f"ERROR: site name, number of workers and data directory must be specified "
                              f"for create operation")
            raise Exception()
        if self.args.exoipStart is not None:
            if not self.__is_valid_ipv4_address(self.args.exoipStart):
                self.logger.error("ERROR: Invalid start ip address specified")
                raise Exception()
            if not self.__can_ip_satisfy_range(self.args.exoipStart, self.args.exoworkers + 1):
                self.logger.error("ERROR: Invalid start ip address specified; cannot accomdate the ip for all nodes")
                raise Exception()
        if self.args.chipStart is not None:
            if not self.__is_valid_ipv4_address(self.args.chipStart):
                self.logger.error("ERROR: Invalid start ip address specified")
                raise Exception()
            if not self.__can_ip_satisfy_range(self.args.chipStart, self.args.chworkers + 1):
                self.logger.debug("ERROR: Invalid start ip address specified; cannot accomdate the ip for all nodes")
                raise Exception()
        if self.args.jtipStart is not None:
            if not self.__is_valid_ipv4_address(self.args.jtipStart):
                self.logger.error("ERROR: Invalid start ip address specified")
                raise Exception()
            if not self.__can_ip_satisfy_range(self.args.jtipStart, self.args.jtworkers + 1):
                self.logger.error("ERROR: Invalid start ip address specified; cannot accomdate the ip for all nodes")
                raise Exception()
        if self.args.mocipStart is not None:
            if not self.__is_valid_ipv4_address(self.args.mocipStart):
                self.logger.error("ERROR: Invalid start ip address specified")
                raise Exception()
            if not self.__can_ip_satisfy_range(self.args.mocipStart, self.args.mocworkers + 1):
                self.logger.error("ERROR: Invalid start ip address specified; cannot accomdate the ip for all nodes")
                raise Exception()
        if self.args.comethost is not None:
            if self.args.cert is None or self.args.key is None:
                self.logger.error("ERROR: comet certificate and key must be specified when comethost is indicated")
                raise Exception()
        if self.args.chameleonsite is not None:
            if self.CHAMELEON not in self.args.chameleonsite:
                self.logger.error("ERROR: Invalid site specified")
                raise Exception()
        if self.args.exogenisite is not None:
            if self.EXOGENI not in self.args.exogenisite:
                self.logger.error("ERROR: Invalid site specified")
                raise Exception()
        if self.args.jetstreamsite is not None:
            if self.JETSTREAM not in self.args.jetstreamsite:
                self.logger.error("ERROR: Invalid site specified")
                raise Exception()
        if self.args.mocsite is not None:
            if self.MOC not in self.args.mocsite:
                self.logger.error("ERROR: Invalid site specified")
                raise Exception()

    def load_data_files(self, site: str, path: str, start_ip: str, count: int):
        json_data = JSONData()
        json_data.start_ip = start_ip
        json_data.site = site
        d = path + "/core.json"
        if os.path.exists(d):
            d_f = open(d, 'r')
            json_data.core = json.load(d_f)
            d_f.close()
            self.site_has_core = site
        d = path + "/worker.json"
        if os.path.exists(d):
            d_f = open(d, 'r')
            json_data.worker = json.load(d_f)
            json_data.worker_count = count
            d_f.close()

        d = path + "/drone.json"
        if os.path.exists(d):
            d_f = open(d, 'r')
            json_data.drone = json.load(d_f)
            d_f.close()

        d = path + "/base.json"
        if os.path.exists(d):
            d_f = open(d, 'r')
            json_data.base_station = json.load(d_f)
            d_f.close()

        self.site_data[site] = json_data
        self.logger.debug(f"Added Json data: {json_data} for site: {site}")

    def __wait_for_network_to_be_active(self, site):
        network_status = ""
        mb = MobiusInterface()
        while network_status != "Active":
            should_sleep = False
            response = mb.get_workflow(self.args.mobiushost, self.args.workflowId)
            if response.json()["status"] == 200:
                status = json.loads(response.json()["value"])
                requests = json.loads(status["workflowStatus"])
                for req in requests:
                    if site in req["site"]:
                        slices = req["slices"]
                        for s in slices:
                            should_sleep = True
                            nodes = s["nodes"]
                            for n in nodes:
                                if "cmnw" == n["name"]:
                                    self.logger.debug("Updating state of " + n["name"])
                                    network_status = n["state"]
            if should_sleep:
                if network_status != "Active":
                    self.logger.debug("Sleeping for 5 seconds")
                    time.sleep(5)
            else:
                break

    def __create_compute(self, site: str, data: dict):
        if self.EXOGENI in site:
            self.__wait_for_network_to_be_active(site=site)
        mb = MobiusInterface()
        self.logger.debug(f"Provisioning node with data: {data}")
        response = mb.create_compute(self.args.mobiushost, self.args.workflowId, data)
        if response.status_code != 200:
            raise Exception(f"Failed to create node: {response.status_code}")
        return response

    def __update_json_data(self, data: dict, start_ip: str, workers: int, name: str, site: str, ip_address: str):
        if data["postBootScript"] is not None and data["postBootScript"] != "":
            value = data["postBootScript"].replace("WORKFLOW", self.args.workflowId)
            value = value.replace("STARTIP", start_ip)
            value = value.replace("WORKERS", str(workers))
            data["postBootScript"] = value
        if name is not None:
            data["hostNamePrefix"] = name
        if self.args.leaseEnd is not None:
            self.logger.debug(f"Setting leaseEnd to {self.args.leaseEnd}")
            data["leaseEnd"] = self.args.leaseEnd
        if data.get("site", None) is not None and "CHI@Edge" in data["site"]:
            self.logger.debug("Requested Container, do not override site")
        else:
            if ip_address is not None:
                data["ipAddress"] = ip_address
            data["site"] = site

        return data

    def __provision_k8s_cluster(self, site_data: JSONData, vlan_dict: dict = None, stitch_ip: str = None):
        ip = site_data.start_ip
        if site_data.has_core():
            data = self.__update_json_data(data=site_data.core, start_ip=site_data.start_ip,
                                           workers=site_data.worker_count, name="master", ip_address=ip,
                                           site=site_data.site)
            self.__create_compute(site=site_data.site, data=data)
            ip = self.__get_next_ip(ip=ip)
        if site_data.has_workers():
            for x in range(site_data.worker_count):
                data = self.__update_json_data(data=site_data.worker, start_ip=site_data.start_ip,
                                               workers=site_data.worker_count, name="worker", ip_address=ip,
                                               site=site_data.site)
                self.__create_compute(site=site_data.site, data=data)
                ip = self.__get_next_ip(ip=ip)

        if site_data.has_drone():
            data = self.__update_json_data(data=site_data.drone, start_ip=site_data.start_ip,
                                           workers=site_data.worker_count, name="drone", ip_address=ip,
                                           site=site_data.site)
            if self.EXOGENI in site_data.site and vlan_dict is not None:
                if self.UC in vlan_dict["site"]:
                    data["stitchPortUrl"] = "http://geni-orca.renci.org/owl/ion.rdf#AL2S/Chameleon/Cisco/6509/GigabitEthernet/1/1"
                    data["stitchTag"] = vlan_dict["vlan"]
                    if stitch_ip is not None:
                        data["stitchIP"] = stitch_ip

                elif self.TACC in vlan_dict["site"]:
                    data["stitchPortUrl"] = "http://geni-orca.renci.org/owl/ion.rdf#AL2S/TACC/Cisco/6509/TenGigabitEthernet/1/1"
                    data["stitchTag"] = vlan_dict["vlan"]
                    if stitch_ip is not None:
                        data["stitchIP"] = stitch_ip

            self.__create_compute(site=site_data.site, data=data)
            ip = self.__get_next_ip(ip=ip)

        if site_data.has_base_station():
            data = self.__update_json_data(data=site_data.base_station, start_ip=site_data.start_ip,
                                           workers=site_data.worker_count, name="base", ip_address=ip,
                                           site=site_data.site)
            self.__create_compute(site=site_data.site, data=data)
            ip = self.__get_next_ip(ip=ip)
        return ip

    def __determine_chameleon_vlan(self) -> dict:
        self.logger.debug(f"Determining Chameleon VLAN tag")
        vlan_dict = None
        gw = GetWorkflow(args=self.args, logger=self.logger)
        response = gw.get()
        if response.status_code == 200:
            status = json.loads(response.json()["value"])
            self.logger.debug(f"type= {type(status)} value= {status}")
            slices = json.loads(status["workflowStatus"])
            self.logger.debug(f"type= {type(slices)} slices= {slices}")
            for s in slices:
                self.logger.debug(f"type= {type(s)} s= {s}")
                vlan_tag = s.get("vlan", None)
                site = s.get("site", None)
                if site is not None and vlan_tag is not None and self.CHAMELEON in site:
                    vlan_dict = {"site": str(site), "vlan": str(vlan_tag)}
        self.logger.debug(f"Determined VLAN TAG: {vlan_dict}")
        return vlan_dict

    def create(self):
        try:
            self.__validate_args()
            mb = MobiusInterface()
            response = mb.create_workflow(self.args.mobiushost, self.args.workflowId)
            if response.json()["status"] == 200:
                if self.args.exogenisite is not None and self.args.exodatadir is not None:
                    self.load_data_files(site=self.args.exogenisite, path=self.args.exodatadir,
                                         start_ip=self.args.exoipStart, count=self.args.exoworkers)
                if self.args.chameleonsite is not None and self.args.chdatadir is not None:
                    self.load_data_files(site=self.args.chameleonsite, path=self.args.chdatadir,
                                         start_ip=self.args.chipStart, count=self.args.chworkers)
                if self.args.jetstreamsite is not None and self.args.jtdatadir is not None:
                    self.load_data_files(site=self.args.jetstreamsite, path=self.args.jtdatadir,
                                         start_ip=self.args.jtipStart, count=self.args.jtworkers)
                if self.args.mocsite is not None and self.args.mocdatadir is not None:
                    self.load_data_files(site=self.args.mocsite, path=self.args.mocdatadir, start_ip=self.args.mocipStart,
                                         count=self.args.mocworkers)

                if self.site_has_core is None:
                    raise Exception("None of the Sites has K8s core provisioned")
                # Provision Site which has CORE first
                site_data = self.site_data.get(self.site_has_core, None)
                vlan_dict = None
                stitch_ip = None
                if site_data is not None:
                    self.logger.debug(f"Provisioning Core on site {self.site_has_core}")
                    stitch_ip = self.__provision_k8s_cluster(site_data)
                    if self.CHAMELEON in self.site_has_core:
                        vlan_dict = self.__determine_chameleon_vlan()

                for site in self.site_data.values():
                    if site.site != self.site_has_core:
                        self.logger.debug("Provisioning Workers/Other nodes")
                        self.__provision_k8s_cluster(site_data=site, vlan_dict=vlan_dict, stitch_ip=stitch_ip)
            else:
                raise Exception(f"Failed to create workflow: {response.status_code}")
        except Exception as e:
            self.logger.error(e)
            self.logger.error(traceback.format_exc())
            d = DeleteWorkflow(args=self.args, logger=self.logger)
            d.delete()

    @staticmethod
    def __is_valid_ipv4_address(address):
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

    def __can_ip_satisfy_range(self, ip, n):
        octets = ip.split('.')
        octets[3] = str(int(octets[3]) + n)
        self.logger.debug("Last IP: " + '.'.join(octets))
        return CreateWorkflow.__is_valid_ipv4_address('.'.join(octets))

    def __get_cidr(self, ip):
        octets = ip.split('.')
        octets[3] = '0/24'
        self.logger.debug("CIDR: " + '.'.join(octets))
        return '.'.join(octets)

    def __get_cidr_escape(self, ip):
        octets = ip.split('.')
        octets[3] = '0\/24'
        self.logger.debug("CIDR: " + '.'.join(octets))
        return '.'.join(octets)

    def __get_default_ip_for_condor(self, ip):
        octets = ip.split('.')
        octets[3] = '*'
        self.logger.debug("Default IP: " + '.'.join(octets))
        return '.'.join(octets)

    def __get_next_ip(self, ip):
        octets = ip.split('.')
        octets[3] = str(int(octets[3]) + 1)
        self.logger.debug("Next IP: " + '.'.join(octets))
        return '.'.join(octets)
