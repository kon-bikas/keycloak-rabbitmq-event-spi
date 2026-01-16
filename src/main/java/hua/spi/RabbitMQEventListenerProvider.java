package hua.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import hua.spi.dto.EventResponse;
import hua.spi.utils.AdminEventTransaction;
import hua.spi.utils.UserEventTransaction;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RabbitMQEventListenerProvider implements EventListenerProvider {

    private static final Logger logger = Logger.getLogger(RabbitMQEventListenerProviderFactory.class);

    private final String exchangeName;

    private final KeycloakSession session;

    private final RabbitMQChannelPool channelPool;

    private final EventListenerTransaction keycloakTransaction = new EventListenerTransaction(
                                                 this::publishAdminEvent, this::publishUserEvent
                                            );

    public RabbitMQEventListenerProvider(KeycloakSession session,
                                         String exchangeName, RabbitMQChannelPool channelPool) {
        this.session = session;
        this.exchangeName = exchangeName;
        this.channelPool = channelPool;

        /*
         * This way, we are storing the events until the keycloak DB transaction (e.g. to store a new user)
         * is completed successfully. That way we ensure that the events published and consumed by our app
         * are about new users that actually exist in keycloak (we have sync) and the process did not fail.
         */
        session.getTransactionManager().enlistAfterCompletion(keycloakTransaction);
    }

    @Override
    public void onEvent(Event event) {
        if (EventType.REGISTER.equals(event.getType())
                || EventType.UPDATE_PROFILE.equals(event.getType())) {
            keycloakTransaction.addEvent(
                    new UserEventTransaction(event, this.session)
            );
        }
    }

    /*
     * pass inside the transaction a AdminEvent of type AdminEventTransaction so that we are able to
     * also create the EventResponse in that transaction before it completes.
     */
    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        if (ResourceType.USER.equals(adminEvent.getResourceType())) {
            keycloakTransaction.addAdminEvent(
                    new AdminEventTransaction(adminEvent, this.session),
                    includeRepresentation
            );
        }
    }

    @Override
    public void close() {

    }

    private void publishUserEvent(Event event) {
        logger.info("User Event REGISTER/UPDATE took place!");

        UserEventTransaction userEventTransaction = (UserEventTransaction) event;
        this.publishEvent(userEventTransaction.getEventResponse());
    }

    private void publishAdminEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        logger.info("Admin Event CREATE/UPDATE took place!");

        /*
         * The transaction has an AdminEventTransaction so when the transaction is complete, it will
         * call this method with that object instance, so we can typecast the adminEvent object to
         * our custom AdminEventTransaction and get the EventResponse to publish.
         */
        AdminEventTransaction adminEventTransaction = (AdminEventTransaction) adminEvent;
        this.publishEvent(adminEventTransaction.getEventResponse());
    }

    private void publishEvent(EventResponse eventResponse) {

        Channel channel = this.channelPool.lendChannel();
        try {
            byte[] json = new ObjectMapper().writeValueAsBytes(eventResponse);
            channel.basicPublish(this.exchangeName, eventResponse.getRoutingKey(), null, json);
        } catch (IOException ex) {
            logger.error("Failed to publish event", ex);
        } finally {
            channelPool.releaseChannel(channel);
        }
    }

}
