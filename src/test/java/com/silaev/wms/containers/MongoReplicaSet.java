package com.silaev.wms.containers;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.util.Arrays;

/**
 * Add `127.0.0.1 dockerhost` to the OS hosts
 */
@Slf4j
public abstract class MongoReplicaSet {
    private final static Network NETWORK = Network.newNetwork();

    /**
     * Move to host.docker.internal once https://github.com/docker/for-linux/issues/264 is resolved
     */
    @Container
    private final static GenericContainer DOCKER_HOST = new GenericContainer<>("qoomon/docker-host:2.3.0")
            .withPrivilegedMode(true)
            .withNetwork(NETWORK)
            .withNetworkAliases("dockerhost");

    @Container
    private final static GenericContainer MONGO_2 = new GenericContainer<>("s256/wms-mongo:4.0.10")
            .withNetwork(NETWORK)
            .withExposedPorts(27017)
            .withCommand("--replSet", "docker-rs")
            .waitingFor(
                    Wait.forLogMessage(".*waiting for connections on port.*", 1)
            );

    @Container
    private final static GenericContainer MONGO_3 = new GenericContainer<>("s256/wms-mongo:4.0.10")
            .withNetwork(NETWORK)
            .withExposedPorts(27017)
            .withCommand("--replSet", "docker-rs")
            .waitingFor(
                    Wait.forLogMessage(".*waiting for connections on port.*", 1)
            );

    @Container
    private final static GenericContainer MONGO_1 = new GenericContainer<>("s256/wms-mongo:4.0.10")
            .withNetwork(NETWORK)
            .dependsOn(Arrays.asList(MONGO_2, MONGO_3))
            .withExposedPorts(27017)
            .withCommand("--replSet", "docker-rs")
            .waitingFor(
                    Wait.forLogMessage(".*waiting for connections on port.*", 1)
            );
    private static final int AWAIT_MASTER_NODE_ATTEMPTS = 29;

    protected static String MONGO_URL_1;
    protected static String MONGO_URL_2;
    protected static String MONGO_URL_3;

    @NotNull
    private static String getMongoRsInitString() {
        return String.format(
                "rs.initiate({\n" +
                        "    \"_id\": \"docker-rs\",\n" +
                        "    \"members\": [\n" +
                        "        {\"_id\": 0, \"host\": \"%s\"},\n" +
                        "        {\"_id\": 1, \"host\": \"%s\"},\n" +
                        "        {\"_id\": 2, \"host\": \"%s\"}\n" +
                        "    ]\n" +
                        "});",
                MONGO_URL_1, MONGO_URL_2, MONGO_URL_3
        );
    }

    @BeforeAll
    public static void beforeAll() throws IOException, InterruptedException {
        val dockerHostName = "dockerhost";

        MONGO_URL_1 = String.format("%s:%d", dockerHostName, MONGO_1.getMappedPort(27017));
        MONGO_URL_2 = String.format("%s:%d", dockerHostName, MONGO_2.getMappedPort(27017));
        MONGO_URL_3 = String.format("%s:%d", dockerHostName, MONGO_3.getMappedPort(27017));

        log.debug(
                MONGO_1.execInContainer(
                        "mongo", "--eval", getMongoRsInitString()
                ).getStdout()
        );

        log.debug("Awaiting mongo1 to be a master node for {} attempts", AWAIT_MASTER_NODE_ATTEMPTS);
        val execResultWaitForMaster = MONGO_1.execInContainer(
                "mongo", "--eval",
                String.format(
                        "var attempt = 0; " +
                                "while" +
                                "(db.runCommand( { isMaster: 1 } ).ismaster==false) " +
                                "{ " +
                                "if (attempt > %d) {quit(1);} " +
                                "print('awaiting mongo1 to be a master node ' + attempt); sleep(1000);  attempt++; " +
                                " }", AWAIT_MASTER_NODE_ATTEMPTS
                )
        );
        log.debug(
                execResultWaitForMaster.getStdout()
        );

        if (execResultWaitForMaster.getExitCode() == 1) {
            val errorMessage = String.format(
                    "The master node was not initialized in a set timeout: %d attempts",
                    AWAIT_MASTER_NODE_ATTEMPTS
            );
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        log.debug(
                MONGO_1.execInContainer("mongo", "--eval", "rs.status()").getStdout()
        );

    }
}
