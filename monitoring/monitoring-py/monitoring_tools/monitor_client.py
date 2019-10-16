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

import sys
import os
import time
import json
import argparse
import subprocess
import socket
import psycopg2

from monitoring_tools import CONFIG, LOGGER
from daemon import runner
from optparse import OptionParser
from monitoring_tools.mobius import *
from kafka import KafkaConsumer

class MonitorDaemon:
    def __init__(self, host, db, user, pwd, mbhost, kafkahost,
                 cputhreshold, diskthreshold, memthreshold, bucketcount, leasedays):
        self._host = host
        self._db = db
        self._user = user
        self._pwd = pwd
        self._mbhost = mbhost
        self._kafkahost = kafkahost
        self._cputhreshold = cputhreshold
        self._diskthreshold = diskthreshold
        self._memthreshold = memthreshold
        self._bucketcount = bucketcount
        self._leasedays = leasedays
        self._workflows = {}

    def isThresholdExceeded(self, topicName):
        retVal = False
        consumer = KafkaConsumer(topicName, auto_offset_reset='earliest',
                              bootstrap_servers=[self._kafkahost], consumer_timeout_ms=1000)

        buckets = {}
        buckets["cpu"] = 0
        buckets["disk"] = 0
        buckets["ram"] = 0

        for msg in consumer:
            #print (msg.value.decode("utf-8", "strict"))
            value = json.loads(msg.value.decode("utf-8", "strict"))
            if int(value["idlecpu"]) < self._cputhreshold :
                buckets["cpu"] = buckets["cpu"] + 1
            if int(value["diskused"]) > self._diskthreshold :
                buckets["disk"] = buckets["disk"] + 1
            if int(value["memoryused"]) > self._memthreshold :
                buckets["ram"] = buckets["ram"] + 1

        print ("Buckets: " + json.dumps(buckets))
        if buckets["cpu"] > self._bucketcount or buckets["disk"] > self._bucketcount or buckets["ram"] > self._bucketcount:
            retVal = True
        return True

    def createComputeNode(self, workflowId, request):
        print("Issuing req: " + request)
        mb=MobiusInterface()
        jsonData = json.loads(request)
        jsonData["leaseEnd"] = round(time.time()) + (86400 * self._leasedays)
        response=mb.create_compute(self._mbhost, workflowId, jsonData)
        print ("Received Response Status Code: " + str(response.status_code))
        print ("Received Response Message: " + str(response.json()["message"]))
        print ("Received Response Status: " + str(response.json()["status"]))

    def process_workflow(self, workflowId, requestsMap):
        print ("Fetching all nodes for : " + workflowId)
        mb=MobiusInterface()
        response=mb.get_workflow(self._mbhost, workflowId)
        print ("Received Response Status Code: " + str(response.status_code))
        print ("Received Response Message: " + str(response.json()["message"]))
        print ("Received Response Status: " + str(response.json()["status"]))
        if response.status_code == 200 :
            status=json.loads(response.json()["value"])
            requests = json.loads(status["workflowStatus"])
            for req in requests:
                slices = req["slices"]
                for s in slices:
                    nodes = s["nodes"]
                    for n in nodes :
                        if "Chameleon" in s["slice"] or "Jetstream" in s["slice"]:
                            hostname=n["name"] + ".novalocal"
                        else :
                            hostname=n["name"]
                        if n["name"] == "cmnw" :
                            continue
                        print("Node= " + hostname)
                        topicName = workflowId + hostname
                        if self.isThresholdExceeded(topicName) :
                            self.createComputeNode(workflowId, requestsMap[hostname])

    def run(self):
        while True:
            """ query data from the workflow_entity table """
            conn = None
            try:
                connection_parameters = {
                    'host': self._host,
                    'database': self._db,
                    'user': self._user,
                    'password': self._pwd
                }
                conn = psycopg2.connect(**connection_parameters)
                cur = conn.cursor()
                cur.execute("SELECT workflow_id, host_names_json FROM workflow_entity")
                print("The number of workflows: ", cur.rowcount)
                row = cur.fetchone()

                while row is not None:
                    print("Processing workflow" + row[0])
                    requestsMap = json.loads(row[1])
                    self.process_workflow(row[0], requestsMap)
                    row = cur.fetchone()

                cur.close()
            except (Exception, psycopg2.DatabaseError) as error:
                print(error)
            finally:
                if conn is not None:
                    conn.close()
            time.sleep(600)

def main():
    usagestr = "Usage: monitoringd start|stop|restart options"
    parser = OptionParser(usage=usagestr, description='Python client to monitor workflows and provision additional resources if required')

    parser.add_option(
        '-t',
        '--dbhost',
        dest='dbhost',
        type = str,
        help='Database Host from which to read the Workflow Names',
        default='database'
    )
    parser.add_option(
        '-d',
        '--database',
        dest='database',
        type = str,
        help='Database Name',
        default='mobius'
    )
    parser.add_option(
        '-u',
        '--user',
        dest='user',
        type = str,
        help='Database User Name',
        default='mobius'
    )
    parser.add_option(
        '-p',
        '--password',
        dest='password',
        type = str,
        help='Database Password',
        default='mobius'
    )
    parser.add_option(
        '-k',
        '--kafkahost',
        dest='kafkahost',
        type = str,
        help='Kafka Host'
    )
    parser.add_option(
        '-m',
        '--mobiushost',
        dest='mobiushost',
        type = str,
        help='Mobius Host e.g. http://localhost:8080/mobius',
        default='http://localhost:8080/mobius'
    )
    parser.add_option(
         '-c',
         '--cputhreshold',
         dest='cputhreshold',
         type = int,
         help='Threshold for idle cpu usage in percent; when idle cpu usage is less than the threshold, additional compute nodes are requested via Mobius',
         default=15
     )
    parser.add_option(
         '-f',
         '--diskthreshold',
         dest='diskthreshold',
         type = int,
         help='Threshold for disk usage in percent; when disk usage exceeds the threshold, additional compute nodes are requested via Mobius',
         default=85
     )
    parser.add_option(
         '-r',
         '--memthreshold',
         dest='memthreshold',
         type = int,
         help='Threshold for memory usage in percent; when memory usage exceeds the threshold, additional compute nodes are requested via Mobius',
         default=85
     )
    parser.add_option(
         '-b',
         '--bucketcount',
         dest='bucketcount',
         type = int,
         help='Number of buckets for which the threshold should be exceeded to trigger provisioning',
         default=10
     )
    parser.add_option(
         '-l',
         '--leasedays',
         dest='leasedays',
         type = int,
         help='Number of days for which lease should be requested',
         default=2
     )

    options, args = parser.parse_args()

    if len(args) != 1:
        parser.print_help()
        sys.exit(1)

    if args[0] == 'start':
        sys.argv = [sys.argv[0], 'start']
    elif args[0] == 'stop':
        sys.argv = [sys.argv[0], 'stop']
    elif args[0] == 'restart':
        sys.argv = [sys.argv[0], 'restart']
    else:
        parser.print_help()
        sys.exit(1)

    initial_log_location = '/dev/null'
    log_format = '%(asctime)s - %(levelname)s - %(message)s'
    logging.basicConfig(format=log_format, filename=initial_log_location)
    log = logging.getLogger(LOGGER)
    log.setLevel('DEBUG')


    app = MonitorDaemon(options.dbhost, options.database, options.user, options.password, options.mobiushost, options.kafkahost,
                        options.cputhreshold, options.diskthreshold, options.memthreshold, options.bucketcount, options.leasedays)
    daemon_runner = runner.DaemonRunner(app)

    try:

        log_dir = "/var/log/monitoring/"
        log_level = "DEBUG"
        log_file = "monitoring.log"
        log_retain = 5
        log_file_size = 5000000
        log_level = 'DEBUG'

        if not os.path.exists(log_dir):
             os.makedirs(log_dir)

        handler = logging.handlers.RotatingFileHandler(
                 log_dir + '/' + log_file,
                 backupCount=log_retain,
                 maxBytes=log_file_size)
        handler.setLevel(log_level)
        formatter = logging.Formatter(log_format)
        handler.setFormatter(formatter)

        log.addHandler(handler)
        log.propagate = False
        log.info('Logging Started')

        daemon_runner.daemon_context.files_preserve = [
                 handler.stream,
             ]

        log.info('Administrative operation: %s' % args[0])
        daemon_runner.do_action()
        log.info('Administrative after action: %s' % args[0])

    except runner.DaemonRunnerStopFailureError as drsfe:
        log.propagate = True
        log.error('Unable to stop service; reason was: %s' % str(drsfe))
        log.error('Exiting...')
        sys.exit(1)
    sys.exit(0)

if __name__ == '__main__':
    main()
