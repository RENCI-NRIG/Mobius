
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

import requests
import urllib3
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
            elif operation == 'reset_families' :
                response = self.reset_families(host, sliceId, rId, readToken, writeToken)
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
                self._log.debug ("delete_families: Deleting Family: '" + key["family"] + "' SliceId: '" + sliceId + "' rId: '" + str(rId) + "'")
                response = self.delete_family(host, sliceId, key["key"], readToken, writeToken, key["family"])
                if response.status_code != 200:
                    self._log.debug ('delete_families: Cannot Delete Family: ' + key["family"])
        return response

    @classmethod
    def reset_families(self, host, sliceId, rId, readToken, writeToken):
        response = self.enumerate_families(host, sliceId, readToken)
        if response.status_code != 200:
            raise CometException('reset_families: Cannot Enumerate Scope: ' + str(response.status_code))
        if response.json()["value"] and response.json()["value"]["entries"]:
            for key in response.json()["value"]["entries"]:
                self._log.debug ("reset_families: Deleting Family: '" + key["family"] + "' SliceId: '" + sliceId + "' rId: '" + str(rId) + "'")
                response = self.update_family(host, sliceId, key["key"], readToken, writeToken, key["family"], "")
                if response.status_code != 200:
                    self._log.debug ('reset_families: Cannot Delete Family: ' + key["family"])
        return response
