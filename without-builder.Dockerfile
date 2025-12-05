FROM quay.io/keycloak/keycloak:26.4.5
WORKDIR /opt/keycloak

# Copy the custom provider jar
COPY --chown=keycloak:keycloak ./target/keycloak-event-spi.jar providers/

# Build Keycloak with custom providers
RUN /opt/keycloak/bin/kc.sh build --verbose

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]