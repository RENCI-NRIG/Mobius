import os
import requests
import threading
import time

sdx_server_address = "bogus_addr"
sdx_server_endpoint = sdx_server_address + "sdx/broload"






# Maybe retrieve this when setting up bro.
my_ip = requests.get('http://ip.42.pl/raw').text

just_sent = False
def reset_sent():
  just_sent = False

while True:
  processes = [float(x) for x in os.popen("ps -aux | grep bro | grep -v grep | tr -s ' ' | cut -d' ' -f3")]
  # Maybe get sum ?
  heaviest_process = max(processes)
  if heaviest_process > 50.0 and not just_sent:
    # Prevent spamming the exoplex.sdx server
    just_sent = True
    threading.Timer(60.0, reset_sent)

    req = requests.post(sdx_server_endpoint, json={'broip': my_ip, 'usage': heaviest_process})
    # TODO Check message was successful
    print req.text
  time.sleep(5)
