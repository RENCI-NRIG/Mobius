package org.renci.controllers.os;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.keystone.config.KeystoneProperties;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.NeutronApiMetadata;
import org.jclouds.openstack.neutron.v2.domain.*;
import org.jclouds.openstack.neutron.v2.extensions.RouterApi;
import org.jclouds.openstack.neutron.v2.features.NetworkApi;
import org.jclouds.openstack.neutron.v2.features.SubnetApi;


import java.util.Properties;
import java.util.Set;

public class NetworkController {
    private String authUrl;
    private String user;
    private String password;
    private String domain;
    private String project;
    private final NeutronApi neutronApi;

    public NetworkController(String authUrl, String user, String password, String domain, String project) {
        this.authUrl = authUrl;
        this.user = user;
        this.password = password;
        this.domain = domain;
        this.project = project;

        Iterable<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());

        // Please refer to 'Keystone v2-v3 authentication' section for complete authentication use case
        final Properties overrides = new Properties();
        overrides.put(KeystoneProperties.KEYSTONE_VERSION, "3");
        overrides.put(KeystoneProperties.SCOPE, "project:" + project);

        String identity = domain + ":" + user;

        neutronApi = ContextBuilder.newBuilder(new NeutronApiMetadata())
                .endpoint(authUrl)
                .credentials(identity, password)
                .overrides(overrides)
                .modules(modules)
                .buildApi(NeutronApi.class);
    }
    public String createNetwork(String region, String physicalNetworkName, String externalNetworkId, boolean shared, String cidr, String name) {
        NetworkApi networkApi = neutronApi.getNetworkApi(region);
        Network net = null;

        try {
            Network.CreateBuilder createBuilder = Network.createBuilder(name)
                    .networkType(NetworkType.VLAN)
                    .physicalNetworkName(physicalNetworkName)
                    .shared(shared);
            net = networkApi.create(createBuilder.build());
            String subnetName = name + "subnet";
            Subnet subnet = createSubnet(region, net.getId(), true, 4, cidr, subnetName);
            String routerName = name + "router";
            Router router = createRouter(region, externalNetworkId, subnet.getId(), routerName);

        } catch (Exception e){
            if(net != null) {
                networkApi.delete(net.getId());
                net = null;
            }
            System.out.println("Exception occured while creating network " + name + " e=" + e);
            throw e;
        }
        return net.getId();
    }

    private Subnet createSubnet(String region, String netId, boolean enableDhcp, Integer ipVersion, String cidr, String name) {
        SubnetApi subnetApi = neutronApi.getSubnetApi(region);
        Subnet subnet = null;
        try {

            subnet = subnetApi.create(Subnet.createBuilder(netId, cidr)
                    .ipVersion(ipVersion)
                    .enableDhcp(enableDhcp)
                    .name(name)
                    .build());
        } catch (Exception e){
            if(subnet != null) {
                subnetApi.delete(subnet.getId());
            }
            System.out.println("Exception occured while creating network " + name + " e=" + e);
            throw e;
        }
        return subnet;
    }

    private Router createRouter(String region, String externalNetworkId, String subnetId, String name) {
        Optional<RouterApi> routerApi = neutronApi.getRouterApi(region);
        Router router = null;
        ExternalGatewayInfo externalGatewayInfo = null;
        try {
            if(routerApi.isPresent()) {
                externalGatewayInfo = ExternalGatewayInfo.builder().networkId(externalNetworkId).build();

                router = routerApi.get().create(Router.createBuilder()
                        .name(name)
                        .externalGatewayInfo(externalGatewayInfo)
                        .build());
            }

        } catch (Exception e){
            if(router != null) {
                routerApi.get().delete(router.getId());
            }
            System.out.println("Exception occured while creating network " + name + " e=" + e);
            throw e;
        }
        return router;
    }

}
