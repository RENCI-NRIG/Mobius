# comet_common_iface.py
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

import requests
import urllib3
import string
import logging
from random import shuffle


urllib3.disable_warnings()
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

class CometException(Exception):
    pass

class CometInterface:
    @classmethod
    def __init__(self, cometHost, caCert, clientCert, clientKey, log=None):
        self._cometHost = cometHost.split(",")
        self.stdout_path = '/dev/null'
        self._log = log
        if self._log is None:
            self._log = logging.getLogger('')
        if caCert != None:
            self._verify = caCert
            self._cert = (clientCert, clientKey)
        else :
            self._verify = False
            self._cert = None
        self._cert = (clientCert, clientKey)

    @classmethod
    def _headers(self):
        headers = {
            'Accept': 'application/json',
        }
        return headers

    @classmethod
    def invokeRoundRobinApi(self, operation, sliceId, rId, readToken, writeToken, family, value):
        response = None
        shuffle(self._cometHost)
        for host in self._cometHost:
            if operation == 'get_family' :
                response = self.get_family(host, sliceId, rId, readToken, family)
            elif operation == 'update_family' :
                response = self.update_family(host, sliceId, rId, readToken, writeToken, family, value)
            elif operation == 'delete_family' :
                response = self.delete_family(host, sliceId, rId, readToken, writeToken, family)
            elif operation == 'enumerate_families' :
                response = self.enumerate_families(host, sliceId, readToken, family)
            elif operation == 'delete_families' :
                response = self.delete_families(host, sliceId, rId, readToken, writeToken)
            if response.status_code == 200:
                break
        return response

    @classmethod
    def get_family(self, host, sliceId, rId, readToken, family):
        params = {
            'contextID':sliceId,
            'family':family,
            'Key':rId,
            'readToken':readToken
        }
        if self._verify == False:
            response = requests.get((host + '/readScope'), headers=self._headers(), params=params, verify=False)
        else:
            response = requests.get((host + '/readScope'), headers=self._headers(), params=params, cert= self._cert, verify=False)
        self._log.debug ("get_family: Received Response Status Code: " + str(response.status_code))
        if response.status_code == 200 :
            self._log.debug ("get_family: Received Response Message: " + response.json()["message"])
            self._log.debug ("get_family: Received Response Status: " + response.json()["status"])
            self._log.debug ("get_family: Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def update_family(self, host, sliceId, rId, readToken, writeToken, family, value):
        params = {
            'contextID':sliceId,
            'family':family,
            'Key':rId,
            'readToken':readToken,
            'writeToken':writeToken
        }
        response = requests.post((host +'/writeScope'), headers=self._headers(), params=params, cert= self._cert, verify=self._verify, json=value)
        self._log.debug ("update_family: Received Response Status Code: " + str(response.status_code))
        if response.status_code == 200 :
            self._log.debug ("update_family: Received Response Message: " + response.json()["message"])
            self._log.debug ("update_family: Received Response Status: " + response.json()["status"])
            self._log.debug ("update_family: Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def delete_family(self, host, sliceId, rId, readToken, writeToken, family):
        params = {
            'contextID':sliceId,
            'family':family,
            'Key':rId,
            'readToken':readToken,
            'writeToken':writeToken
        }
        if self._verify == False:
            response = requests.delete((host +'/deleteScope'), headers=self._headers(), params=params, verify=False)
        else:
            response = requests.delete((host +'/deleteScope'), headers=self._headers(), params=params, cert= self._cert, verify=False)
        self._log.debug ("delete_family: Received Response Status Code: " + str(response.status_code))
        if response.status_code == 200 :
            self._log.debug ("delete_family: Received Response Message: " + response.json()["message"])
            self._log.debug ("delete_family: Received Response Status: " + response.json()["status"])
            self._log.debug ("delete_family: Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def enumerate_families(self, host, sliceId, readToken, family=None):
        params = {}
        if family is None:
          params = {
               'contextID':sliceId,
               'readToken':readToken,
            }
        else :
          params = {
               'contextID':sliceId,
               'readToken':readToken,
               'family':family,
            }

        if self._verify == False:
            response = requests.get((host +'/enumerateScope'), headers=self._headers(), params=params, verify=False)
        else:
            response = requests.get((host +'/enumerateScope'), headers=self._headers(), params=params, cert= self._cert, verify=False)
        self._log.debug ("enumerate_families: Received Response Status Code: " + str(response.status_code))
        if response.status_code == 200 :
            self._log.debug ("enumerate_families: Received Response Message: " + response.json()["message"])
            self._log.debug ("enumerate_families: Received Response Status: " + response.json()["status"])
            self._log.debug ("enumerate_families: Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def delete_families(self, host, sliceId, rId, readToken, writeToken):
        response = self.enumerate_families(host, sliceId, readToken)
        if response.status_code != 200:
            raise CometException('delete_families: Cannot Enumerate Scope: ' + str(response.status_code))
        if response.json()["value"] and response.json()["value"]["entries"]:
            for key in response.json()["value"]["entries"]:
                self._log.debug ("delete_families: Deleting Family: '" + key["family"] + "' SliceId: '" + sliceId + "' rId: '" + rId + "'")
                response = self.delete_family(host, sliceId, rId, readToken, writeToken, key["family"])
                if response.status_code != 200:
                    self._log.debug('delete_families: Cannot Delete Family: ' + key["family"])
        return response
