package org.renci.mobius.controllers.utils;

import com.jcraft.jsch.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class Exec {
    private static final Logger LOGGER = LogManager.getLogger( Exec.class.getName() );
    final static int RETRYTIMES = 5;

    public static String exec(String cmd) {
        LOGGER.debug(cmd);
        String res = "";
        try {
            Runtime run = Runtime.getRuntime();
            Process pr = run.exec(cmd);
            pr.waitFor();
            BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line = "";
            while ((line = buf.readLine()) != null) {
                res = res + line;
                LOGGER.debug(line);
            }
        } catch (Exception e) {
            LOGGER.debug("Exeption while setting up link" + e);
        }
        return res;
    }

    public static Session getSession(String user, String host, String privkey) {
        Session session = null;
        boolean flag = true;
        int times = 0;
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(privkey);
            while (flag) {
                times++;
                session = jsch.getSession(user, host, 22);
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.setServerAliveInterval(10000);
                try {
                    session.connect();
                    return session;
                } catch (Exception ex) {
                    LOGGER.debug(ex.getMessage());
                    LOGGER.debug(" wait 10s and retry");
                    if (times == RETRYTIMES) {
                        break;
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (Exception exc) {
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String[] runCmd(Session session, String command) {
        String result = "";
        String errResult = "";
        try {
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);
            InputStream in = null;
            InputStream err = null;
            in = channel.getInputStream();
            err = ((ChannelExec) channel).getErrStream();
            channel.connect();
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    result += new String(tmp, 0, i);
                    LOGGER.debug(new String(tmp, 0, i));
                }
                while (err.available() > 0) {
                    int i = err.read(tmp, 0, 1024);
                    if (i < 0) break;
                    errResult += new String(tmp, 0, i);
                    LOGGER.debug(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    //get status returns int;
                    LOGGER.debug("exit-status: " + channel.getExitStatus());
                    if (channel.getExitStatus() != 0) {
                        result = "error:" + channel.getExitStatus() + "\n" + errResult;
                    }
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {
            LOGGER.debug(e.getMessage());
            e.printStackTrace();
            return new String[]{null, null};
        }
        return new String[]{result, errResult};

    }

    public static String[] sshExec(String user, String host, String command, String privkey) {
        StringBuilder result = new StringBuilder();
        StringBuilder errResult = new StringBuilder();
        LOGGER.debug(host + ":" + command);
        try {
            Session session = null;
            session = getSession(user, host, privkey);
            while (session == null) {
                session = getSession(user, host, privkey);
                LOGGER.warn(String.format("Returned session is null: %s %s %s", user, host, privkey));
            }
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);
            InputStream in = null;
            InputStream err = null;
            in = channel.getInputStream();
            err = ((ChannelExec) channel).getErrStream();
            channel.connect();
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    result.append(new String(tmp, 0, i));
                    LOGGER.debug(new String(tmp, 0, i));
                }
                while (err.available() > 0) {
                    int i = err.read(tmp, 0, 1024);
                    if (i < 0) break;
                    errResult.append(new String(tmp, 0, i));
                    LOGGER.debug(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    //get status returns int;
                    LOGGER.debug("exit-status: " + channel.getExitStatus());
                    if (channel.getExitStatus() != 0) {
                        result.append("error:" + channel.getExitStatus() + "\n");
                        result.append(errResult.toString());
                    }
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {
            LOGGER.debug(host + " " + command);
            LOGGER.debug(e.getMessage());
            e.printStackTrace();
            return new String[]{null, null};
        }
        return new String[]{result.toString(), errResult.toString()};
    }

    public static class MyUserInfo implements UserInfo, UIKeyboardInteractive {
        final GridBagConstraints gbc =
                new GridBagConstraints(0, 0, 1, 1, 1, 1,
                        GridBagConstraints.NORTHWEST,
                        GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0);
        String passwd;
        JTextField passwordField = new JPasswordField(20);
        private Container panel;

        public String getPassword() {
            return passwd;
        }

        public boolean promptYesNo(String str) {
            Object[] options = {"yes", "no"};
            int foo = JOptionPane.showOptionDialog(null,
                    str,
                    "Warning",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null, options, options[0]);
            return foo == 0;
        }

        public String getPassphrase() {
            return null;
        }

        public boolean promptPassphrase(String message) {
            return true;
        }

        public boolean promptPassword(String message) {
            Object[] ob = {passwordField};
            int result =
                    JOptionPane.showConfirmDialog(null, ob, message,
                            JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                passwd = passwordField.getText();
                return true;
            } else {
                return false;
            }
        }

        public void showMessage(String message) {
            JOptionPane.showMessageDialog(null, message);
        }

        public String[] promptKeyboardInteractive(String destination,
                                                  String name,
                                                  String instruction,
                                                  String[] prompt,
                                                  boolean[] echo) {
            panel = new JPanel();
            panel.setLayout(new GridBagLayout());

            gbc.weightx = 1.0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.gridx = 0;
            panel.add(new JLabel(instruction), gbc);
            gbc.gridy++;

            gbc.gridwidth = GridBagConstraints.RELATIVE;

            JTextField[] texts = new JTextField[prompt.length];
            for (int i = 0; i < prompt.length; i++) {
                gbc.fill = GridBagConstraints.NONE;
                gbc.gridx = 0;
                gbc.weightx = 1;
                panel.add(new JLabel(prompt[i]), gbc);

                gbc.gridx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weighty = 1;
                if (echo[i]) {
                    texts[i] = new JTextField(20);
                } else {
                    texts[i] = new JPasswordField(20);
                }
                panel.add(texts[i], gbc);
                gbc.gridy++;
            }

            if (JOptionPane.showConfirmDialog(null, panel,
                    destination + ": " + name,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE)
                    == JOptionPane.OK_OPTION) {
                String[] response = new String[prompt.length];
                for (int i = 0; i < prompt.length; i++) {
                    response[i] = texts[i].getText();
                }
                return response;
            } else {
                return null;  // cancel
            }
        }
    }
}

