package org.renci.notificationsink;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class NotificationSubscriber {
    private static final String EXCHANGE_NAME = "EXCHANGE_NAME";
    private static final String ROUTING_KEY = "ROUTING_KEY";
    private static final String TOPIC_TYPE = "topic";
    private static final String RABBITMQ_SERVER = "RABBITMQ_SERVER";
    private static final String RABBITMQ_PORT = "RABBITMQ_PORT";

    public static void main(String[] args) throws Exception {
        System.out.println("Notification sink startup");
        ConnectionFactory factory = new ConnectionFactory();
        String rabbitMqHost = System.getenv(RABBITMQ_SERVER);
        if(rabbitMqHost == null) {
            rabbitMqHost = "127.0.0.1";
        }
        Integer port = 5672;
        if(System.getenv(RABBITMQ_PORT) != null) {
            port = Integer.parseInt(System.getenv(RABBITMQ_PORT));
        }
        System.out.println("Host = " + rabbitMqHost + " Port= " + port);  
        factory.setHost(rabbitMqHost);
        factory.setPort(port);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String exchangeName = System.getenv(EXCHANGE_NAME);
        if(exchangeName == null) {
            exchangeName = "notifications";
        }

        String routingKey = System.getenv(ROUTING_KEY);
        if(routingKey == null) {
            routingKey = "workflows";
        }

        System.out.println("exchangeName= " + exchangeName + " routingKey= " + routingKey);  
        channel.exchangeDeclare(exchangeName, TOPIC_TYPE, true);
        String queueName = channel.queueDeclare().getQueue();

        channel.queueBind(queueName, exchangeName, routingKey);

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
    }
}
