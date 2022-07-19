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
auth = identity.v3.oidc.OidcPassword(
    'https://kaizen.massopen.cloud:13000/v3',
    identity_provider='moc',
    protocol='openid',
    client_id='',
    client_secret='',
    access_token_endpoint='https://sso.massopen.cloud/auth/realms/moc/protocol/openid-connect/token',
    discovery_endpoint='https://sso.massopen.cloud/auth/realms/moc/.well-known/openid-configuration',
    username='',
    password='',
    project_name='',
    project_domain_name='Default'
)
s = session.Session(auth)
nova = client.Client(VERSION, session=s)
print(nova.servers.list())
