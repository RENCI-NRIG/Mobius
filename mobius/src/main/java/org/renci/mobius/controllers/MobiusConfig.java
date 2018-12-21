package org.renci.mobius.controllers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MobiusConfig {
    private Properties properties;
    private boolean load = true;

    public static final String configFile = "src/main/resources/application.properties";
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
                input = new FileInputStream(configFile);
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
}
