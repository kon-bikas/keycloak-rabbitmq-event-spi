package hua.spi.utils;

import hua.spi.dto.EventResponse;
import lombok.Getter;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

/*
 * This is a helper class that allows the adminEvent that will be added to the EventListenerTransaction
 * to also construct the EventResponse (that makes transactional calls) inside that transaction.
 * Otherwise, the EventResponse will fail to be constructed because the transaction will be completed
 * by the time we try to create it.
 */
@Getter
public class AdminEventTransaction extends AdminEvent {

    private final EventResponse eventResponse;

    public AdminEventTransaction(AdminEvent adminEvent, KeycloakSession session) {
        super(adminEvent);
        this.eventResponse = new EventResponse(adminEvent, session);
    }

}
