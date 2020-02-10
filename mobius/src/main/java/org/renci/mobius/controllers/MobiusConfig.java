package org.renci.mobius.controllers;

import com.google.common.collect.ImmutableSet;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/*
 * @brief class implements singleton interface to load config info from applications.properties
 *
 * @author kthare10
 */
public class MobiusConfig {
    private Properties properties;
    private boolean load = true;

    private static final String MOBIUS_HOME = "MOBIUS_HOME";
    public static final String exogeniUser = "mobius.exogeni.user";
    public static final String exogeniUserKeyPath = "mobius.exogeni.KeyPath";
    public static final String exogeniUserCertKey = "mobius.exogeni.certKeyFile";
    public static final String exogeniUserSshKey = "mobius.exogeni.sshKeyFile";
    public static final String exogeniUserSshPrivateKey = "mobius.exogeni.sshPrivateKeyFile";
    public static final String exogeniControllerUrl = "mobius.exogeni.controllerUrl";
    public static final String exogeniDefaultImageUrl = "mobius.exogeni.defaultImageUrl";
    public static final String exogeniDefaultImageName = "mobius.exogeni.defaultImageName";
    public static final String exogeniDefaultImageHash = "mobius.exogeni.defaultImageHash";
    public static final String exogeniCommitRetryCount = "mobius.exogeni.commitRetryCount";
    public static final String exogeniCommitSleepInterval = "mobius.exogeni.commitSleepInterval";
    public static final String mobiusSdxUrl = "mobius.sdxUrl";

    public static final String enablePeriodicProcessingFilePath = "mobius.enable.periodic.processing.path";
    public static final String enablePeriodicProcessingFile = "mobius.enable.periodic.processing.file";
    public static final String periodicProcessingWaitTime = "mobius.periodic.processing.max.wait.time";
    public static final String periodicProcessingPeriod = "mobius.periodic.processing.period";

    public static final String amqpServerHost = "mobius.amqp.server.host";
    public static final String amqpServerPort = "mobius.amqp.server.port";
    public static final String amqpUseSsl = "mobius.amqp.use.ssl";
    public static final String amqpUserName = "mobius.amqp.user.name";
    public static final String amqpPassword = "mobius.amqp.user.password";
    public static final String amqpVirtualHost = "mobius.amqp.virtual.host";
    public static final String amqpExchangeName = "mobius.amqp.exchange.name";
    public static final String amqpRoutingKey = "mobius.amqp.exchange.routing.key";

    public static final String chameleonAuthUrl = "mobius.chameleon.authUrl";
    public static final String chameleonUser = "mobius.chameleon.user";
    public static final String chameleonUserDomain = "mobius.chameleon.user.domain";
    public static final String chameleonUserPassword = "mobius.chameleon.user.password";
    public static final String chameleonProject = "mobius.chameleon.project";
    public static final String chameleonProjectDomain = "mobius.chameleon.project.domain";
    public static final String chameleonUserKeyPath = "mobius.chameleon.KeyPath";
    public static final String chameleonUserSshKey = "mobius.chameleon.sshKeyFile";
    public static final String chameleonDefaultNetwork = "mobius.chameleon.default.network";
    public static final String chameleonDefaultImageName = "mobius.chameleon.defaultImageName";
    public static final String chameleonDefaultFlavorName = "mobius.chameleon.defaultFlavorName";
    public static final String chameleonFloatingIpPool = "mobius.chameleon.floatingIpPool";
    public static final String chameleonLeaseRetry = "mobius.chameleon.leaseRetry";
    public static final String chameleonUCStitchPort = "mobius.chameleon.uc.stitchport";
    public static final String chameleonTACCStitchPort = "mobius.chameleon.tacc.stitchport";
    public static final String chameleonDnsServers = "mobius.chameleon.dnsServers";

    public static final String cometHost = "mobius.comet.host";
    public static final String cometCert = "mobius.comet.cert";
    public static final String cometCaCert = "mobius.comet.cacert";
    public static final String cometCertPwd = "mobius.comet.cert.pwd";

    public static final String jetStreamIuAuthUrl = "mobius.jetstream.iu.authUrl";
    public static final String jetStreamTaccAuthUrl = "mobius.jetstream.tacc.authUrl";
    public static final String jetStreamUser = "mobius.jetstream.user";
    public static final String jetStreamUserDomain = "mobius.jetstream.user.domain";
    public static final String jetStreamUserPassword = "mobius.jetstream.user.password";
    public static final String jetStreamProject = "mobius.jetstream.project";
    public static final String jetStreamProjectDomain = "mobius.jetstream.project.domain";
    public static final String jetStreamUserKeyPath = "mobius.jetstream.KeyPath";
    public static final String jetStreamUserSshKey = "mobius.jetstream.sshKeyFile";
    public static final String jetStreamDefaultImageName = "mobius.jetstream.defaultImageName";
    public static final String jetStreamFloatingIpPool = "mobius.jetstream.floatingIpPool";

    public static final String mosAuthUrl = "mobius.mos.authUrl";
    public static final String mosUser = "mobius.mos.user";
    public static final String mosUserDomain = "mobius.mos.user.domain";
    public static final String mosUserPassword = "mobius.mos.user.password";
    public static final String mosProject = "mobius.mos.project";
    public static final String mosProjectDomain = "mobius.mos.project.domain";
    public static final String mosUserKeyPath = "mobius.mos.KeyPath";
    public static final String mosUserSshKey = "mobius.mos.sshKeyFile";
    public static final String mosDefaultImageName = "mobius.mos.defaultImageName";
    public static final String mosFloatingIpPool = "mobius.mos.floatingIpPool";
    public static final String mosAccessTokenEndpoint = "mobius.mos.access_token_endpoint";
    public static final String mosFederatedIdentityProvider = "mobius.mos.federated_identity_provider";
    public static final String mosClientId = "mobius.mos.client_id";
    public static final String mosClientSecret = "mobius.mos.client_secret";
    public static final String mosAccessEndpointScope = "mobius.mos.endpoint.scope";

    public static final String awsAccessId = "mobius.aws.accessId";
    public static final String awsSecreteKey = "mobius.aws.secretKey";
    public static final String awsDefaultImage = "mobius.aws.defaultImage";
    public static final String awsUserKeyPath = "mobius.aws.KeyPath";
    public static final String awsUserSshKey = "mobius.aws.sshKeyFile";

    public List<String> getChameleonDnsServers() {
        String dnsServers = properties.getProperty(MobiusConfig.chameleonDnsServers);
        return Arrays.asList(dnsServers.split(","));
    }
    public String getAwsUserSshKey() {
        String mobiusHome = getMobiusHome();
        if(mobiusHome == null) {
            return properties.getProperty(MobiusConfig.awsUserKeyPath) + "/" +
                    properties.getProperty(MobiusConfig.awsUserSshKey);
        }
        else {
            return mobiusHome + "/ssh/" +
                    properties.getProperty(MobiusConfig.awsUserSshKey);
        }
    }

    public String getAwsAccessId() { return properties.getProperty(awsAccessId); }

    public String getAwsSecreteKey() { return properties.getProperty(awsSecreteKey); }

    public String getAwsDefaultImage() { return properties.getProperty(awsDefaultImage); }

    public String getMosAuthUrl() { return properties.getProperty(mosAuthUrl); }

    public String getMosUser() { return properties.getProperty(mosUser); }

    public String getMosUserDomain() { return properties.getProperty(mosUserDomain); }

    public String getMosUserPassword() { return properties.getProperty(mosUserPassword); }

    public String getMosProject() { return properties.getProperty(mosProject); }

    public String getMosProjectDomain() { return properties.getProperty(mosProjectDomain); }

    public String getMosUserSshKey() {
        String mobiusHome = getMobiusHome();
        if(mobiusHome == null) {
            return properties.getProperty(MobiusConfig.mosUserKeyPath) + "/" +
                    properties.getProperty(MobiusConfig.mosUserSshKey);
        }
        else {
            return mobiusHome + "/ssh/" +
                    properties.getProperty(MobiusConfig.mosUserSshKey);
        }
    }

    public String getMosAccessEndpointScope() { return properties.getProperty(mosAccessEndpointScope); }

    public String getMosAccessTokenEndpoint() { return properties.getProperty(mosAccessTokenEndpoint); }

    public String getMosFederatedIdentityProvider() { return properties.getProperty(mosFederatedIdentityProvider); }

    public String getMosClientId() { return properties.getProperty(mosClientId); }

    public String getMosClientSecret() { return properties.getProperty(mosClientSecret); }

    public String getMosDefaultImageName() { return properties.getProperty(mosDefaultImageName); }

    public String getMosFloatingIpPool() { return properties.getProperty(mosFloatingIpPool); }

    public String getMobiusSdxUrl() { return properties.getProperty(mobiusSdxUrl); }

    public String getJetStreamIuAuthUrl() { return properties.getProperty(jetStreamIuAuthUrl); }

    public String getJetStreamTaccAuthUrl() { return properties.getProperty(jetStreamTaccAuthUrl); }

    public String getJetStreamUser() { return properties.getProperty(jetStreamUser); }

    public String getJetStreamUserDomain() { return properties.getProperty(jetStreamUserDomain); }

    public String getJetStreamUserPassword() { return properties.getProperty(jetStreamUserPassword); }

    public String getJetStreamProject() { return properties.getProperty(jetStreamProject); }

    public String getJetStreamProjectDomain() { return properties.getProperty(jetStreamProjectDomain); }

    public String getJetStreamUserSshKey() {
        String mobiusHome = getMobiusHome();
        if(mobiusHome == null) {
            return properties.getProperty(MobiusConfig.jetStreamUserKeyPath) + "/" +
                    properties.getProperty(MobiusConfig.jetStreamUserSshKey);
        }
        else {
            return mobiusHome + "/ssh/" +
                    properties.getProperty(MobiusConfig.jetStreamUserSshKey);
        }
    }

    public String getJetStreamDefaultImageName() { return properties.getProperty(jetStreamDefaultImageName); }

    public String getJetStreamFloatingIpPool() { return properties.getProperty(jetStreamFloatingIpPool); }

    public String getCometHost() { return properties.getProperty(cometHost); }

    public String getCometCert() { return properties.getProperty(cometCert); }

    public String getCometCaCert() { return properties.getProperty(cometCaCert); }

    public String getCometCertPwd() { return properties.getProperty(cometCertPwd); }

    public String getChameleonUCStitchPort() { return properties.getProperty(chameleonUCStitchPort); }

    public String getChameleonTACCStitchPort() { return properties.getProperty(chameleonTACCStitchPort); }

    public String getChameleonAuthUrl() { return properties.getProperty(chameleonAuthUrl); }

    public String getChameleonUser() { return properties.getProperty(chameleonUser); }

    public String getChameleonUserDomain() { return properties.getProperty(chameleonUserDomain); }

    public String getChameleonUserPassword() { return properties.getProperty(chameleonUserPassword); }

    public String getChameleonProject() { return properties.getProperty(chameleonProject); }

    public String getChameleonProjectDomain() { return properties.getProperty(chameleonProjectDomain); }

    public String getChameleonFloatingIpPool() { return properties.getProperty(chameleonFloatingIpPool); }

    public String getChameleonDefaultNetwork() { return properties.getProperty(chameleonDefaultNetwork); }

    public String getDefaultChameleonImageName() { return properties.getProperty(MobiusConfig.chameleonDefaultImageName); }

    public String getChameleonDefaultFlavorName() { return properties.getProperty(MobiusConfig.chameleonDefaultFlavorName); }

    public String getDefaultChameleonUserSshKey() {
        String mobiusHome = getMobiusHome();
        if(mobiusHome == null) {
            return properties.getProperty(MobiusConfig.chameleonUserKeyPath) + "/" +
                    properties.getProperty(MobiusConfig.chameleonUserSshKey);
        }
        else {
            return mobiusHome + "/ssh/" +
                    properties.getProperty(MobiusConfig.chameleonUserSshKey);
        }
    }

    public Integer getChameleonLeaseRetry() { return Integer.valueOf(properties.getProperty(MobiusConfig.chameleonLeaseRetry)); }

    public String getAmqpExchangeName() { return properties.getProperty(amqpExchangeName); }

    public String getAmqpRoutingKey() { return properties.getProperty(amqpRoutingKey); }

    public String getAmqpServerHost() { return properties.getProperty(amqpServerHost); }

    public Integer getAmqpServerPort() {
        if(properties.getProperty(amqpServerPort) != null) {
            return Integer.valueOf(properties.getProperty(amqpServerPort));
        }
        return -1;
    }

    public boolean getAmqpUseSsl() {
        if(properties.getProperty(amqpUseSsl) != null) {
        return Boolean.valueOf(properties.getProperty(amqpUseSsl));
        }
        return false;
    }

    public String getAmqpUserName() { return properties.getProperty(amqpUserName); }

    public String getAmqpPassword() { return properties.getProperty(amqpPassword); }

    public String getAmqpVirtualHost() { return properties.getProperty(amqpVirtualHost); }

    public String getPeriodicProcessingWaitTime() { return properties.getProperty(periodicProcessingWaitTime); }

    public String getPeriodicProcessingPeriod() { return properties.getProperty(periodicProcessingPeriod); }

    public String getEnablePeriodicProcessingFile() {
        String mobiusHome = getMobiusHome();
        if(mobiusHome == null) {
            return properties.getProperty(MobiusConfig.enablePeriodicProcessingFilePath) + "/" +
                    properties.getProperty(MobiusConfig.enablePeriodicProcessingFile);
        }
        else {
            return mobiusHome + "/" +
                    properties.getProperty(MobiusConfig.enablePeriodicProcessingFile);
        }
    }

    public String getDefaultExogeniUser() {
        return properties.getProperty(MobiusConfig.exogeniUser);
    }

    public String getDefaultExogeniUserCertKey() {
        String mobiusHome = getMobiusHome();
        if(mobiusHome == null) {
            return properties.getProperty(MobiusConfig.exogeniUserKeyPath) + "/" +
                    properties.getProperty(MobiusConfig.exogeniUserCertKey);
        }
        else {
            return mobiusHome + "/ssh/" +
                    properties.getProperty(MobiusConfig.exogeniUserCertKey);
        }
    }

    public String getDefaultExogeniUserSshKey() {
        String mobiusHome = getMobiusHome();
        if(mobiusHome == null) {
            return properties.getProperty(MobiusConfig.exogeniUserKeyPath) + "/" +
                    properties.getProperty(MobiusConfig.exogeniUserSshKey);
        }
        else {
            return mobiusHome + "/ssh/" +
                    properties.getProperty(MobiusConfig.exogeniUserSshKey);
        }
    }

    public String getDefaultExogeniUserSshPrivateKey() {
        String mobiusHome = getMobiusHome();
        if(mobiusHome == null) {
            return properties.getProperty(MobiusConfig.exogeniUserKeyPath) + "/" +
                    properties.getProperty(MobiusConfig.exogeniUserSshPrivateKey);
        }
        else {
            return mobiusHome + "/ssh/" +
                    properties.getProperty(MobiusConfig.exogeniUserSshPrivateKey);
        }
    }

    public String getDefaultExogeniImageUrl() {
        return properties.getProperty(MobiusConfig.exogeniDefaultImageUrl);
    }

    public String getDefaultExogeniImageName() {
        return properties.getProperty(MobiusConfig.exogeniDefaultImageName);
    }

    public String getDefaultExogeniImageHash() {
        return properties.getProperty(MobiusConfig.exogeniDefaultImageHash);
    }

    public Integer getDefaultExogeniCommitSleepInterval() {
        return Integer.valueOf(properties.getProperty(MobiusConfig.exogeniCommitSleepInterval));
    }

    public Integer getDefaultExogeniCommitRetryCount() {
        return Integer.valueOf(properties.getProperty(MobiusConfig.exogeniCommitRetryCount));
    }

    public String getDefaultExogeniControllerUrl() {
        return properties.getProperty(MobiusConfig.exogeniControllerUrl);
    }

    private static final MobiusConfig fINSTANCE = new MobiusConfig();
    public static MobiusConfig getInstance() {
        return fINSTANCE;
    }
    public String getProperty(String name) {
        return properties.getProperty(name);
    }

    private MobiusConfig() {
        load();
    }


    public void load() {
        InputStream input = null;

        try {
            if(load) {
                if(getMobiusHome() == null) {
                    input = MobiusConfig.class.getClassLoader().getResourceAsStream("application.properties");
                }
                else {
                    String HomeDirectory = MobiusConfig.getMobiusHome();
                    String ConfigDirectory = HomeDirectory + System.getProperty("file.separator") + "config"
                           + System.getProperty("file.separator");
                    String MobiusConfigurationFile = ConfigDirectory + "application.properties";
                    input = new FileInputStream(MobiusConfigurationFile);
                }
                properties = new Properties();
                // load a properties file
                properties.load(input);
                load = false;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String getMobiusHome() {
        // first check if MOBIUS_HOME is defined as a system property
        String mobiusHome = System.getProperty(MOBIUS_HOME);
        if (mobiusHome == null){
            // next check if there is an environment variable MOBIUS_HOME
            mobiusHome = System.getenv(MOBIUS_HOME);
        }

        return mobiusHome;
    }
}
