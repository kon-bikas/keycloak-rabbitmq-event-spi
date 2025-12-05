package hua.spi.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

import java.util.List;
import java.util.Map;

/**
 * This is the class that will be serialized into json and then publish to the rabbitmq exchange.
 **/
@Getter @Setter
public class EventResponse {

    /*
     * Operation made to User, if admin action: CREATE, UPDATE, DELETE.
     * If client/user action then we care about REGISTER and UPDATE_PROFILE.
     */
    private String eventType;

    /*
     * Capture the user's keycloak unique identifier .
     */
    private String userUUID;

    /*
     * All keycloak user attributes (username, email, First name e.t.c.) in a Map.
     */
    private Map<String, List<String>> attributes;

    /*
     * This will contain the routing key value attached to the event publish, that we also
     * do not want to include in the message body (is a helper attribute).
     */
    @JsonIgnore
    private String routingKey;
    /*
     * If this constructor is called then we know that we have to do with an admin event,
     * but we also know that the resource type of the event is USER.
     */
    public EventResponse(AdminEvent adminEvent, KeycloakSession session) {
        this.eventType = adminEvent.getOperationType().toString();
        /*
         * when the resource is USER then the resource path has a structure of: users/<user-uuid>,
         * so we take the substring starting from the 6th position (0-indexed) to the end
         */
        this.userUUID = adminEvent.getResourcePath().substring(6);

        /*
         * We prefer getting all the information our apps want here to take advantage of session and not
         * add unnecessary network overhead.
         */
        this.attributes = session
                .users()
                .getUserById(
                        session.getContext().getRealm(),
                        this.userUUID
                )
                .getAttributes();

        /*
         * Routing key structure: <Realm-id>.admin.<Operation-type>
         */
        this.routingKey = adminEvent.getRealmId() + ".admin." + this.eventType;

    }

    /*
     * If this constructor was called we know that we have to do with a client/user event and that the
     * action was either register or profile update.
     */
    public EventResponse(Event event, KeycloakSession session) {
        this.eventType = event.getType().toString();

        this.userUUID = event.getUserId();

        /*
         * We prefer getting all the information our apps want here to take advantage of session and not
         * add unnecessary network overhead.
         */
        this.attributes = session
                .users()
                .getUserById(
                        session.getContext().getRealm(),
                        this.userUUID
                )
                .getAttributes();

        /*
         * Routing key structure: <Realm-id>.client.<Resource-type>
         */
        this.routingKey = event.getRealmId() + ".client." + this.eventType;

    }


}
