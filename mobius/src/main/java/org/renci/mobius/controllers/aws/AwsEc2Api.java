package org.renci.mobius.controllers.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

import org.apache.http.HttpStatus;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class AwsEc2Api {
    public final static String VpcId = "vpcId";
    public final static String InternalSubnetId = "intSubnetId";
    public final static String ExternalSubnetId = "extSubnetId";
    public final static String RouteTableId = "routeTableId";
    public final static String InternetGatewayId = "internetGatewayId";
    public final static String AssociationId = "associationId";
    public final static String SecurityGroupId = "securityGroupId";
    public final static String KeyPairId = "keyPairId";

    private AmazonEC2 ec2;

    public AwsEc2Api(String region, String accessId, String secretKey) {
        System.setProperty("aws.accessKeyId", accessId);
        System.setProperty("aws.secretKey", secretKey);
        AWSCredentialsProvider awsCredentialsProvider = new SystemPropertiesCredentialsProvider();
        ec2 = AmazonEC2ClientBuilder.standard().withRegion(region).withCredentials(awsCredentialsProvider).build();
    }

    public Map<String, String> setupNetwork(String name, String vpcCidr, String externalCidr,
                                            String internalCidr, String publicKey) throws Exception{
        Map<String, String> networkMap = new HashMap<>();
        try {
            String vpcId = createVpc(name, vpcCidr);
            networkMap.put(VpcId, vpcId);

            String externalSubnetId = createSubnet(vpcId, externalCidr);
            networkMap.put(ExternalSubnetId, externalSubnetId);

            String routeTableId = createRouteTable(vpcId);
            networkMap.put(RouteTableId, routeTableId);

            String associationId = associateRouteTableWithSubnet(routeTableId, externalSubnetId);
            networkMap.put(AssociationId, associationId);

            String internetGatewayId = createInternetGateway(vpcId);
            networkMap.put(InternetGatewayId, internetGatewayId);

            createRoute(routeTableId, internetGatewayId);

            String securityGroupId = createSecurityGroup(name, vpcId);
            networkMap.put(SecurityGroupId, securityGroupId);

            String keyPairId = createKeyPair(name, publicKey);
            networkMap.put(KeyPairId, keyPairId);

            String internalSubnetId = createSubnet(vpcId, internalCidr);
            networkMap.put(InternalSubnetId, internalSubnetId);
            return networkMap;
        }
        catch (Exception e) {
            tearNetwork(networkMap);
            throw e;
        }
    }

    public void tearNetwork(Map<String, String> networkMap) {
        if(networkMap.containsKey(InternalSubnetId)) {
            // TODO delete respective network interfaces on instance deletion
            deleteSubnet(networkMap.get(ExternalSubnetId));
        }
        if(networkMap.containsKey(KeyPairId)) {
            deleteKeyPair(networkMap.get(KeyPairId));
        }
        if(networkMap.containsKey(SecurityGroupId)) {
            deleteSecurityGroup(networkMap.get(SecurityGroupId));
        }
        if(networkMap.containsKey(InternetGatewayId)) {
            deleteInternetGateway(networkMap.get(InternetGatewayId));
        }
        if(networkMap.containsKey(AssociationId)) {
            dissociateRouteTableFromSubnet(networkMap.get(AssociationId));
        }
        if(networkMap.containsKey(RouteTableId)) {

            deleteRouteTable(networkMap.get(RouteTableId));
        }
        if(networkMap.containsKey(ExternalSubnetId)) {
            deleteSubnet(networkMap.get(ExternalSubnetId));
        }
    }

    private  String createSecurityGroup(String groupname, String vpcid) throws Exception {
        try {
            CreateSecurityGroupRequest create_request = new CreateSecurityGroupRequest()
                    .withGroupName(groupname)
                    .withDescription(groupname + "_API_group")
                    .withVpcId(vpcid);

            CreateSecurityGroupResult create_response = ec2.createSecurityGroup(create_request);
            int statusCode = create_response.getSdkHttpMetadata().getHttpStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new Exception("Failed to create Key Pair");
            }
            return create_response.getGroupId();
        }
        catch (Exception e) {
            System.out.println("Exception occured e=" + e);
            throw e;
        }
    }


    private  void deleteSecurityGroup(String groupid) {
        try {
            DeleteSecurityGroupRequest request = new DeleteSecurityGroupRequest().withGroupId(groupid);
            ec2.deleteSecurityGroup(request);
        }
        catch (Exception e) {
            System.out.println("Exception occured while deleting Security Group=" + groupid);
            System.out.println(e);
        }
    }


    private  String createKeyPair(String name, String publicKey) throws Exception{
        try {
            ImportKeyPairRequest request = new ImportKeyPairRequest()
                    .withKeyName(name)
                    .withPublicKeyMaterial(publicKey);

            ImportKeyPairResult result = ec2.importKeyPair(request);
            int statusCode = result.getSdkHttpMetadata().getHttpStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new Exception("Failed to create Key Pair");
            }
        }
        catch (AmazonEC2Exception e) {
            if(e.getErrorCode().compareToIgnoreCase("InvalidKeyPair.Duplicate") == 0) {
                return name;
            }
            throw e;
        }
        catch (Exception e) {
            System.out.println("Exception occured e=" + e);
            throw e;
        }

        return name;
    }

    private  void deleteKeyPair(String keyname) {
        try {
            DeleteKeyPairRequest request = new DeleteKeyPairRequest()
                    .withKeyName(keyname);

            ec2.deleteKeyPair(request);
        }
        catch (Exception e) {
            System.out.println("Exception occured while deleting Key pair =" + keyname);
            System.out.println(e);
        }
    }

    public  String createInstance(String namePrefix, String keyPairId, String securityGroupId, String extSubnet,
                                  String userData, String imageId, InstanceType flavor) throws Exception {

        try {

            List<InstanceNetworkInterfaceSpecification> networkInterfaceSpecificationList = new LinkedList<>();

            if (extSubnet != null) {
                InstanceNetworkInterfaceSpecification networkInterfaceSpecification = new InstanceNetworkInterfaceSpecification();
                networkInterfaceSpecification.setSubnetId(extSubnet);
                networkInterfaceSpecification.setDeviceIndex(0);
                networkInterfaceSpecification.setAssociatePublicIpAddress(true);
                List<String> list = new LinkedList<>();
                list.add(securityGroupId);
                networkInterfaceSpecification.setGroups(list);
                networkInterfaceSpecificationList.add(networkInterfaceSpecification);
            }


            List<Tag> tags = new ArrayList<Tag>();

            Tag t = new Tag();
            t.setKey("Name");
            t.setValue(namePrefix);
            tags.add(t);
            TagSpecification tagSpecification = new TagSpecification().withTags(tags).withResourceType(ResourceType.Instance);

            RunInstancesRequest runInstancesRequest = new
                    RunInstancesRequest()
                    .withImageId(imageId)
                    .withInstanceType(flavor)
                    .withMinCount(1)
                    .withMaxCount(1)
                    .withKeyName(keyPairId)
                    .withUserData(userData)
                    .withNetworkInterfaces(networkInterfaceSpecificationList)
                    .withTagSpecifications(tagSpecification);

            RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);

            String instanceId = runInstancesResult.getReservation().getInstances().get(0).getInstanceId();

            StartInstancesRequest request = new StartInstancesRequest().withInstanceIds(instanceId);

            StartInstancesResult result = ec2.startInstances(request);

            for (InstanceStateChange instance : result.getStartingInstances()) {
                InstanceState state = instance.getCurrentState();
                System.out.println("State at the beginning=" + state);
                while (state.getName().compareToIgnoreCase("running") != 0) {
                    TimeUnit.SECONDS.sleep(5);
                    state = getInstanceStatus(instanceId);
                    System.out.println("New =" + state);
                }
            }

            return instanceId;
        }
        catch (Exception e) {
            System.out.println("Exception occured while creating instance");
            System.out.println(e);
            throw e;
        }
    }

    public Instance getInstance(String instanceId) {
        DescribeInstancesRequest describeInstanceRequest = new DescribeInstancesRequest().withInstanceIds(instanceId);
        DescribeInstancesResult describeInstanceResult = ec2.describeInstances(describeInstanceRequest);
        return describeInstanceResult.getReservations().get(0).getInstances().get(0);
    }

    public InstanceState getInstanceStatus(String instanceId) {
        DescribeInstancesRequest describeInstanceRequest = new DescribeInstancesRequest().withInstanceIds(instanceId);
        DescribeInstancesResult describeInstanceResult = ec2.describeInstances(describeInstanceRequest);
        InstanceState state = describeInstanceResult.getReservations().get(0).getInstances().get(0).getState();
        return state;
    }

    public  void deleteInstance(String instanceId) {
        try {
            TerminateInstancesRequest request = new TerminateInstancesRequest().withInstanceIds(instanceId);
            TerminateInstancesResult result = ec2.terminateInstances(request);
            for(InstanceStateChange instanceStateChange : result.getTerminatingInstances()) {
                InstanceState state = instanceStateChange.getCurrentState();
                System.out.println("State at the beginning=" + state);
                while (state.getName().compareToIgnoreCase("terminated") != 0) {
                    TimeUnit.SECONDS.sleep(5);
                    state = getInstanceStatus(instanceId);
                    System.out.println("New =" + state);
                }
            }
        }
        catch (Exception e) {
            System.out.println("Exception occured while deleting instance =" + instanceId);
            System.out.println(e);
        }
    }

    private String createVpc(String name, String cidrBlock) {
        try {
            CreateVpcRequest createVpcRequest = new CreateVpcRequest(cidrBlock);
            CreateVpcResult result = ec2.createVpc(createVpcRequest);
            Vpc vpc = result.getVpc();

            List<Tag> tags = new ArrayList<Tag>();

            Tag t = new Tag();
            t.setKey("Name");
            t.setValue(name);
            tags.add(t);
            CreateTagsRequest ctr = new CreateTagsRequest();
            ctr.setTags(tags);
            ctr.withResources(vpc.getVpcId());
            ec2.createTags(ctr);

            return vpc.getVpcId();
        }
        catch (Exception e) {
            System.out.println("Exception occured while creating vpc");
            System.out.println(e);
            throw e;
        }
    }

    private void deleteVpc(String vpcId) {
        try {
            DeleteVpcRequest deleteVpcRequest = new DeleteVpcRequest().withVpcId(vpcId);
            ec2.deleteVpc(deleteVpcRequest);
        }
        catch (Exception e) {
            System.out.println("Exception occured while deleting Vpc =" + vpcId);
            System.out.println(e);
        }
    }

    private String createSubnet(String vpcid, String cidrBlock) {
        try {
            CreateSubnetRequest subnetRequest = new CreateSubnetRequest(vpcid, cidrBlock);
            CreateSubnetResult subnetResult = ec2.createSubnet(subnetRequest);

            Subnet subnet = subnetResult.getSubnet();
            return subnet.getSubnetId();
        }
        catch (Exception e) {
            System.out.println("Exception occured while creating subnet");
            System.out.println(e);
            throw e;
        }
    }

    private void deleteSubnet(String subnetId) {
        try {
            DeleteSubnetRequest subnetRequest = new DeleteSubnetRequest().withSubnetId(subnetId);
            ec2.deleteSubnet(subnetRequest);
        }
        catch (Exception e) {
            System.out.println("Exception occured while deleting Subnet =" + subnetId);
            System.out.println(e);
        }
    }

    private String createInternetGateway(String vpcid) {
        try {
            CreateInternetGatewayResult gatewayResult = ec2.createInternetGateway();
            InternetGateway gateway = gatewayResult.getInternetGateway();
            AttachInternetGatewayRequest attachInternetGatewayRequest = new AttachInternetGatewayRequest()
                    .withInternetGatewayId(gateway.getInternetGatewayId())
                    .withVpcId(vpcid);

            ec2.attachInternetGateway(attachInternetGatewayRequest);

            return gateway.getInternetGatewayId();
        }
        catch (Exception e) {
            System.out.println("Exception occured while creating internet gateway");
            System.out.println(e);
            throw e;
        }
    }

    private String createRouteTable(String vpcid) throws Exception {
        try {
            CreateRouteTableRequest routeTableRequest = new CreateRouteTableRequest().withVpcId(vpcid);
            CreateRouteTableResult routeTableResult = ec2.createRouteTable(routeTableRequest);
            RouteTable routeTable = routeTableResult.getRouteTable();

            return routeTable.getRouteTableId();
        }
        catch (Exception e) {
            System.out.println("Exception occured while attaching subnet to internet gateway");
            System.out.println(e);
            throw e;
        }
    }

    private String associateRouteTableWithSubnet(String routeTableId, String subnetId) {
        try {
            AssociateRouteTableRequest associateRouteTableRequest = new AssociateRouteTableRequest()
                    .withRouteTableId(routeTableId)
                    .withSubnetId(subnetId);
            AssociateRouteTableResult associateRouteTableResult = ec2.associateRouteTable(associateRouteTableRequest);
            return associateRouteTableResult.getAssociationId();
        }
        catch (Exception e) {
            System.out.println("Exception occured while attaching subnet to internet gateway");
            System.out.println(e);
            throw e;
        }
    }

    private void createRoute(String routeTableId, String internetGatewayId) {
        try {
            CreateRouteRequest createRouteRequest = new CreateRouteRequest()
                    .withGatewayId(internetGatewayId)
                    .withRouteTableId(routeTableId)
                    .withDestinationCidrBlock("0.0.0.0/0");

            ec2.createRoute(createRouteRequest);
        }
        catch (Exception e) {
            System.out.println("Exception occured while attaching subnet to internet gateway");
            System.out.println(e);
            throw e;
        }
    }

    public String associateNetworkWithInstance(String subnetId, String instanceId) {
        try {
            CreateNetworkInterfaceRequest networkInterfaceRequest = new CreateNetworkInterfaceRequest()
                    .withSubnetId(subnetId);

            CreateNetworkInterfaceResult createNetworkInterfaceResult = ec2.createNetworkInterface(networkInterfaceRequest);
            NetworkInterface networkInterface = createNetworkInterfaceResult.getNetworkInterface();

            AttachNetworkInterfaceRequest attachNetworkInterfaceRequest = new AttachNetworkInterfaceRequest()
                    .withInstanceId(instanceId)
                    .withNetworkInterfaceId(networkInterface.getNetworkInterfaceId())
                    .withDeviceIndex(1);
            ec2.attachNetworkInterface(attachNetworkInterfaceRequest);
            return networkInterface.getNetworkInterfaceId();
        }
        catch (Exception e) {
            System.out.println("Exception occured while attaching network interface to instance");
            System.out.println(e);
            throw e;
        }
    }

    public void deleteNetworkInterface(String networkInterfaceId) {
        try {
            DeleteNetworkInterfaceRequest deleteNetworkInterfaceRequest = new DeleteNetworkInterfaceRequest()
                    .withNetworkInterfaceId(networkInterfaceId);
            ec2.deleteNetworkInterface(deleteNetworkInterfaceRequest);
        }
        catch (Exception e) {
            System.out.println("Exception occured while deleting subnet dependent resources");
            System.out.println(e);
        }
    }

    private void dissociateRouteTableFromSubnet(String associationId) {
        try {

            DisassociateRouteTableRequest disassociateRouteTableRequest = new DisassociateRouteTableRequest()
                    .withAssociationId(associationId);
            ec2.disassociateRouteTable(disassociateRouteTableRequest);
        }
        catch (Exception e) {
            System.out.println("Exception occured while deleting subnet dependent resources");
            System.out.println(e);
        }
    }
    private void deleteRouteTable(String routeTableId) {
        try {

            DeleteRouteTableRequest deleteRouteTableRequest = new DeleteRouteTableRequest()
                    .withRouteTableId(routeTableId);
            ec2.deleteRouteTable(deleteRouteTableRequest);

        }
        catch (Exception e) {
            System.out.println("Exception occured while deleting subnet dependent resources");
            System.out.println(e);
        }
    }
    private void deleteInternetGateway(String internetGateway) {
        try {
            DeleteInternetGatewayRequest request = new DeleteInternetGatewayRequest()
                    .withInternetGatewayId(internetGateway);
            ec2.deleteInternetGateway(request);
        }
        catch (Exception e) {
            System.out.println("Exception occured while deleting subnet dependent resources");
            System.out.println(e);
        }
    }
}
