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

from mobius.controller.api.api_client import ApiClient
from mobius.controller.util.config import Config


class ChiClient(ApiClient):
    def __init__(self, *, logger: logging.Logger, chi_config: dict, runtime_config: dict):
        self.logger = logger
        self.chi_config = chi_config
        self.runtime_config = runtime_config
        self.slices = {}

    def setup_environment(self, *, site: str):
        """
        Setup the environment variables for Chameleon
        Should be invoked before any of the chi packages are imported otherwise, none of the CHI APIs work and
        fail with BAD REQUEST error
        @param site: site name
        """
        site_id = self.__get_site_identifier(site=site)
        os.environ['OS_AUTH_URL'] = self.chi_config.get(Config.CHI_AUTH_URL)[site_id]
        os.environ['OS_IDENTITY_API_VERSION'] = "3"
        os.environ['OS_INTERFACE'] = "public"
        os.environ['OS_PROJECT_ID'] = self.chi_config.get(Config.CHI_PROJECT_ID)[site_id]
        os.environ['OS_USERNAME'] = self.chi_config.get(Config.CHI_USER)
        os.environ['OS_PROTOCOL'] = "openid"
        os.environ['OS_AUTH_TYPE'] = "v3oidcpassword"
        os.environ['OS_PASSWORD'] = self.chi_config.get(Config.CHI_PASSWORD)
        os.environ['OS_IDENTITY_PROVIDER'] = "chameleon"
        os.environ['OS_DISCOVERY_ENDPOINT'] = "https://auth.chameleoncloud.org/auth/realms/chameleon/.well-known/openid-configuration"
        os.environ['OS_CLIENT_ID'] = self.chi_config.get(Config.CHI_CLIENT_ID)[site_id]
        os.environ['OS_ACCESS_TOKEN_TYPE'] = "access_token"
        os.environ['OS_CLIENT_SECRET'] = "none"
        os.environ['OS_REGION_NAME'] = site
        os.environ['OS_SLICE_PRIVATE_KEY_FILE'] = self.runtime_config.get(Config.RUNTIME_SLICE_PRIVATE_KEY_LOCATION)
        os.environ['OS_SLICE_PUBLIC_KEY_FILE'] = self.runtime_config.get(Config.RUNTIME_SLICE_PUBLIC_KEY_LOCATION)

    @staticmethod
    def __get_site_identifier(*, site: str):
        if site == "CHI@UC":
            return "uc"
        elif site == "CHI@TACC":
            return "tacc"
        elif site == "CHI@NU":
            return "nu"
        elif site == "KVM@TACC":
            return "kvm"
        elif site == "CHI@Edge":
            return "edge"

    def get_resources(self, slice_id: str = None, slice_name: str = None):
        if slice_name is not None:
            if slice_name in self.slices:
                return [self.slices.get(slice_name)]
        else:
            return self.slices.values()

    def get_available_resources(self):
        pass

    def add_resources(self, *, resource: dict, slice_name: str):
        if resource.get(Config.RES_COUNT) < 1:
            return None

        if slice_name in self.slices:
            self.logger.info(f"Slice {slice_name} already exists!")
            return None

        self.logger.debug(f"Adding {resource} to {slice_name}")

        site = resource.get(Config.RES_SITE)
        self.setup_environment(site=site)

        # Should be done only after setting up the environment
        from mobius.controller.chi.slice import Slice
        slice_object = Slice(name=slice_name, logger=self.logger, key_pair=self.chi_config.get(Config.CHI_KEY_PAIR),
                             project_name=self.chi_config.get(Config.CHI_PROJECT_NAME))

        slice_object.add_resource(resource=resource)
        self.slices[slice_name] = slice_object
        return slice_object

    def delete_resources(self, *, slice_id: str = None, slice_name: str = None):
        try:
            if slice_name is not None:
                if slice_name in self.slices:
                    self.logger.info(f"Deleting slice {slice_name}")
                    slice_object = self.slices.get(slice_name)
                    slice_object.delete()
            else:
                for s in self.slices.values():
                    s.delete()
        except Exception as e:
            self.logger.info(f"Fail: {e}")
