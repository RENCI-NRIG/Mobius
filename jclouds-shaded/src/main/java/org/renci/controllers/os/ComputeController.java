package org.renci.controllers.os;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.inject.Module;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.cinder.v1.CinderApi;
import org.jclouds.openstack.cinder.v1.CinderApiMetadata;
import org.jclouds.openstack.cinder.v1.domain.Volume;
import org.jclouds.openstack.cinder.v1.features.VolumeApi;
import org.jclouds.openstack.cinder.v1.options.CreateVolumeOptions;
import org.jclouds.openstack.cinder.v1.predicates.VolumePredicates;
import org.jclouds.openstack.keystone.config.KeystoneProperties;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaApiMetadata;
import org.jclouds.openstack.nova.v2_0.domain.*;
import org.jclouds.openstack.nova.v2_0.domain.regionscoped.RegionAndId;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeAttachmentApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.extensions.KeyPairApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;
import org.jclouds.scriptbuilder.ScriptBuilder;
import org.jclouds.scriptbuilder.domain.OsFamily;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.jclouds.scriptbuilder.domain.Statements.exec;

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
    private final CinderApi cinderApi;
    private final ComputeService computeService;
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

        cinderApi = ContextBuilder.newBuilder(new CinderApiMetadata())
                .endpoint(authUrl)
                .credentials(identity, password)
                .overrides(overrides)
                .modules(modules)
                .buildApi(CinderApi.class);


        ComputeServiceContext context = ContextBuilder.newBuilder("openstack-nova")
                .credentials(identity, password)
                .modules(modules)
                .overrides(overrides)
                .buildView(ComputeServiceContext.class);

        computeService = context.getComputeService();
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
    public Server getInstanceFromInstanceName(String region, String instanceName) {
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

                    System.out.println("Creating keypair using " + sshKeyFile);
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
     * @param ip - ip
     * @param securityGroup - securityGroup
     *
     * @return instance id
     *
     * @throws exception in case of error
     */
    public String createInstance(String region, String sshKeyFile, String imageName,
                                 String flavorName, String networkId,
                                 String reservation, String keypairName, String name,
                                 String userData, Map<String, String> metaData,
                                 String ip,
                                 String securityGroup) throws Exception{
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
                    .keyPairName(keypair);
            if(ip == null) {
                allInOneOptions.networks(networkId);
            }
            else {
                System.out.println("Assigning IP Address " + ip);
                Network network = Network.builder().networkUuid(networkId).fixedIp(ip).build();
                List<Network> networks = new LinkedList<>();
                networks.add(network);
                allInOneOptions.novaNetworks(networks);
            }

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
            Server instance = getInstanceFromInstanceName(region, name);

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

            try {
                deleteVolumesFromServer(region, serverId);
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
     * @brief create a volume
     *
     * @param region - region
     * @param name - volume name
     * @param size - volume size in GB
     *
     * @return volume
     * @throws Exception in case of failure
     */
    public org.jclouds.openstack.cinder.v1.domain.Volume createVolume(String region, String name, Integer size) throws Exception {
        CreateVolumeOptions options = CreateVolumeOptions.Builder
                .name(name);

        VolumeApi volumeApi = cinderApi.getVolumeApi(region);

        // 100 GB is the minimum volume size on the Rackspace Cloud
        org.jclouds.openstack.cinder.v1.domain.Volume volume = volumeApi.create(size, options);

        // Wait for the volume to become Available before moving on
        // If you want to know what's happening during the polling, enable logging. See
        // /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
        if (!VolumePredicates.awaitAvailable(volumeApi).apply(volume)) {
            throw new TimeoutException("Timeout on volume: " + volume);
        }

        System.out.format("Volume created successfully  %s%n", volume);

        return volume;
    }

    /*
     * @brief get a volume
     *
     * @param region - region
     * @param name - volume name
     *
     * @return volume
     */
    private org.jclouds.openstack.cinder.v1.domain.Volume getVolume(String region, String name) {
        VolumeApi volumeApi = cinderApi.getVolumeApi(region);
        org.jclouds.openstack.cinder.v1.domain.Volume volume = null;
        for (org.jclouds.openstack.cinder.v1.domain.Volume thisVolume : volumeApi.list()) {
            if (thisVolume.getName().equals(name)) {
                volume = thisVolume;
            }
        }
        return volume;
    }

    /*
     * @brief delete a volume
     *
     * @param region - region
     * @param id - volume id
     *
     * @return none
     */
    public void deleteVolume(String region, String id)  {
        VolumeApi volumeApi = cinderApi.getVolumeApi(region);
        org.jclouds.openstack.cinder.v1.domain.Volume volume = volumeApi.get(id);
        if(volume != null) {
            volumeApi.delete(volume.getId());
        }
        else {
            System.out.println("Volume " + id + " could not be deleted as it does not exist!");
        }
    }
    /*
     * @brief attach volume instance
     *
     * @param region - region
     * @param serverId - serverId
     * @param volume - volume
     * @param deviceName - deviceName
     *
     * @throws Exception in case of failure
     */
    private void attachVolume(String region, String serverId, Volume volume, String deviceName) throws Exception {
        VolumeAttachmentApi volumeAttachmentApi = novaApi.getVolumeAttachmentApi(region).get();
        VolumeApi volumeApi = cinderApi.getVolumeApi(region);

        if( volumeAttachmentApi != null ) {
            VolumeAttachment volumeAttachment = volumeAttachmentApi.attachVolumeToServerAsDevice(volume.getId(), serverId, deviceName);

            // Wait for the volume to become Attached (aka In Use) before moving on
            if (!VolumePredicates.awaitInUse(volumeApi).apply(volume)) {
                throw new TimeoutException("Timeout on volume: " + volume);
            }

            System.out.format("Volume attached successfully  %s%n", volumeAttachment);
        }
        else {
            throw new OpenstackException("VolumeAttachment Api not available");
        }
    }

    /*
     * @brief attach volume instance
     *
     * @param serverId - serverId
     * @param deviceName - deviceName
     *
     * @throws Exception in case of failure
     */
    private void mountVolume(String serverId, String deviceName, String mntPoint, String password) throws Exception {

        System.out.format("Mount Volume and Create Filesystem%n");

        NodeMetadata nodeMetadata = computeService.getNodeMetadata(serverId);
        LoginCredentials loginCredentials = nodeMetadata.getCredentials();
        if(loginCredentials.getOptionalPassword().isPresent()) {
            password = loginCredentials.getOptionalPassword().get();
            System.out.format("Using passowrd=" + password);
        }

        String script = new ScriptBuilder()
                .addStatement(exec("mkfs -t ext4 " + deviceName))
                .addStatement(exec("mount " + deviceName + " " + mntPoint))
                .render(OsFamily.UNIX);

        RunScriptOptions options = RunScriptOptions.Builder
                .blockOnComplete(true)
                .overrideLoginPassword(password);

        ExecResponse response = computeService.runScriptOnNode(serverId, script, options);

        if (response.getExitStatus() == 0) {
            System.out.format("  Exit Status: %s%n", response.getExitStatus());
        }
        else {
            System.out.format("  Error: %s%n", response.getOutput());
        }
    }

    /**
     * Make sure you've unmounted the volume first. Failure to do so could result in failure or data loss.
     */
    private void unmountVolume(String region, String serverId, String password, String mntPoint) {
        System.out.format("Unmount Volume%n");

        String script = new ScriptBuilder().addStatement(exec("umount " + mntPoint)).render(OsFamily.UNIX);

        RunScriptOptions options = RunScriptOptions.Builder
                .overrideLoginUser("root")
                .overrideLoginPassword(password)
                .blockOnComplete(true);

        RegionAndId regionAndId = RegionAndId.fromRegionAndId(region, serverId);
        ExecResponse response = computeService.runScriptOnNode(regionAndId.slashEncode(), script, options);

        if (response.getExitStatus() == 0) {
            System.out.format("  Exit Status: %s%n", response.getExitStatus());
        }
        else {
            System.out.format("  Error: %s%n",response.getOutput());
        }
    }

    private void detachVolume(String region, String volumeId, String serverId) throws Exception {
        System.out.format("Detach Volume%n");

        VolumeAttachmentApi volumeAttachmentApi = novaApi.getVolumeAttachmentApi(region).get();
        VolumeApi volumeApi = cinderApi.getVolumeApi(region);
        if(volumeAttachmentApi != null) {
            boolean result = volumeAttachmentApi.detachVolumeFromServer(volumeId, serverId);

            // Wait for the volume to become Attached (aka In Use) before moving on
            // If you want to know what's happening during the polling, enable
            // logging. See /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
            if (!VolumePredicates.awaitAvailable(volumeApi).apply(Volume.forId(volumeId))) {
                throw new TimeoutException("Timeout on volume: " + volumeId);
            }

            System.out.format("  %s%n", result);
        }
        else {
            throw new OpenstackException("VolumeAttachment Api not available");
        }
    }

    public String addVolumeToServer(String region, String serverId, String deviceName, String mntPoint, String name, Integer size, String password) throws Exception {

        VolumeAttachmentApi volumeAttachmentApi = novaApi.getVolumeAttachmentApi(region).get();
        if(volumeAttachmentApi != null) {

            if( volumeAttachmentApi.listAttachmentsOnServer(serverId).size() > 0) {
                throw new OpenstackException("storage already attached to the instance");
            }
            Volume volume = null;

            try {
                volume = createVolume(region, name, size);
                attachVolume(region, serverId, volume, deviceName);
                //mountVolume(serverId, deviceName, mntPoint, password);
                return volume.getId();
            } catch (Exception e) {
                if (volume != null) {
                    deleteVolumeFromServer(region, volume.getId(), password, mntPoint, serverId);
                }
                throw e;
            }
        }
        else {
            throw new OpenstackException("VolumeAttachment Api not available");
        }
    }

    public void deleteVolumeFromServer(String region, String volumeId, String password, String mntPoint, String serverId) {
        try {
            if(serverId == null) {
                VolumeApi volumeApi = cinderApi.getVolumeApi(region);
                Volume volume = volumeApi.get(volumeId);
                if(volume.getAttachments().size() != 0) {
                    org.jclouds.openstack.cinder.v1.domain.VolumeAttachment volumeAttachment = volume.getAttachments().iterator().next();
                    if (volumeAttachment != null) {
                        serverId = volumeAttachment.getServerId();
                    }
                }
            }
            //unmountVolume(region, serverId, password, mntPoint);
            detachVolume(region, volumeId, serverId);
        }
        catch (Exception e) {

        }
        finally {
            if(volumeId != null){
                deleteVolume(region, volumeId);
            }
        }
    }

    public void deleteVolumesFromServer(String region, String serverId) {
        String volumeId = null;
        try {
            VolumeAttachmentApi volumeAttachmentApi = novaApi.getVolumeAttachmentApi(region).get();
            for(VolumeAttachment volumeAttachment : volumeAttachmentApi.listAttachmentsOnServer(serverId)) {
                volumeId = volumeAttachment.getVolumeId();
                detachVolume(region, volumeAttachment.getVolumeId(), serverId);
            }
        }
        catch (Exception e) {

        }
        finally {
            if(volumeId != null){
                deleteVolume(region, volumeId);
            }
        }
    }

    /*
     * @brief close the controller
     *
     */
    public void close() {
        try {
            Closeables.close(novaApi, true);
            Closeables.close(cinderApi, true);
        }
        catch (Exception e) {
            System.out.println("Exception occured while closing e=" + e);
        }
    }

}
