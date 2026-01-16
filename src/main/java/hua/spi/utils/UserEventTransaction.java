package hua.spi.utils;

import hua.spi.dto.EventResponse;
import lombok.Getter;
import org.keycloak.events.Event;
import org.keycloak.models.KeycloakSession;

/*
 * This is a helper class that allows the Event that will be added to the EventListenerTransaction
 * to be also construct the EventResponse (that makes transactional calls) inside that transaction.
 * Otherwise, the EventResponse will fail to be constructed because the transaction will be completed
 * by the time we try to create it.
 */
@Getter
public class UserEventTransaction extends Event {

    private final EventResponse eventResponse;

    public UserEventTransaction(Event event, KeycloakSession session) {
        super();
        this.eventResponse = new EventResponse(event, session);
    }

}
