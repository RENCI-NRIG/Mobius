
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

import sys
import argparse

from mobius.comet.comet_common_iface import CometInterface


def main():
    parser = argparse.ArgumentParser()

    parser.add_argument(
        '-c',
        '--comethost',
        dest='comethost',
        type = str,
        help='Comet Host default(https://comet-hn1.exogeni.net:8111/) used only for provisioning resources on chameleon',
        required=False,
        default='https://comet-hn1.exogeni.net:8111/'
    )
    parser.add_argument(
        '-t',
        '--cert',
        dest='cert',
        type = str,
        help='Comet Certificate default(certs/inno-hn_exogeni_net.pem); used only for provisioning resources on chameleon',
        required=False,
        default='certs/inno-hn_exogeni_net.pem'
    )
    parser.add_argument(
        '-k',
        '--key',
        dest='key',
        type = str,
        help='Comet Certificate key default(certs/inno-hn_exogeni_net.key); used only for provisioning resources on chameleon',
        required=False,
        default='certs/inno-hn_exogeni_net.key'
    )
    parser.add_argument(
        '-w',
        '--workflowId',
        dest='workflowId',
        type = str,
        help='workflowId',
        required=True
    )

    args = parser.parse_args()

    if args.comethost is not None and args.workflowId is not None:
        print("Cleaning up COMET context for workflow")
        comet = CometInterface(args.comethost, None, args.cert, args.key, None)
        readToken = args.workflowId + "read"
        writeToken = args.workflowId + "write"
        response = comet.delete_families(args.comethost, args.workflowId, None, readToken, writeToken)
    else:
        parser.print_help()
        sys.exit(1)

    sys.exit(0)


if __name__ == '__main__':
    main()
