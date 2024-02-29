package org.kendar.amqp.v09.exchange;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.concurrent.ConcurrentHashMap;

public class Square {
    public static ConcurrentHashMap<String, String> results = new ConcurrentHashMap<>();
    private static String QUEUE_NAME = "square";
    private static String EXCHANGE_NAME = "myExchange";
    private static String KEY_NAME = "key";
    DeliverCallback findSquare = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        int number = Integer.parseInt(message);
        int squareNumber = number * number;
        results.put("Square of " + message + " is: " + squareNumber, "");
    };

    public void listenToMessage(ConnectionFactory connectionFactory) {
        Queue queue = new Queue(connectionFactory);
        queue.createExchangeQueue(QUEUE_NAME, EXCHANGE_NAME, "direct", KEY_NAME);
        queue.listenToQueue(QUEUE_NAME, findSquare);
    }
}
