package hua.spi;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class TestingClassTest {

    @Container
    public RabbitMQContainer rabbitmq = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4.2-management")
    )
            .withExposedPorts(5672)
            .withAdminUser("admin")
            .withAdminPassword("admin");

    @Container
    public KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.4.5")
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            .dependsOn(rabbitmq)
            .withRealmImportFile("realm-import.json");
//            .withCopyFileToContainer(
//                    MountableFile.forHostPath("./target/keycloak-event-spi.jar"),
//                    "/opt/keycloak/providers/keycloak-event-spi.jar"
//            );

    @Test
    void testMethod() {
        assertTrue(rabbitmq.isRunning());
        assertTrue(keycloak.isRunning());
    }

}
