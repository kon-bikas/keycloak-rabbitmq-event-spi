# Custom Keycloak Event SPI
This is a custon SPI for extending keycloak functionality by publishing all user create, update, delete events from the user or the admin to a rabbitmq exchange in order for our subscribed app to catch the event and make the necessary changes in the app's local database.

keycloak uses the custom SPI by placing the project's jar under the keycloak's container (docker containers used) /opt/keycloak/providers path. 

### Run the project
Make sure that you have docker and docker-compose installed and then type: ```docker compose up``` from project's root directory.