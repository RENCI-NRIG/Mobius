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


class Config:
    FABRIC = "fabric"
    FABRIC_OC_HOST = "oc-host"
    FABRIC_CM_HOST = "cm-host"
    FABRIC_TOKEN_LOCATION = "token-location"
    FABRIC_BASTION_HOST = "bastion-host"
    FABRIC_BASTION_USER_NAME = "bastion-user-name"
    FABRIC_BASTION_KEY_LOCATION = "bastion-key-location"
    FABRIC_SLICE_PRIVATE_KEY_LOCATION = "slice-private-key-location"
    FABRIC_SLICE_PUBLIC_KEY_LOCATION = "slice-public-key-location"

    LOGGING = "logging"
    PROPERTY_CONF_LOG_FILE = 'log-file'
    PROPERTY_CONF_LOG_LEVEL = 'log-level'
    PROPERTY_CONF_LOG_RETAIN = 'log-retain'
    PROPERTY_CONF_LOG_SIZE = 'log-size'
    PROPERTY_CONF_LOGGER = "logger"

    RUNTIME = "runtime"

    def __init__(self, *, path: str):
        self.config_dict = None
        if path is None:
            raise Exception("No data source has been specified")

        print(f"Reading config file: {path}")

        with open(path) as f:
            self.config_dict = yaml.safe_load(f)

    def get_fabric_config(self) -> dict:
        return self.config_dict.get(Config.FABRIC, None)

    def get_log_config(self) -> dict:
        return self.config_dict.get(Config.LOGGING, None)

    def get_runtime_config(self) -> dict:
        return self.config_dict.get(Config.RUNTIME, None)
