package org.kendar.amqp.v09.exchange;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;

@SuppressWarnings("ThrowablePrintedToSystemOut")
public class Queue {
    private static final String HOST = "localhost";
    private Channel channel;

    public Queue(ConnectionFactory cf) {
        try {
            Connection connection = cf.newConnection();
            channel = connection.createChannel();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public void sendMessage(String exchange, String key, String message) {
        try {
            channel.basicPublish(exchange, key, null, message.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public void listenToQueue(String queueName, DeliverCallback dlr) {
        try {
            System.out.println("START CONSUMING -----------------------------");
            channel.basicConsume(queueName, true, dlr, consumerTag -> {
            });
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public void createExchangeQueue(String queueName, String exchangeName, String exchangeType, String key) {
        try {
            System.out.println("QUEUE DECLARE -----------------------------");
            channel.queueDeclare(queueName, false, false, false, null);
            System.out.println("EXCHANGE DECLARE -----------------------------");
            channel.exchangeDeclare(exchangeName, exchangeType);
            System.out.println("QUEUE BIND -----------------------------");
            channel.queueBind(queueName, exchangeName, key);
            System.out.println("QUEUE BINDED -----------------------------");
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}