package com.bwagih.notification.learn;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import java.util.Arrays;
import java.util.List;

public class MainApplication {

    public static void main(String[] args) {
        // Instantiate RabbitMQConfig
        RabbitMQConfig rabbitMQConfig = new RabbitMQConfig();

        // Declare exchange
        rabbitMQConfig.declareExchange("myExchange");

        // Declare queue
        rabbitMQConfig.declareQueue("myQueue");

        // Bind exchange and queue
        rabbitMQConfig.bind("myExchange", "myQueue", "myRoutingKey");


        // Send a message
        rabbitMQConfig.sendMessage("myExchange", "myRoutingKey", "Hello RabbitMQ!");

        // Send a list as an event
        List<String> myList = Arrays.asList("item1", "item2", "item3");
        rabbitMQConfig.sendMessage("myExchange", "myRoutingKey", myList);

        // Send an object as an event
        Object[][] myObject = {{"test1", 30}, {"test2", 27}};
        rabbitMQConfig.sendMessage("myExchange", "myRoutingKey", myObject);


//        // Receive a message
//        String receivedMessage = rabbitMQConfig.receiveMessage("myQueue");
//        System.out.println("Received message: " + receivedMessage);

        // Start listening for messages
        rabbitMQConfig.startListening();

        // To stop listening, you can call rabbitMQConfig.stopListening();


    }
}
