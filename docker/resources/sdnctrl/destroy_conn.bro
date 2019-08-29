@load base/protocols/conn
@load base/files/hash
@load base/frameworks/openflow
@load base/frameworks/netcontrol

global of_controller: OpenFlow::Controller;

# Switch datapath ID
const switch_dpid: count = bogus_dpid;
# Controller Address
const controller_addr: addr = bogus_addr;
# Evil Sha1
const evil_file_sha1: string = "5ba00e7d99175cf4aa53ad5787e2efe6f1398216";

event NetControl::init() &priority=2
        {
        of_controller = OpenFlow::ryu_new(controller_addr, 8080, switch_dpid);
        local pacf_of = NetControl::create_openflow(of_controller, NetControl::OfConfig($monitor=T, $forward=T, $priority_offset=+5));
        NetControl::activate(pacf_of, 0);
        }

event NetControl::init_done()
        {
        print "NeControl is starting operations";
        }

event file_hash(f: fa_file, kind: string, hash: string)
        {
        if ( kind == "sha1" && hash == evil_file_sha1 )
                {
                for ( c in f$conns)
                        {
                        local id = f$conns[c]$id;
                        NetControl::drop_address(id$orig_h, 0sec);
                        }
                }
        }

event file_new(f: fa_file)
        {
        Files::add_analyzer(f, Files::ANALYZER_SHA1);
        }
