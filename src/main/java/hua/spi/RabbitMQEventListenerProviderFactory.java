package hua.spi;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger logger = Logger.getLogger(RabbitMQEventListenerProviderFactory.class);

    private ConnectionFactory connectionFactory;

    private RabbitMQChannelPool channelPool;

    private Connection connection;

    private String exchangeName;

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {
        logger.info("RabbitMQEventListenerProviderFactory created");
        return new RabbitMQEventListenerProvider(keycloakSession, exchangeName, channelPool);
    }

    @Override
    public void init(Config.Scope scope) {
        this.connectionFactory = new ConnectionFactory();
        this.connectionFactory.setHost(scope.get("rabbit.host"));
        this.connectionFactory.setPort(Integer.parseInt(scope.get("rabbit.port")));
        this.connectionFactory.setUsername(scope.get("rabbit.username"));
        this.connectionFactory.setPassword(scope.get("rabbit.password"));
        this.connectionFactory.setVirtualHost(scope.get("rabbit.virtual.host"));

        /*
         * get the name exchange from the keycloak configuration.
         */
        this.exchangeName = scope.get("rabbit.exchange");

        /*
         * Initiate a TCP connection with rabbitmq node. We only need to create one long-lived
         * connection that creates multiple channels per request/thread. Connection is thread safe (we can
         * share a single connection across multiple threads).
         */
        this.establishConnection();

        /*
         * Create a channel pool using the Connection instance we created
         */
        if (scope.getInt("rabbit.pool.size") != null) {
            this.channelPool = new RabbitMQChannelPool(connection, scope.getInt("rabbit.pool.size"));
        } else {
            this.channelPool = new RabbitMQChannelPool(connection);
        }

        logger.info("Rabbitmq event listener provider factory initialized");
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {
        try {
            channelPool.clearWorkerQueue();
            connection.close();
            logger.info("Event listener provider factory closed. rabbitmq connection closed.");
        } catch (IOException | TimeoutException ex) {
            logger.error("Event listener provider factory close failed.");
        }
    }

    @Override
    public String getId() {
        return "test-listener";
    }

    private void establishConnection() {
        try {
            this.connection = connectionFactory.newConnection();
        } catch (IOException | TimeoutException ex) {
            logger.error("Error on rabbitmq connection establishment!");
        }

    }

}
