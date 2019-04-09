package org.renci.controllers.os;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.inject.Module;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.keystone.config.KeystoneProperties;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.NeutronApiMetadata;
import org.jclouds.openstack.neutron.v2.features.NetworkApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaApiMetadata;
import org.jclouds.openstack.nova.v2_0.domain.*;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.extensions.KeyPairApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class OpenstackController implements Closeable {

    private String authUrl;
    private String user;
    private String password;
    private String domain;
    private String project;
    private final NovaApi novaApi;
    private final NeutronApi neutronApi;
    private final Set<String> regions;

    public OpenstackController(String authUrl, String user, String password, String domain, String project) {
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

        novaApi = ContextBuilder.newBuilder(new NovaApiMetadata())
                .endpoint(authUrl)
                .credentials(identity, password)
                .overrides(overrides)
                .modules(modules)
                .buildApi(NovaApi.class);

        regions = novaApi.getConfiguredRegions();
    }

    private String getImageId(String region, String imageName) {

        for (Image image : novaApi.getImageApi(region).listInDetail().concat()) {
            if (image.getName().equals(imageName)) {
                return image.getId();
            }
        }
        return null;
    }

    private String getFlavorId(String region, String flavorName) {
        Flavor flavor = novaApi.getFlavorApi(region).get(flavorName);
        if(flavor != null) {
            System.out.println(flavor.toString());
            return flavor.getId();
        }
        return null;
    }

    private Server getInstanceFromInstanceIName(String region, String instanceName) {
        Server instance = null;
        ServerApi serverApi = novaApi.getServerApi(region);

        for (Server thisInstance : serverApi.listInDetail().concat()) {
            if (thisInstance.getName().equals(instanceName)) {
                instance = thisInstance;
            }
        }
        return instance;
    }

    private String createKeyPairIfNotExists(String region, String sshKeyFile, String name) throws Exception {
        Optional<? extends KeyPairApi> keyPairApiExtension = novaApi.getKeyPairApi(region);
        try {
            if (keyPairApiExtension.isPresent()) {
                System.out.println("Checking for existing SSH keypair...");

                KeyPairApi keyPairApi = keyPairApiExtension.get();

                boolean keyPairFound = keyPairApi.get(name) != null;

                if (keyPairFound) {
                    System.out.println("Keypair " + name + " already exists.");
                    return name;

                } else {

                    byte[] encoded = Files.readAllBytes(Paths.get(sshKeyFile));
                    String publicKey = new String(encoded);

                    System.out.println("Creating keypair.");
                    KeyPair k = keyPairApi.createWithPublicKey(name, publicKey);
                    System.out.println("Keypair " + name + " created.");

                    System.out.println("Existing keypairs:");
                    keyPairApi.list().forEach(keyPair -> System.out.println("  " + keyPair));

                    return k.getName();
                }
            } else {

                System.out.println("No keypair extension present; skipping keypair checks.");
                throw new OpenstackException(500, "No keypair extension present; skipping keypair checks.");

            }
        }
        catch (FileNotFoundException e) {
            System.out.println("Key file not found");
            throw new OpenstackException(400, "Key file not found");
        }
    }

    private String getNetworkId(String region, String networkName) {
        org.jclouds.openstack.neutron.v2.domain.Network network = null;
        NetworkApi networkApi = neutronApi.getNetworkApi(region);

        for (org.jclouds.openstack.neutron.v2.domain.Network thisNetwork : networkApi.list().concat()) {
            if (thisNetwork.getName().equals(networkName)) {
                network = thisNetwork;
            }
        }
        if(network != null) {
            return network.getId();
        }
        return null;
    }

    public Server getInstanceFromInstanceId(String region, String instanceId) {
        Server server = novaApi.getServerApi(region).get(instanceId);
        if(server != null) {
            System.out.println(server.toString());
        }
        return server;
    }

    public FloatingIP allocateFloatingIp(String region, String poolName) throws Exception {
        System.out.println("Checking for unused floating IP's...");

        FloatingIP unusedFloatingIP = null;

        if (novaApi.getFloatingIPApi(region).isPresent()) {

            FloatingIPApi floatingIPApi = novaApi.getFloatingIPApi(region).get();

            List<FloatingIP> freeIP = floatingIPApi.list().toList().stream().filter(
                    floatingIp -> floatingIp.getInstanceId() == null).collect(Collectors.toList());

            if (freeIP.size() > 0) {

                System.out.println("The following IPs are available:");
                freeIP.forEach(floatingIP -> System.out.println("  " + floatingIP.getIp()));
                unusedFloatingIP = freeIP.get(0);
                System.out.println("Using: " + unusedFloatingIP.getIp());

            } else {
                System.out.println("Creating new floating IP.... ");
                unusedFloatingIP = floatingIPApi.allocateFromPool(poolName);
            }
            return unusedFloatingIP;
        } else {

            System.out.println("No floating ip extension present; skipping floating ip creation.");
            throw new OpenstackException(500, "No floating ip extension present; skipping floating ip creation.");

        }
    }

    public void deallocateFloatingIp(String region, String ip) throws Exception {
        if (novaApi.getFloatingIPApi(region).isPresent()) {

            FloatingIPApi floatingIPApi = novaApi.getFloatingIPApi(region).get();


            List<FloatingIP> floatingIPToRemove = floatingIPApi.list().toList().stream().filter(
                    floatingIp -> floatingIp.getIp() == ip).collect(Collectors.toList());

            if (floatingIPToRemove.size() > 0) {
                System.out.println("The floatingIPToRemove :" + floatingIPToRemove);
                floatingIPToRemove.forEach(floatingIP -> floatingIPApi.delete(floatingIP.getId()));
            }
        } else {

            System.out.println("No floating ip extension present; skipping floating ip creation.");
            throw new OpenstackException(500, "No floating ip extension present; skipping floating ip creation.");
        }
    }

    public void attachFloatingIp(String region, Server instance, FloatingIP floatingIP) {

        System.out.println(instance.getAddresses());
        String ip = getFloatingIpFromInstance(instance);
        if (ip != null) {

            System.out.println("Public IP already assigned. Skipping attachment.");
        } else if (floatingIP != null) {

            System.out.println("Attaching new IP, please wait...");
            // api must be present if we have managed to allocate a floating IP
            novaApi.getFloatingIPApi(region).get().addToServer(floatingIP.getIp(), instance.getId());
            //This operation takes some indeterminate amount of time; don't move on until it's done.
            while (instance.getAccessIPv4() != null) {
                //Objects are not updated "live" so keep checking to make sure it's been added
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch(InterruptedException ex) {
                    System.out.println( "Awakened prematurely." );
                }
                instance = novaApi.getServerApi(region).get(instance.getId());
            }
        }
    }

    public String getFloatingIpFromInstance(Server instance) {
        String floatingIP = null;
        if(instance != null) {
            System.out.println("Addresses=" + instance.getAddresses());
            if (instance.getAddresses() != null && instance.getAddresses().size() > 0) {
                for (Address address : instance.getAddresses().values()) {
                    System.out.println("Address =" + address);
                    if (address.getType().get().compareToIgnoreCase("floating") == 0) {
                        floatingIP = address.getAddr();
                        System.out.println("floatingIP =" + floatingIP);
                        break;
                    }
                }
            }
        }
        else {
            System.out.println("Null instance passed");
        }
        return floatingIP;
    }

    public void deattachFloatingIp(String region, Server instance) {

        System.out.println(instance.getAddresses());

        String floatingIP = getFloatingIpFromInstance(instance);

        if (floatingIP == null) {

            System.out.println("Public IP not assigned. Skipping deattachment.");
        } else {

            System.out.println("DeAttaching new IP, please wait...");
            // api must be present if we have managed to allocate a floating IP
            novaApi.getFloatingIPApi(region).get().removeFromServer(floatingIP, instance.getId());
            //This operation takes some indeterminate amount of time; don't move on until it's done.
            while (floatingIP != null) {
                //Objects are not updated "live" so keep checking to make sure it's been added
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch(InterruptedException ex) {
                    System.out.println( "Awakened prematurely." );
                }
                instance = novaApi.getServerApi(region).get(instance.getId());
                floatingIP = getFloatingIpFromInstance(instance);
            }
        }
    }

    public String createInstance(String region, String sshKeyFile, String imageName,
                                 String flavorName, String networkName,
                                 String reservation, String keypairName, String name,
                                 String userData, Map<String, String> metaData) throws Exception{
        try {
            String imageId = getImageId(region, imageName);

            String flavorId = getFlavorId(region, flavorName);

            String networkId = getNetworkId(region, networkName);

            if (imageId == null || flavorId == null || networkId == null || reservation == null) {
                System.out.println("BAD_REQUEST: invalid flavor or image or network or reservation");
                return null;
            }

            String keypair = createKeyPairIfNotExists(region, sshKeyFile, keypairName);
            SchedulerHints hints = SchedulerHints.builder().reservation(reservation).build();
            CreateServerOptions allInOneOptions = null;

            allInOneOptions = CreateServerOptions.Builder
                    .keyPairName(keypair)
                    .networks(networkId)
                    .schedulerHints(hints);
            if(userData != null) {
                System.out.println("Begin userdata=========");
                System.out.println(userData);
                System.out.println("End userdata=========");
                allInOneOptions.userData(userData.getBytes());
            }
            if(metaData != null) {
                System.out.println("Begin metdata=========");
                System.out.println(metaData);
                System.out.println("End metdata=========");
                allInOneOptions.metadata(metaData);
            }

            System.out.println("Checking for existing instance...");
            Server instance = getInstanceFromInstanceIName(region, name);

            ServerCreated newInstance = null;
            if (instance == null) {
                System.out.println("Creating instance...");
                newInstance = novaApi.getServerApi(region).create(name, imageId, flavorId, allInOneOptions);
                System.out.println("Server created. ID: " + newInstance.getId());
                return newInstance.getId();
            } else {
                System.out.println("Instance already exists");
            }
            return instance.getId();
        }
        catch (OpenstackException e) {
            throw e;
        }
        catch (Exception e) {
            System.out.println("Exception e=" + e);
            e.printStackTrace();
            throw new OpenstackException(500, "Internal server error");
        }
    }

    public void deleteKeyPair(String region, String name) throws Exception {
        Optional<? extends KeyPairApi> keyPairApiExtension = novaApi.getKeyPairApi(region);

        if (keyPairApiExtension.isPresent()) {
            System.out.println("Checking for existing SSH keypair...");

            KeyPairApi keyPairApi = keyPairApiExtension.get();

            boolean keyPairFound = keyPairApi.delete(name);
            if (keyPairFound == true) {
                System.out.println("Keypair " + name + " deleted successfully");
            } else {
                System.out.println("Keypair " + name + " failed to delete");
            }
        } else {
            System.out.println("No keypair extension present; skipping keypair checks.");
            throw new OpenstackException(500, "No keypair extension present; skipping keypair checks.");
        }
    }

    public void destroyInstance(String region, String serverId) throws Exception {
        ServerApi serverApi = novaApi.getServerApi(region);
        Server server = getInstanceFromInstanceId(region, serverId);

        if( server != null ) {
            String floatingIP = getFloatingIpFromInstance(server);

            try {
                deattachFloatingIp(region, server);
            }
            catch (Exception e) {
                System.out.println("Ignoring exception during destroy e=" + e);
            }

            try {
                deallocateFloatingIp(region, floatingIP);
            }
            catch (Exception e) {
                System.out.println("Ignoring exception during destroy e=" + e);
            }
        }

        if (serverApi.delete(serverId)) {
            System.out.println("Server " + serverId + " being deleted, please wait.");
            serverApi.list().concat().forEach(instance -> System.out.println("  " + instance));
        } else {
            System.out.println("Server not deleted.");
        }
    }
    public void close() {
        try {
            Closeables.close(novaApi, true);
            Closeables.close(neutronApi, true);
        }
        catch (Exception e) {
            System.out.println("Exception occured while closing e=" + e);
        }
    }

}
