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
import json
import traceback
from ssl import create_default_context, Purpose

from kafka import KafkaProducer

from mobius.client.mobius import MobiusInterface


class DeleteWorkflow:
    def __init__(self, args, logger):
        self.logger = logger
        self.args = args

    def __cleanup_monitoring(self, response):
        if response.json()["status"] == 200:
            status = json.loads(response.json()["value"])
            requests = json.loads(status["workflowStatus"])
            for req in requests:
                slices = req["slices"]
                for s in slices:
                    nodes = s["nodes"]
                    for n in nodes:
                        if "Chameleon" in s["slice"] or "Jetstream" in s["slice"] or "Mos" in s["slice"]:
                            hostname = n["name"] + ".novalocal"
                        else:
                            hostname = n["name"]
                        if n["name"] == "cmnw":
                            continue
                        self.__delete_prometheus_target(n["publicIP"])

    def __delete_prometheus_target(self, ip):
        try:
            topic_name = 'client-promeithus'
            context = self.__create_ssl_context(cafile="certs/DigiCertCA.crt",
                                                certfile="certs/client.pem",
                                                keyfile="certs/client.key",
                                                password="fabric")
            context.check_hostname = False
            context.verify_mode = False
            producer_instance = KafkaProducer(bootstrap_servers=[self.args.kafkahost],
                                              security_protocol='SSL',
                                              ssl_context=context)
            ip = ip + ":9100"
            key = 'delete'
            key_bytes = key.encode(encoding='utf-8')
            value_bytes = ip.encode(encoding='utf-8')
            producer_instance.send(topic_name, key=key_bytes, value=value_bytes)
            producer_instance.flush()
        except Exception as e:
            self.logger.error("Exception occurred while deleting topics e=" + str(e))

    @staticmethod
    def __create_ssl_context(cafile=None, capath=None, cadata=None,
                             certfile=None, keyfile=None, password=None,
                             crlfile=None):
        """
        Simple helper, that creates an SSLContext based on params similar to
        those in ``kafka-python``, but with some restrictions like:

                * ``check_hostname`` is not optional, and will be set to True
                * ``crlfile`` option is missing. It is fairly hard to test it.

        .. _load_verify_locations: https://docs.python.org/3/library/ssl.html\
            #ssl.SSLContext.load_verify_locations
        .. _load_cert_chain: https://docs.python.org/3/library/ssl.html\
            #ssl.SSLContext.load_cert_chain

        Arguments:
            cafile (str): Certificate Authority file path containing certificates
                used to sign broker certificates. If CA not specified (by either
                cafile, capath, cadata) default system CA will be used if found by
                OpenSSL. For more information see `load_verify_locations`_.
                Default: None
            capath (str): Same as `cafile`, but points to a directory containing
                several CA certificates. For more information see
                `load_verify_locations`_. Default: None
            cadata (str/bytes): Same as `cafile`, but instead contains already
                read data in either ASCII or bytes format. Can be used to specify
                DER-encoded certificates, rather than PEM ones. For more
                information see `load_verify_locations`_. Default: None
            certfile (str): optional filename of file in PEM format containing
                the client certificate, as well as any CA certificates needed to
                establish the certificate's authenticity. For more information see
                `load_cert_chain`_. Default: None.
            keyfile (str): optional filename containing the client private key.
                For more information see `load_cert_chain`_. Default: None.
            password (str): optional password to be used when loading the
                certificate chain. For more information see `load_cert_chain`_.
                Default: None.

        """
        if cafile or capath:
            print('Loading SSL CA from %s', cafile or capath)
        elif cadata is not None:
            print('Loading SSL CA from data provided in `cadata`')
            print('`cadata`: %r', cadata)
        # Creating context with default params for client sockets.
        context = create_default_context(
            Purpose.SERVER_AUTH, cafile=cafile, capath=capath, cadata=cadata)
        # Load certificate if one is specified.
        if certfile is not None:
            print('Loading SSL Cert from %s', certfile)
            if keyfile:
                if password is not None:
                    print('Loading SSL Key from %s with password', keyfile)
                else:  # pragma: no cover
                    print('Loading SSL Key from %s without password', keyfile)
            # NOTE: From docs:
            # If the password argument is not specified and a password is required,
            # OpenSSLs built-in password prompting mechanism will be used to
            # interactively prompt the user for a password.
            context.load_cert_chain(certfile, keyfile, password)
        return context

    def delete(self):
        try:
            mb = MobiusInterface()
            self.logger.info("Deleting workflow")
            get_response = mb.get_workflow(self.args.mobiushost, self.args.workflowId)
            response = mb.delete_workflow(self.args.mobiushost, self.args.workflowId)
            self.__cleanup_monitoring(response=get_response)
        except Exception as e:
            self.logger.error(e)
            self.logger.error(traceback.format_exc())
