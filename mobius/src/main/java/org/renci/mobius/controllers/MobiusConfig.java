package org.renci.mobius.controllers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MobiusConfig {
    private Properties properties;
    private boolean load = true;

    private static final String MOBIUS_HOME = "MOBIUS_HOME";
    public static final String exogeniUser = "mobius.exogeni.user";
    public static final String exogeniUserCertKey = "mobius.exogeni.certKeyPath";
    public static final String exogeniUserSshKey = "mobius.exogeni.sshKeyPath";
    public static final String exogeniControllerUrl = "mobius.exogeni.controllerUrl";
    public static final String exogeniDefaultImageUrl = "mobius.exogeni.defaultImageUrl";
    public static final String exogeniDefaultImageName = "mobius.exogeni.defaultImageName";
    public static final String exogeniDefaultImageHash = "mobius.exogeni.defaultImageHash";
    public static final String exogeniCommitRetryCount = "mobius.exogeni.commitRetryCount";
    public static final String exogeniCommitSleepInterval = "mobius.exogeni.commitSleepInterval";
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

    public String getEnablePeriodicProcessingFile() { return properties.getProperty(enablePeriodicProcessingFile); }

    public String getDefaultExogeniUser() {
        return properties.getProperty(MobiusConfig.exogeniUser);
    }

    public String getDefaultExogeniUserCertKey() {
        return properties.getProperty(MobiusConfig.exogeniUserCertKey);
    }

    public String getDefaultExogeniUserSshKey() {
        return properties.getProperty(MobiusConfig.exogeniUserSshKey);
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
