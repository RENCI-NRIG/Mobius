# !/usr/bin/env python3
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
from keystoneauth1 import identity
from keystoneauth1 import session
from novaclient import client
VERSION=2
auth = identity.v3.oidc.OidcPassword('https://kvm.tacc.chameleoncloud.org:5000/v3',
    identity_provider='chameleon',
    protocol='openid',
    client_id='keystone-kvm-prod',
    client_secret='none',
    access_token_endpoint='https://auth.chameleoncloud.org/auth/realms/chameleon/protocol/openid-connect/token',
    discovery_endpoint='https://auth.chameleoncloud.org/auth/realms/chameleon/.well-known/openid-configuration',
    username='kthare10',
    password='',
    #project_name='CHI-210827',
    project_id='40753b15f6c34f9c8d4695c97b026796',
    project_domain_name='Default'
)
s = session.Session(auth)
nova = client.Client(VERSION, session=s)
print(nova.servers.list())
