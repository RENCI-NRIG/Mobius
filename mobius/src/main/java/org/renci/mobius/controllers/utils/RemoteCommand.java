package org.renci.mobius.controllers.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RemoteCommand {
    private static final Logger LOGGER = LogManager.getLogger( Exec.class.getName() );
    private static final String defaultUserName = "root";

    private String userName;
    private String sshKey;

    public RemoteCommand(String userName, String sshKey) {
        this.userName = userName;
        this.sshKey = sshKey;
        if(this.userName == null) {
            this.userName = defaultUserName;
        }
    }

    public String runCmdByIP(final String cmd, String ip, boolean repeat) {
        LOGGER.debug(String.format("[%s] run command: %s", ip, cmd));
        String[] res = Exec.sshExec(userName, ip, cmd, sshKey);
        if (repeat && (res[0] == null
                || res[0].startsWith("error")
                || res[0].contains("Could not get lock")
                || res[0].contains("command not found"))) {
            LOGGER.debug(res[1]);
            LOGGER.debug(String.format("[%s] retrying to run command: %s", ip, cmd));
            res = Exec.sshExec(userName, ip, cmd, sshKey);
            if (res[0].startsWith("error")) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
        }
        LOGGER.debug("Returning " + res[0]);
        return res[0];
    }
}
