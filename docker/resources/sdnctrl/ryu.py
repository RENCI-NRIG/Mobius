import logging

from ryu.base import app_manager
from ryu.controller import dpset
from ryu.controller import ofp_event
from ryu.controller.handler import CONFIG_DISPATCHER, MAIN_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.ofproto import ofproto_v1_0
from ryu.lib.mac import haddr_to_bin
from ryu.lib.packet import packet
from ryu.lib.packet import ethernet
from ryu.lib.packet import ether_types

class BroDuplicateSwitch(app_manager.RyuApp):
  OFP_VERSIONS = [ofproto_v1_0.OFP_VERSION]

  def __init__(self, *args, **kwargs):
    super(BroDuplicateSwitch, self).__init__(*args, **kwargs)
    self.mac_to_port = {}
    with open('broConf') as f:
        self.bro_port_name = f.readline().strip()
        self.bro_mac_addr = f.readline().strip()
        print "Bro Port name: %s-%s-" % (self.bro_port_name, self.bro_mac_addr)

  @set_ev_cls(dpset.EventDP, dpset.DPSET_EV_DISPATCHER)
  def datapath_handler(self, ev):
    self.bro_port = [x.port_no for x in ev.ports if x.name == self.bro_port_name][0]
    print "Bro Port number: %s" % self.bro_port

  @set_ev_cls(ofp_event.EventOFPPacketIn, MAIN_DISPATCHER)
  def packet_in_handler(self, ev):
    # the "miss_send_length" of your switch
    if ev.msg.msg_len < ev.msg.total_len:
      print "packet truncated: only %s of %s bytes" % (ev.msg.msg_len, ev.msg.total_len)

    msg = ev.msg
    datapath = msg.datapath
    ofproto = datapath.ofproto

    pkt = packet.Packet(msg.data)
    eth = pkt.get_protocol(ethernet.ethernet)
    if eth.ethertype == ether_types.ETH_TYPE_LLDP:
        # ignore lldp packet
        return

    dst = eth.dst
    src = eth.src
    in_port = msg.in_port

    parser = datapath.ofproto_parser
    dpid = datapath.id

    print "packet in %s %s %s %s" % (dpid, src, dst, in_port)

    self.mac_to_port.setdefault(dpid, {})
    self.mac_to_port[dpid][src] = in_port

    # Learning part of the switch
    self.mac_to_port[dpid][src] = in_port
    if dst in self.mac_to_port[dpid]:
      out_port = self.mac_to_port[dpid][dst]
    else:
      out_port = ofproto.OFPP_FLOOD

    actions = []
    actions.append(parser.OFPActionOutput(out_port))
    actions.append(parser.OFPActionSetDlDst(haddr_to_bin(self.bro_mac_addr)))
    actions.append(parser.OFPActionOutput(self.bro_port))

    # install a flow to avoid packet_in next time
    if ofproto.OFPP_FLOOD != out_port:
      match = parser.OFPMatch(in_port=in_port, 
                              dl_dst=haddr_to_bin(dst))

      mod = parser.OFPFlowMod(
         datapath=datapath, match=match, cookie=0,
         command=ofproto.OFPFC_ADD, idle_timeout=0, hard_timeout=0,
         priority=ofproto.OFP_DEFAULT_PRIORITY,
         flags=ofproto.OFPFF_SEND_FLOW_REM,
         actions=actions)
      datapath.send_msg(mod)

    data = None
    if msg.buffer_id == ofproto.OFP_NO_BUFFER:
      data = msg.data

    out = parser.OFPPacketOut(
        datapath=datapath, buffer_id=msg.buffer_id, in_port=in_port,
        actions=actions, data=data)
    datapath.send_msg(out)
