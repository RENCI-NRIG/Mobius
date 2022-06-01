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
import argparse
import logging
from logging.handlers import RotatingFileHandler

import paramiko
import subprocess


class KubeEdgeHelper:
    def __init__(self, core_ip, logger):
        self.core_ip = core_ip
        self.logger = logger

    def get_token_from_master(self):
        key = paramiko.RSAKey.from_private_key_file("/root/.ssh/id_rsa")
        client = paramiko.SSHClient()
        client.load_system_host_keys()
        client.set_missing_host_key_policy(paramiko.MissingHostKeyPolicy())
        client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

        script = "/bin/su - core -c 'sudo /home/core/bin/keadm gettoken --kube-config=/home/core/.kube/config'"
        client.connect(self.core_ip, username='root', pkey=key)

        stdin, stdout, stderr = client.exec_command('echo \"' +
                                                    script + '\" > script.sh; chmod +x script.sh; sudo ./script.sh')
        token = str(stdout.read(),'utf-8')
        token = token.strip('\n')
        self.logger.debug(token)
        client.close()
        return token

    def join_core(self, token):
        if token is not None and token != '':
            cmd = ["/bin/su", "-", "worker", "-c",
                   "sudo /home/worker/bin/keadm join --cloudcore-ipport={} --token={}".format(self.core_ip, token)]
            ret_code, stdout, stderr = self.__run_cmd(args=cmd)
            self.logger.debug("ret_code: {}".format(ret_code))
            self.logger.debug("stdout: {}".format(stdout))
            self.logger.debug("stderr: {}".format(stderr))

    def __run_cmd(self, args):
        cmd = args
        self.logger.debug('running command: ' + ' '.join(cmd))
        p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        stdout, stderr = p.communicate()
        return p.returncode, stdout, stderr


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-c', '--coreip', dest='coreip', type=str, help='Core Node Advertise IP', required=True)
    args = parser.parse_args()
    logger = logging.getLogger(__name__)
    file_handler = RotatingFileHandler('/var/log/kube_edge_helper.log', backupCount=5, maxBytes=50000)
    logging.basicConfig(level=logging.DEBUG,
                        format="%(asctime)s [%(filename)s:%(lineno)d] [%(levelname)s] %(message)s",
                        handlers=[logging.StreamHandler(), file_handler])

    ke_helper = KubeEdgeHelper(core_ip=args.coreip, logger=logger)
    token = ke_helper.get_token_from_master()
    if token is not None:
        ke_helper.join_core(token=token)
    else:
        logger.error("Token not found!")
