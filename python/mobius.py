
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
import json
from random import shuffle

class MobiusException(Exception):
    pass

class MobiusInterface:
    @classmethod
    def __init__(self, log=None):
        self.stdout_path = '/dev/null'
        self._log = log
        if self._log is None:
            self._log = logging.getLogger('')

    @classmethod
    def _headers(self):
        headers = {
            'Accept': 'application/json',
             'Content-Type': "application/json",
        }
        return headers

    @classmethod
    def create_workflow(self, host, workflowId):
        params = {
            'workflowID':workflowId
            }
        response = requests.post((host + '/workflow'), headers=self._headers(), params=params, verify=False)
        print ("Received Response Message: " + response.json()["message"])
        print ("Received Response Status: " + str(response.json()["status"]))
        if response.json()["status"] == 200:
            print ("Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def list_workflows(self, host):
        response = requests.get(host + "/listWorkflows", verify=False)
        print("Received Response Status:{}".format(response.status_code))
        if response.status_code != 200:
            if response.json() is not None:
                print(json.dumps(response.json()))
        else:
            if response.json() is not None:
                print(json.dumps(response.json()["value"]))
        return response

    @classmethod
    def get_workflow(self, host, workflowId):
        params = {
            'workflowID':workflowId
            }
        response = requests.get((host + '/workflow'), headers=self._headers(), params=params, verify=False)
        print ("Received Response Message: " + response.json()["message"])
        print ("Received Response Status: " + str(response.json()["status"]))
        if response.json()["status"] == 200:
            print ("Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def delete_workflow(self, host, workflowId):
        params = {
            'workflowID':workflowId
            }
        response = requests.delete((host + '/workflow'), headers=self._headers(), params=params, verify=False)
        print ("Received Response Message: " + response.json()["message"])
        print ("Received Response Status: " + str(response.json()["status"]))
        if response.json()["status"] == 200:
            print ("Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def create_compute(self, host, workflowId, data):
        params = {
            'workflowID':workflowId
            }
        response = requests.post((host + '/compute'), headers=self._headers(), params=params, json=data, verify=False)
        print ("Received Response Message: " + response.json()["message"])
        print ("Received Response Status: " + str(response.json()["status"]))
        if response.json()["status"] == 200:
            print ("Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def create_storage(self, host, workflowId, data):
        params = {
            'workflowID':workflowId
            }
        response = requests.post((host + '/storage'), headers=self._headers(), params=params, json=data, verify=False)
        print ("Received Response Message: " + response.json()["message"])
        print ("Received Response Status: " + str(response.json()["status"]))
        if response.json()["status"] == 200:
            print ("Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def create_stitchport(self, host, workflowId, data):
        params = {
            'workflowID':workflowId
            }
        response = requests.post((host + '/stitch'), headers=self._headers(), params=params, json=data, verify=False)
        print ("Received Response Message: " + response.json()["message"])
        print ("Received Response Status: " + str(response.json()["status"]))
        if response.json()["status"] == 200:
            print ("Received Response Value: " + str(response.json()["value"]))
        return response
