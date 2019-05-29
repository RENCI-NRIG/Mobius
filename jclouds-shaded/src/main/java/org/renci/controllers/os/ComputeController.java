package org.renci.controllers.os;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.inject.Module;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.keystone.config.KeystoneProperties;
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

/*
 * @brief class represents api to provision compute resources on openstack via NOVA API
 *
 * @author kthare10
 */
public class ComputeController implements Closeable {

    private String authUrl;
    private String user;
    private String password;
    private String domain;
    private String project;
    private final NovaApi novaApi;
    private final Set<String> regions;
    /*
     * @brief constructor
     *
     * @param authUrl - auth url for chameleon
     * @parm username - chameleon user name
     * @param password - chameleon user password
     * @param domain - chameleon user domain
     * @param project - chameleon project Name
     */
    public ComputeController(String authUrl, String user, String password, String domain, String project) {
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

        novaApi = ContextBuilder.newBuilder(new NovaApiMetadata())
                .endpoint(authUrl)
                .credentials(identity, password)
                .overrides(overrides)
                .modules(modules)
                .buildApi(NovaApi.class);

        regions = novaApi.getConfiguredRegions();
    }

    /*
     * @brief determine image id for an image
     *
     * @param region - region
     * @param imageName - image name
     *
     * @return image id
     */
    private String getImageId(String region, String imageName) {

        for (Image image : novaApi.getImageApi(region).listInDetail().concat()) {
            if (image.getName().equals(imageName)) {
                return image.getId();
            }
        }
        return null;
    }

    /*
     * @brief determine flavor id for an flavor
     *
     * @param region - region
     * @param flavorName - flavor name
     *
     * @return flavor id
     */
    private String getFlavorId(String region, String flavorName) {
        Flavor flavor = novaApi.getFlavorApi(region).get(flavorName);
        if(flavor != null) {
            System.out.println(flavor.toString());
            return flavor.getId();
        }
        return null;
    }

    /*
     * @brief determine flavor id for an flavor by fetching list of all flavors (needed as get on flavor by name doesn't work with jetstream)
     *
     * @param region - region
     * @param flavorName - flavor name
     *
     * @return flavor id
     */
    private String getFlavorIdFromList(String region, String flavorName) {
        for(Flavor flavor : novaApi.getFlavorApi(region).listInDetail().concat()) {
            if (flavor != null && flavorName.compareToIgnoreCase(flavor.getName()) == 0) {
                System.out.println(flavor.toString());
                return flavor.getId();
            }
        }
        return null;
    }

    /*
     * @brief get instance provided its name
     *
     * @param region - region
     * @param instanceName - instance name
     *
     * @return instance
     */
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

    /*
     * @brief create a key pair if does not exist
     *
     * @param region - region
     * @param sshKeyFile - ssh key
     * @param name - key pair name
     *
     * @return key pair id
     * @throws exception in case of error
     */
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
            throw new OpenstackException(400, "Key file not found e=" + e.getMessage());
        }
    }

    /*
     * @brief get instance provided its id
     *
     * @param region - region
     * @param instanceId - instance id
     *
     * @return instance
     */
    public Server getInstanceFromInstanceId(String region, String instanceId) {
        Server server = novaApi.getServerApi(region).get(instanceId);
        if(server != null) {
            System.out.println(server.toString());
        }
        return server;
    }

    /*
     * @brief allocate floating ip
     *
     * @param region - region
     * @param poolName - floating ip pool name
     *
     * @return floating ip
     * @throws exception in case of error
     */
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

    /*
     * @brief deallocate floating ip
     *
     * @param region - region
     * @param ip - floating ip
     *
     * @throws exception in case of error
     */
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

    /*
     * @brief attach floating ip
     *
     * @param region - region
     * @param instance - server to be attached to
     * @param poolName - floating ip
     *
     */
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

    /*
     * @brief get floating ip from instance
     *
     * @param instance - server
     *
     * @return floating ip
     */
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

    /*
     * @brief deattach floating ip
     *
     * @param region - region
     * @param instance - server to be attached to
     *
     */
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

    /*
     * @brief create instance
     *
     * @param region - region
     * @param sshKeyFile - sshKeyFile
     * @param imageName - imageName
     * @param flavorName - flavorName
     * @param networkId - networkId
     * @param reservation - reservation
     * @param keypairName - keypairName
     * @param name - name
     * @param userData - userData
     * @param metaData - metaData
     *
     * @return instance id
     *
     * @throws exception in case of error
     */
    public String createInstance(String region, String sshKeyFile, String imageName,
                                 String flavorName, String networkId,
                                 String reservation, String keypairName, String name,
                                 String userData, Map<String, String> metaData, String securityGroup) throws Exception{
        try {
            String imageId = getImageId(region, imageName);

            String flavorId = getFlavorId(region, flavorName);

            // Check if it exists in list of flavors; only needed for jetstream as getFlavorId doesn't work for jetstream
            if(flavorId == null) {
                flavorId = getFlavorIdFromList(region, flavorName);
            }

            if (imageId == null || flavorId == null || networkId == null) {
                System.out.println("imageId = " + imageId + " flavorId = " + flavorId + " networkId = " + networkId);
                throw new OpenstackException("invalid flavor or image or network");
            }

            String keypair = createKeyPairIfNotExists(region, sshKeyFile, keypairName);
            SchedulerHints hints = null;
            if(reservation != null) {
                hints = SchedulerHints.builder().reservation(reservation).build();
            }
            CreateServerOptions allInOneOptions = null;

            allInOneOptions = CreateServerOptions.Builder
                    .keyPairName(keypair)
                    .networks(networkId);

            if(securityGroup != null) {
                allInOneOptions.securityGroupNames(securityGroup);
            }

            if(hints != null) {
                allInOneOptions.schedulerHints(hints);
            }
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
            throw new OpenstackException(500, "Internal server error e=" + e.getMessage());
        }
    }

    /*
     * @brief delete key pair
     *
     * @param region - region
     * @param name - name
     *
     * @throws exception in case of error
     */
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

    /*
     * @brief destroy instance
     *
     * @param region - region
     * @param serverId - serverId
     *
     *
     */
    public void destroyInstance(String region, String serverId)  {
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

    /*
     * @brief close the controller
     *
     */
    public void close() {
        try {
            Closeables.close(novaApi, true);
        }
        catch (Exception e) {
            System.out.println("Exception occured while closing e=" + e);
        }
    }

}
