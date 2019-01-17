package org.renci.mobius.notification;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.renci.mobius.controllers.MobiusConfig;

public class NotificationPublisher {
    ConnectionFactory factory;
    boolean connected;
    private static final NotificationPublisher fINSTANCE = new NotificationPublisher();
    public static NotificationPublisher getInstance() {
        return fINSTANCE;
    }

    private static final String ExchangeType = "topic";

    private NotificationPublisher()  {
        factory = null;
        connected = false;
    }

    private synchronized void connect() {
        try {
            factory = new ConnectionFactory();
            factory.setHost(MobiusConfig.getInstance().getAmqpServerHost());
            factory.setPort(MobiusConfig.getInstance().getAmqpServerPort());
            if (MobiusConfig.getInstance().getAmqpUseSsl()) {
                System.out.println("Setting useSsl to " + true);
                factory.useSslProtocol();
            }
            if(MobiusConfig.getInstance().getAmqpUserName() != null &&
                    !MobiusConfig.getInstance().getAmqpUserName().isEmpty()) {
                System.out.println("Setting username to " + MobiusConfig.getInstance().getAmqpUserName());
                factory.setUsername(MobiusConfig.getInstance().getAmqpUserName());
            }
            if(MobiusConfig.getInstance().getAmqpPassword() != null &&
                    !MobiusConfig.getInstance().getAmqpPassword().isEmpty()) {
                factory.setPassword(MobiusConfig.getInstance().getAmqpPassword());
                System.out.println("Setting password to " + MobiusConfig.getInstance().getAmqpPassword());
            }
            if(MobiusConfig.getInstance().getAmqpVirtualHost() != null &&
                    !MobiusConfig.getInstance().getAmqpVirtualHost().isEmpty()) {
                factory.setVirtualHost(MobiusConfig.getInstance().getAmqpVirtualHost());
                System.out.println("Setting virtualHost to " + MobiusConfig.getInstance().getAmqpVirtualHost());
            }
            connected = true;
        }
        catch (Exception e) {
            System.out.println("Failed to connect to AMQP");
        }
    }

    public boolean isConnected() {
        if(!connected) {
            connect();
        }
        return connected;
    }

    public synchronized void push(String workflowId, String notification) {
        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(MobiusConfig.getInstance().getAmqpExchangeName(), ExchangeType, true);
            channel.basicPublish(MobiusConfig.getInstance().getAmqpExchangeName(), MobiusConfig.getInstance().getAmqpRoutingKey(), null, notification.getBytes());
        }
        catch (Exception e) {
            System.out.println("Exception occured while sending notification e=" + e);
            e.printStackTrace();
        }
    }

}
