FROM eclipse-temurin:17.0.17_10-jdk-ubi9-minimal AS builder
WORKDIR /opt/app

COPY .mvn/ .mvn
COPY mvnw ./
COPY pom.xml ./
COPY ./src ./src

RUN ./mvnw package -Dmaven.test.skip

FROM quay.io/keycloak/keycloak:26.4.5
WORKDIR /opt/keycloak

# Copy the custom provider jar
COPY --from=builder --chown=keycloak:keycloak /opt/app/target/keycloak-event-spi.jar providers/

# Build Keycloak with custom providers
RUN /opt/keycloak/bin/kc.sh build --verbose

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]