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

import chi
from chi.lease import Lease, get_lease_id, get_node_reservation
from chi.server import get_server

import paramiko
import time


class Node:
    def __init__(self, *, name: str, image: str, site: str, flavor: str, project_name: str,
                 logger: logging.Logger, key_pair: str, network: str, slice_name: str):
        self.name = name
        self.image = image
        self.site = site
        self.flavor = flavor
        self.project_name = project_name
        self.key_pair = key_pair
        self.network = network
        self.slice_name = slice_name
        self.lease = None
        self.mgmt_ip = None
        self.logger = logger
        self.private_key_file = os.environ['OS_SLICE_PRIVATE_KEY_FILE']
        self.public_key_file = os.environ['OS_SLICE_PUBLIC_KEY_FILE']
        self.retry = 5
        self.default_username = "cc"
        self.state = None

    def __is_lease_active(self, *, lease: Lease = None) -> bool:
        if lease is None:
            lease = self.lease
        if lease is not None:
            lease.refresh()
            if lease.status == 'ERROR':
                self.logger.error("Lease is in ERROR state")
                return False
            elif lease.status == 'ACTIVE':
                self.logger.info("Lease is in ACTIVE state")
                return True
        return False

    def __delete_lease(self, *, lease: Lease):
        lease_id = lease.id
        self.logger.info(f"Deleting lease: {lease}")
        lease.delete()
        for i in range(0, self.retry):
            if self.__lease_exists(lease_id=lease_id) is None:
                break
            else:
                time.sleep(30)

    def __lease_exists(self, *, lease_id: str = None) -> Lease or None:
        try:
            if lease_id is None:
                lease_id = get_lease_id(f'{self.slice_name}-{self.name}')
            existing_lease = Lease.from_existing(lease_id)
            self.logger.info(f"Lease exists: {existing_lease.lease}")
            return existing_lease
        except Exception as e:
            self.logger.error(f"Lease does not exist: {e}")
        return None

    def __create_lease(self):
        # Check if the lease exists
        existing_lease = self.__lease_exists()

        # Lease Exists
        if existing_lease is not None:
            # Lease is not ACTIVE; delete it
            if not self.__is_lease_active(lease=existing_lease):
                self.logger.info("Deleting the existing non-Active lease")
                self.__delete_lease(lease=existing_lease)
            # Use existing lease
            else:
                self.lease = existing_lease
                return

        # Lease doesn't exist Create a Lease
        self.logger.info("Creating the lease")
        self.lease = Lease(name=f'{self.slice_name}-{self.name}', node_type=self.flavor)
        try:
            self.lease.wait()
        except Exception as e:
            self.logger.debug(self.lease.lease)
            self.logger.error(f"Failed to wait for the lease to be ACTIVE: {e}")

    def get_reservation_id(self):
        if self.lease is not None:
            return get_node_reservation(self.lease.id)
        return None

    def create(self):
        if self.site == "KVM@TACC":
            self.__create_kvm()
        else:
            self.__create_baremetal()

    def __server_exists(self) -> bool:
        try:
            server_id = chi.server.get_server_id(f'{self.slice_name}-{self.name}')
            if server_id is not None:
                server = chi.server.get_server(server_id)
                self.logger.info(f"Server: {server._info}")
                self.state = server._info['OS-EXT-STS:vm_state']
                addresses = server._info['addresses'][self.network]
                for a in addresses:
                    if a['OS-EXT-IPS:type'] == 'floating':
                        self.mgmt_ip = a['addr']
                return True
        except Exception as e:
            self.logger.error(f"Server {self.slice_name}-{self.name} does not exist")
        return False

    def __create_kvm(self):
        # Select your project
        chi.set('project_name', self.project_name)
        chi.set('project_domain_name', 'default')

        if self.__server_exists():
            self.logger.info(f"Server {self.slice_name}-{self.name} already exists!")
            return

        # Create the VM
        server = chi.server.create_server(server_name=f'{self.slice_name}-{self.name}', image_name=self.image,
                                          network_name=self.network, key_name=self.key_pair, flavor_name=self.flavor)

        # Wait for VM to be Active
        chi.server.wait_for_active(server_id=server.id)

        # Attach the floating IP
        self.mgmt_ip = chi.server.associate_floating_ip(server_id=server.id)

    def __create_baremetal(self):
        # Select your project
        chi.set('project_name', self.project_name)
        chi.set('project_domain_name', 'default')

        # Select your site
        chi.use_site(self.site)

        self.__create_lease()

        if self.__server_exists():
            self.logger.info(f"Server {self.slice_name}-{self.name} already exists!")
            return

        # Created Lease is not Active
        if not self.__is_lease_active():
            self.logger.error("Stopping the provisioning as the lease could not be created")
            return

        self.logger.info(f"Using the lease {self.lease.lease}")

        # Launch the server
        server = chi.server.create_server(server_name=f'{self.slice_name}-{self.name}', image_name=self.image,
                                          network_name=self.network, key_name=self.key_pair,
                                          reservation_id=self.get_reservation_id())

        # Wait for Server to be Active
        chi.server.wait_for_active(server_id=server.id)

        # Attach the floating IP
        self.mgmt_ip = chi.server.associate_floating_ip(server_id=server.id)

    def delete(self):
        server = chi.server.get_server(self.name)
        if server is not None:
            server.delete()
        if self.lease is not None:
            self.lease.delete()

    def __get_paramiko_key(self, private_key_file: str, private_key_passphrase: str = None):

        if private_key_passphrase:
            try:
                return paramiko.RSAKey.from_private_key_file(private_key_file,
                                                             password=private_key_passphrase)
            except:
                pass

            try:
                return paramiko.ecdsakey.ECDSAKey.from_private_key_file(private_key_file,
                                                                        password=private_key_passphrase)
            except:
                pass
        else:
            try:
                return paramiko.RSAKey.from_private_key_file(private_key_file)
            except:
                pass

            try:
                return paramiko.ecdsakey.ECDSAKey.from_private_key_file(private_key_file)
            except:
                pass

        raise Exception(f"ssh key invalid: CHI requires RSA or ECDSA keys")

    def upload_file(self, local_file_path, remote_file_path, retry=3, retry_interval=10):
        """
        Upload a local file to a remote location on the node.
        :param local_file_path: the path to the file to upload
        :type local_file_path: str
        :param remote_file_path: the destination path of the file on the node
        :type remote_file_path: str
        :param retry: how many times to retry SCP upon failure
        :type retry: int
        :param retry_interval: how often to retry SCP on failure
        :type retry_interval: int
        :raise Exception: if management IP is invalid
        """
        self.logger.debug(f"upload node: {self.name}, local_file_path: {local_file_path}")

        for attempt in range(retry):
            try:
                key = self.__get_paramiko_key(private_key_file=self.private_key_file)

                client = paramiko.SSHClient()
                client.load_system_host_keys()
                client.set_missing_host_key_policy(paramiko.MissingHostKeyPolicy())
                client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

                client.connect(self.mgmt_ip, username=self.default_username, pkey=key)

                ftp_client=client.open_sftp()
                file_attributes = ftp_client.put(local_file_path, remote_file_path)
                ftp_client.close()

                return file_attributes
            except Exception as e:
                try:
                    client.close()
                except:
                    logging.debug("Exception in client.close")
                    pass

                if attempt+1 == retry:
                    raise e

                self.logger.info(f"SCP upload fail. Node: {self.name}, trying again")
                self.logger.info(f"Fail: {e}")
                time.sleep(retry_interval)
                pass

        raise Exception("scp upload failed")

    def download_file(self, local_file_path, remote_file_path, retry=3, retry_interval=10):
        """
        Download a remote file from the node to a local destination.
        :param local_file_path: the destination path for the remote file
        :type local_file_path: str
        :param remote_file_path: the path to the remote file to download
        :type remote_file_path: str
        :param retry: how many times to retry SCP upon failure
        :type retry: int
        :param retry_interval: how often to retry SCP upon failure
        :type retry_interval: int
        :param verbose: indicator for verbose outpu
        :type verbose: bool
        """
        logging.debug(f"download node: {self.name}, remote_file_path: {remote_file_path}")

        for attempt in range(retry):
            try:
                key = self.__get_paramiko_key(private_key_file=self.private_key_file)

                client = paramiko.SSHClient()
                client.load_system_host_keys()
                client.set_missing_host_key_policy(paramiko.MissingHostKeyPolicy())
                client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

                client.connect(self.mgmt_ip, username=self.default_username, pkey=key)

                ftp_client = client.open_sftp()
                ftp_client.get(remote_file_path, local_file_path)
                ftp_client.close()

                return
            except Exception as e:
                try:
                    client.close()
                except:
                    logging.debug("Exception in client.close")
                    pass

                if attempt+1 == retry:
                    raise e

                self.logger.info(f"SCP download fail. Node: {self.name}, trying again")
                self.logger.info(f"Fail: {e}")
                time.sleep(retry_interval)
                pass

        raise Exception("scp download failed")

    def upload_directory(self,local_directory_path, remote_directory_path, retry=3, retry_interval=10):
        """
        Upload a directory to remote location on the node.
        Makes a gzipped tarball of a directory and uploades it to a node. Then
        unzips and tars the directory at the remote_directory_path
        :param local_directory_path: the path to the directory to upload
        :type local_directory_path: str
        :param remote_directory_path: the destination path of the directory on the node
        :type remote_directory_path: str
        :param retry: how many times to retry SCP upon failure
        :type retry: int
        :param retry_interval: how often to retry SCP on failure
        :type retry_interval: int
        :raise Exception: if management IP is invalid
        """
        import tarfile
        import os

        logging.debug(f"upload node: {self.name}, local_directory_path: {local_directory_path}")

        output_filename = local_directory_path.split('/')[-1]
        root_size = len(local_directory_path) - len(output_filename)
        temp_file = "/tmp/" + output_filename + ".tar.gz"

        with tarfile.open(temp_file, "w:gz") as tar_handle:
            for root, dirs, files in os.walk(local_directory_path):
                for file in files:
                    tar_handle.add(os.path.join(root, file), arcname = os.path.join(root, file)[root_size:])

        self.upload_file(temp_file, temp_file, retry, retry_interval)
        os.remove(temp_file)
        self.execute("mkdir -p "+remote_directory_path + "; tar -xf " + temp_file + " -C " + remote_directory_path +
                     "; rm " + temp_file, retry, retry_interval)
        return "success"

    def download_directory(self,local_directory_path, remote_directory_path, retry=3, retry_interval=10):
        """
        Downloads a directory from remote location on the node.
        Makes a gzipped tarball of a directory and downloads it from a node. Then
        unzips and tars the directory at the local_directory_path
        :param local_directory_path: the path to the directory to upload
        :type local_directory_path: str
        :param remote_directory_path: the destination path of the directory on the node
        :type remote_directory_path: str
        :param retry: how many times to retry SCP upon failure
        :type retry: int
        :param retry_interval: how often to retry SCP on failure
        :type retry_interval: int
        :raise Exception: if management IP is invalid
        """
        import tarfile
        import os
        logging.debug(f"upload node: {self.name}, local_directory_path: {local_directory_path}")

        temp_file = "/tmp/unpackingfile.tar.gz"
        self.execute("tar -czf " + temp_file + " " + remote_directory_path, retry, retry_interval)

        self.download_file(temp_file, temp_file, retry, retry_interval)
        tar_file = tarfile.open(temp_file)
        tar_file.extractall(local_directory_path)

        self.execute("rm " + temp_file, retry, retry_interval)
        os.remove(temp_file)
        return "success"

    def execute(self, command, retry=3, retry_interval=10):
        """
        Runs a command on the FABRIC node.
        :param command: the command to run
        :type command: str
        :param retry: the number of times to retry SSH upon failure
        :type retry: int
        :param retry_interval: the number of seconds to wait before retrying SSH upon failure
        :type retry_interval: int
        :raise Exception: if management IP is invalid
        """
        import logging

        logging.debug(f"execute node: {self.name}, management_ip: {self.mgmt_ip}, command: {command}")

        for attempt in range(retry):
            try:
                key = self.__get_paramiko_key(private_key_file=self.private_key_file)
                client = paramiko.SSHClient()
                client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

                client.connect(self.mgmt_ip, username=self.default_username, pkey=key)

                stdin, stdout, stderr = client.exec_command('echo \"' + command + '\" > /tmp/chi_execute_script.sh; chmod +x /tmp/chi_execute_script.sh; /tmp/chi_execute_script.sh')
                rtn_stdout = str(stdout.read(), 'utf-8').replace('\\n', '\n')
                rtn_stderr = str(stderr.read(), 'utf-8').replace('\\n', '\n')

                client.close()

                logging.debug(f"rtn_stdout: {rtn_stdout}")
                logging.debug(f"rtn_stderr: {rtn_stderr}")

                return rtn_stdout, rtn_stderr
            except Exception as e:
                try:
                    client.close()
                except:
                    logging.debug("Exception in client.close")
                    pass

                if attempt+1 == retry:
                    raise e

                time.sleep(retry_interval)
                pass

        raise Exception("ssh failed: Should not get here")

    def __str__(self):
        return f"Name: {self.name} Mgmt IP: {self.mgmt_ip} Site: {self.site} Image: {self.image} " \
               f"Slice Name: {self.slice_name} KeyPair: {self.key_pair} Project Name: {self.project_name}"

    def get_name(self) -> str:
        return f'{self.slice_name}-{self.name}'

    def get_site(self) -> str:
        return self.site

    def get_flavor(self) -> str:
        return self.flavor

    def get_image(self) -> str:
        return self.image

    def get_management_ip(self) -> str:
        return self.mgmt_ip

    def get_reservation_state(self) -> str:
        return self.state

