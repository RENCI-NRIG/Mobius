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
import logging
import json
import pprint


class MobiusException(Exception):
    pass


class MobiusInterface:
    def __init__(self, log=None):
        self.stdout_path = '/dev/null'
        self._log = log
        if self._log is None:
            self._log = logging.getLogger('')

    def _headers(self):
        headers = {
            'Accept': 'application/json',
             'Content-Type': "application/json",
        }
        return headers

    def __print_value(self, value: str):
        if value is None:
            return
        print(f"Received Response Value:")
        pp = pprint.PrettyPrinter(indent=4)
        value = json.loads(value)
        pp.pprint(value)

    def create_workflow(self, host: str, workflow_id: str):
        params = {
            'workflowID': workflow_id
            }
        response = requests.post((host + '/workflow'), headers=self._headers(), params=params, verify=False)
        print("Received Response Message: " + response.json()["message"])
        print("Received Response Status: " + str(response.json()["status"]))
        if response.json()["status"] == 200:
            print("Received Response Value: " + str(response.json()["value"]))
            self.__print_value(value=response.json()["value"])
        return response

    def list_workflows(self, host: str):
        response = requests.get(host + "/listWorkflows", verify=False)
        print("Received Response Status:{}".format(response.status_code))
        if response.status_code != 200:
            if response.json() is not None:
                print(json.dumps(response.json()))
        else:
            if response.json() is not None:
                print("Received Response Value: ")
                pp = pprint.PrettyPrinter(indent=4)
                value = json.loads(response.content)["value"]
                pp.pprint(value)
        return response
    
    def get_workflow(self, host: str, workflow_id: str):
        params = {
            'workflowID': workflow_id
            }
        response = requests.get((host + '/workflow'), headers=self._headers(), params=params, verify=False)
        print("Received Response Message: " + response.json()["message"])
        print("Received Response Status: " + str(response.json()["status"]))
        if response.json()["status"] == 200:
            self.__print_value(value=response.json()["value"])
        return response

    def delete_workflow(self, host: str, workflow_id: str):
        params = {
            'workflowID': workflow_id
            }
        response = requests.delete((host + '/workflow'), headers=self._headers(), params=params, verify=False)
        print("Received Response Message: " + response.json()["message"])
        print("Received Response Status: " + str(response.json()["status"]))
        if response.json()["status"] == 200:
            self.__print_value(value=response.json()["value"])
        return response

    def create_compute(self, host: str, workflow_id: str, data: dict):
        params = {
            'workflowID': workflow_id
            }
        response = requests.post((host + '/compute'), headers=self._headers(), params=params, json=data, verify=False)
        print("Received Response Message: " + response.json()["message"])
        print("Received Response Status: " + str(response.json()["status"]))
        if response.json()["status"] == 200:
            self.__print_value(value=response.json()["value"])
        return response

    def create_storage(self, host: str, workflow_id: str, data: dict):
        params = {
            'workflowID': workflow_id
            }
        response = requests.post((host + '/storage'), headers=self._headers(), params=params, json=data, verify=False)
        print("Received Response Message: " + response.json()["message"])
        print("Received Response Status: " + str(response.json()["status"]))
        if response.json()["status"] == 200:
            self.__print_value(value=response.json()["value"])
        return response

    def create_stitchport(self, host: str, workflow_id: str, data: dict):
        params = {
            'workflowID': workflow_id
            }
        response = requests.post((host + '/stitch'), headers=self._headers(), params=params, json=data, verify=False)
        print("Received Response Message: " + response.json()["message"])
        print("Received Response Status: " + str(response.json()["status"]))
        if response.json()["status"] == 200:
            self.__print_value(value=response.json()["value"])
        return response
    
    def create_network(self, host: str, workflow_id: str, data: dict):
        params = {
            'workflowID': workflow_id
            }
        response = requests.post((host + '/network'), headers=self._headers(), params=params, json=data, verify=False)
        print("Received Response Message: " + response.json()["message"])
        print("Received Response Status: " + str(response.json()["status"]))
        if response.json()["status"] == 200:
            self.__print_value(value=response.json()["value"])
        return response
    
    def add_prefix(self, host: str, workflow_id: str, data: dict):
        params = {
            'workflowID': workflow_id
            }
        response = requests.post((host + '/sdxPrefix'), headers=self._headers(), params=params, json=data, verify=False)
        print("Received Response Message: " + response.json()["message"])
        print("Received Response Status: " + str(response.json()["status"]))
        if response.json()["status"] == 200:
            self.__print_value(value=response.json()["value"])
        return response

    def push_script(self, host: str, workflow_id: str, data: dict):
        params = {
            'workflowID': workflow_id
            }
        response = requests.post((host + '/script'), headers=self._headers(), params=params, json=data, verify=False)
        print("Received Response Message: " + response.json()["message"])
        print("Received Response Status: " + str(response.json()["status"]))
        if response.json()["status"] == 200:
            self.__print_value(value=response.json()["value"])
        return response
