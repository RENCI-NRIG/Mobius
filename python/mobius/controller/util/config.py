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
import yaml


class ConfigException(Exception):
    pass


class Config:
    FABRIC = "fabric"
    FABRIC_OC_HOST = "oc-host"
    FABRIC_CM_HOST = "cm-host"
    FABRIC_TOKEN_LOCATION = "token-location"
    FABRIC_BASTION_HOST = "bastion-host"
    FABRIC_BASTION_USER_NAME = "bastion-user-name"
    FABRIC_BASTION_KEY_LOCATION = "bastion-key-location"

    FABRIC_RANDOM = "FABRIC.RANDOM"
    FABRIC_PROJECT_ID = "project_id"

    CHAMELEON = "chameleon"
    CHI_USER = "user"
    CHI_PASSWORD = "password"
    CHI_AUTH_URL = "auth_url"
    CHI_TACC = "tacc"
    CHI_UC = "uc"
    CHI_KVM = "kvm"
    CHI_EDGE = "edge"
    CHI_CLIENT_ID = "client_id"
    CHI_PROJECT_NAME = "project_name"
    CHI_PROJECT_ID = "project_id"
    CHI_RANDOM = "CHI.RANDOM"
    CHI_KEY_PAIR = "key_pair"

    RESOURCES = "resources"
    RESOURCE = "resource"
    RES_TYPE = "type"
    RES_SITE = "site"
    RES_COUNT = "count"
    RES_IMAGE = "image"
    RES_NIC_MODEL = "nic_model"
    RES_NETWORK = "network"
    RES_NAME_PREFIX = "name_prefix"
    RES_TYPE_VM = "VM"
    RES_TYPE_BM = "Baremetal"
    RES_FLAVOR = "flavor"
    RES_FLAVOR_CORES = "cores"
    RES_FLAVOR_RAM = "ram"
    RES_FLAVOR_DISK = "disk"
    RES_FLAVOR_NAME = "name"

    LOGGING = "logging"
    PROPERTY_CONF_LOG_FILE = 'log-file'
    PROPERTY_CONF_LOG_LEVEL = 'log-level'
    PROPERTY_CONF_LOG_RETAIN = 'log-retain'
    PROPERTY_CONF_LOG_SIZE = 'log-size'
    PROPERTY_CONF_LOGGER = "logger"

    RUNTIME = "runtime"
    RUNTIME_SLICE_NAME = "slice_name"
    RUNTIME_SLICE_PRIVATE_KEY_LOCATION = "slice-private-key-location"
    RUNTIME_SLICE_PUBLIC_KEY_LOCATION = "slice-public-key-location"

    def __init__(self, *, path: str):
        self.config_dict = None
        if path is None:
            raise ConfigException("No data source has been specified")

        with open(path) as f:
            self.config_dict = yaml.safe_load(f)

    def get_fabric_config(self) -> dict:
        return self.config_dict.get(Config.FABRIC, None)

    def get_chi_config(self) -> dict:
        return self.config_dict.get(Config.CHAMELEON, None)

    def get_resource_config(self) -> dict:
        return self.config_dict.get(Config.RESOURCES, None)

    def get_log_config(self) -> dict:
        return self.config_dict.get(Config.LOGGING, None)

    def get_runtime_config(self) -> dict:
        return self.config_dict.get(Config.RUNTIME, None)
