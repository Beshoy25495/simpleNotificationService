package com.bwagih.notification.learn;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import com.rabbitmq.client.Channel;
import org.springframework.web.client.RestTemplate;

public class RabbitMQConfig {

    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final SimpleMessageListenerContainer messageListenerContainer;
    private final ConnectionFactory factory;
    private final String managementBaseUrl = "http://localhost:15672/api/";
    private final String managementUsername = "guest";  // RabbitMQ management username
    private final String managementPassword = "guest";  // RabbitMQ management password


    public RabbitMQConfig() {
        this.factory = rabbitConnectionFactory();
        createOrUpdateRabbitMQResources();

        // Initialize RabbitAdmin
        this.amqpAdmin = new RabbitAdmin(factory);

        // Initialize RabbitTemplate
        this.rabbitTemplate = new RabbitTemplate(factory);
        // Configure message converter
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        rabbitTemplate.setMessageConverter(converter);

        // Initialize SimpleMessageListenerContainer
        this.messageListenerContainer = new SimpleMessageListenerContainer();
        this.messageListenerContainer.setConnectionFactory(factory);
        this.messageListenerContainer.setQueueNames("myQueue"); // Specify the queue to listen to
        this.messageListenerContainer.setMessageListener(new MyMessageListener()); // Set the message listener
    }


    public void createOrUpdateRabbitMQResources() {
        // Create or update virtual hosts and users based on user input or configuration
        this.createVirtualHosts("/my_vhost");
        this.addUsersWithPermissions("configure,write,read", "my_user", "my_password", "/my_vhost");
    }

    // Method to create virtual hosts using RabbitMQ management API
    //http://localhost:15672/api/vhosts/
    public void createVirtualHosts(String virtualHost) {
        String url = managementBaseUrl + "vhosts/" + virtualHost;

        // Set up HTTP basic authentication headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(managementUsername, managementPassword);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Make the HTTP PUT request
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);
        // Handle the response as needed
    }

    // Method to add users with specific permissions using RabbitMQ management API
    public void addUsersWithPermissions(String permissions, String username, String password, String virtualHost) {
        String url = managementBaseUrl + "users/" + username;

        // Construct the payload for adding user
        String payload = String.format("{\"password\":\"%s\",\"tags\":\"\",\"vhost\":\"%s\"}", password, virtualHost);

        // Set up HTTP basic authentication headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(managementUsername, managementPassword);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(payload, headers);

        // Make the HTTP PUT request to add the user
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);
        // Handle the response as needed

        // Set permissions for the user
        if (response.getStatusCode() == HttpStatus.CREATED) {
            setPermissions(username, virtualHost, permissions);
        }
    }

    // Method to set permissions for a user on a virtual host
    private void setPermissions(String username, String virtualHost, String permissions) {
        String url = managementBaseUrl + "permissions/" + virtualHost + "/" + username;

        // Construct the payload for setting permissions
        String payload = String.format("{\"configure\":\"%s\",\"write\":\"%s\",\"read\":\"%s\"}", permissions, permissions, permissions);

        // Set up HTTP basic authentication headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(managementUsername, managementPassword);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(payload, headers);

        // Make the HTTP PUT request to set permissions
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);
        // Handle the response as needed
    }

//    // Method to create virtual hosts and add users with specific permissions
//    private void createVirtualHostsAndUsers() throws Exception {
//        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
//            // Create virtual hosts
//            channel.exchangeDeclare("my_exchange", "direct", true);
//            channel.queueDeclare("my_queue", true, false, false, null);
//            channel.queueBind("my_queue", "my_exchange", "my_routing_key");
//
//            // Add user
//            channel.basicPublish("", "AddUser", null, "username:password".getBytes());
//
//            // Set permissions
//            channel.basicPublish("", "SetPermissions", null, "username:vhost:configure,write,read".getBytes());
//        }
//    }

    // Method to create RabbitMQ connection factory
    public static ConnectionFactory rabbitConnectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost("localhost"); // Set RabbitMQ host
        connectionFactory.setUsername("guest"); // Set RabbitMQ username
        connectionFactory.setPassword("guest"); // Set RabbitMQ password
        connectionFactory.setVirtualHost("/");  // Set virtual host if necessary
        return connectionFactory;
    }

    // Method to declare exchange
    public void declareExchange(String exchangeName) {
        Exchange exchange = new DirectExchange(exchangeName);
        amqpAdmin.declareExchange(exchange);
    }

    // Method to declare queue
    public void declareQueue(String queueName) {
        Queue queue = new Queue(queueName, true); // durable queue
        amqpAdmin.declareQueue(queue);
    }

    // Method to bind exchange and queue
    public void bind(String exchangeName, String queueName, String routingKey) {
        amqpAdmin.declareBinding(BindingBuilder.bind(new Queue(queueName)).to(new DirectExchange(exchangeName)).with(routingKey));
    }

    // Method to send message
    public void sendMessage(String exchangeName, String routingKey, Object message) {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
    }

    // Start message listener container
    public void startListening() {
        this.messageListenerContainer.start();
    }

    // Stop message listener container
    public void stopListening() {
        this.messageListenerContainer.stop();
    }

    // Custom message listener
    private static class MyMessageListener implements ChannelAwareMessageListener {
        @Override
        public void onMessage(Message message, Channel channel) throws Exception {
            String receivedMessage = new String(message.getBody());
            System.out.println("Received message: " + receivedMessage);
            // Acknowledge the message
            assert channel != null;
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }
}
