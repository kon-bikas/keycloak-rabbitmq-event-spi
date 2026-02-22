# Custom Keycloak Event SPI
This is a custon SPI for extending keycloak functionality by publishing all user create, update, delete events from the user or the admin to a rabbitmq exchange in order for our subscribed app to catch the event and make the necessary changes in the app's local database.

keycloak uses the custom SPI by placing the project's jar under the keycloak's `/opt/keycloak/providers` path.

# 🚀 Usage
## 🛠️ Bare Metal of VMs
**1. Connect to host running keycloak**
```bash
ssh -i /path/to/private/ssh/key user@keycloak-ip
```
**2. Clone the repository**
```bash
git clone https://github.com/kon-bikas/keycloak-rabbitmq-event-spi.git
```
**3. Package the app using mnvw**
```bash
./mvnw clean package -Dmaven.test.skip
```
**4. Move jar to keycloak/providers**
```bash
mv ./target/keycloak-event-spi.jar /path/to/keycloak/providers
```
**5. Make sure that SPI is configures to talk to RabbitMQ**
add the following to your keycloak.conf file:
```keycloak.conf
spi-events-listener-test-listener-enabled=true
spi-events-listener-test-listener-rabbit-host={{ broker_host }}
spi-events-listener-test-listener-rabbit-port={{ broker.port }}
spi-events-listener-test-listener-rabbit-username={{ broker.admin.username }}
spi-events-listener-test-listener-rabbit-password={{ broker.admin.password }}
spi-events-listener-test-listener-rabbit-exchange={{ broker.exchange }}
spi-events-listener-test-listener-rabbit-virtual-host={{ broker.virtual_host }}
```
Give the values for your rabbitmq host 

**6. Build using kc.sh**
```bash
/path/to/keycloak/bin/kc.sh build
```
**7. Start the keycloak server**
```bash
# start in dev mode
/path/to/keycloak/bin/kc.sh start-dev
```

## 🐳 Docker and Kubernetes
### Docker
**Build Image using with-builder.Dockerfile or without-builder.Dockerfile**
`with-builder.Dockerfile`: You do not have to package the application, it takes care of that
`without-builder.Dockerfile`: You have to provide the jar project file by having it under `target/` with name `keycloak-event-spi.jar`

**❗ Note:** You have to configure the rabbitmq values for keycloak to be able to communicate with RabbitMQ.
You can do that by passing the environmental variables that are specified in step 5 of bare metal section.
Do not forget to change the name to env version, e.g. `KC_SPI_EVENTS_LISTENER_TEST_LISTENER_ENABLED`
```bash
docker build -t keycloak-event-spi:latest -f with-builder.Dockerfile
docker run --rm -d --name=kc-event -p 8080:8080 -e .... keycloak-event-spi:latest 
```
**⚠️ CAUTION:** If you intend to use this project in a CI/CD server pipeline, avoid using `without-builder.Dockerfile`. The target/ directory is inside .gitignore, so if you try and pass it to the Dockerfile you will have an error. You can first package it but `with-builder.Dockerfile` is a safer option.

### Docker Compose
You can use it in a docker compose, example:
```yaml
keycloak:
    image: keycloak-event:latest
    build:
        context: .
        dockerfile: with-builder.Dockerfile
    command: ["start-dev"]
    ports:
        -   "8080:8080"
    container_name: kc-event
    environment:
        KC_BOOTSTRAP_ADMIN_USERNAME: admin
        KC_BOOTSTRAP_ADMIN_PASSWORD: admin
        KC_SPI_EVENTS_LISTENER_TEST_LISTENER_ENABLED: true
        KC_SPI_EVENTS_LISTENER_TEST_LISTENER_RABBIT_HOST: rabbitmq
        KC_SPI_EVENTS_LISTENER_TEST_LISTENER_RABBIT_PORT: 5672
        KC_SPI_EVENTS_LISTENER_TEST_LISTENER_RABBIT_USERNAME: admin
        KC_SPI_EVENTS_LISTENER_TEST_LISTENER_RABBIT_PASSWORD: admin
        KC_SPI_EVENTS_LISTENER_TEST_LISTENER_RABBIT_EXCHANGE: app.exchange
        KC_SPI_EVENTS_LISTENER_TEST_LISTENER_RABBIT_VIRTUAL_HOST: service
```

### Kubernetes 
If you want to use this project with kubernetes, you have to have the image in a public container registry.

You can build an image and push it to a container registry or use `ghcr.io/kon-bikas/keycloak-event-spi:latest` 

And use it in your keycloak deployment inside your kubernetes cluster:
```yaml
spec:
    template:
        spec:
            containers:
                - name: kc-container
                  image: ghcr.io/kon-bikas/keycloak-event-spi:latest
```

