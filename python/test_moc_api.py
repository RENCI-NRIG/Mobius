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
