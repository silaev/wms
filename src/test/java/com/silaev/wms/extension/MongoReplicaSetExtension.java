package com.silaev.wms.extension;

import com.silaev.wms.extension.model.PropertyContainer;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Add `127.0.0.1 dockerhost` to the OS hosts if useHostWorkaround=true
 * or  `127.0.0.1 host.docker.internal` if useHostWorkaround=false
 */
@Slf4j
public class MongoReplicaSetExtension implements BeforeAllCallback, ExecutionCondition {
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(MongoReplicaSetExtension.class);
    private static final String EXTENSION_NAME = "MongoReplicaSetExtension";

    private final Boolean useHostWorkaround;
    private final Integer replicaSetNumber;
    private final Integer awaitMasterNodeAttempts;
    private final String mongoDockerImageName;
    /**
     * 127.0.0.1 mongo1 mongo2 mongo3 if disabled and Composed used
     */
    @Getter
    private final Boolean isEnabled;
    @Getter
    private String mongoRsUrl;

    @Builder
    public MongoReplicaSetExtension(
            Boolean useHostWorkaround,
            Integer replicaSetNumber,
            Integer awaitMasterNodeAttempts,
            String fileNameToCheckIfEnabled,
            String mongoDockerImageName
    ) {
        this.useHostWorkaround = Optional.ofNullable(useHostWorkaround).orElse(Boolean.TRUE);
        this.replicaSetNumber = Optional.ofNullable(replicaSetNumber).orElse(3);
        this.awaitMasterNodeAttempts = Optional.ofNullable(awaitMasterNodeAttempts).orElse(29);
        this.isEnabled = fetchEnabled(
                Optional.ofNullable(fileNameToCheckIfEnabled).orElse("application-test.yml")
        );
        this.mongoDockerImageName = Optional.ofNullable(mongoDockerImageName).orElse("s256/wms-mongo:4.0.10");
    }

    /**
     * Move to host.docker.internal once https://github.com/docker/for-linux/issues/264 is resolved
     */
    private String getDockerHostName() {
        return useHostWorkaround ? "dockerhost" : "host.docker.internal";
    }

    /**
     * Finds a property mongoReplicaSetProperties.enabled in a provided
     * param fileNameToCheckIfEnabled.
     *
     * @param fileNameToCheckIfEnabled
     * @return
     */
    private boolean fetchEnabled(String fileNameToCheckIfEnabled) {
        val representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(
                new Constructor(PropertyContainer.class),
                representer
        );
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream(fileNameToCheckIfEnabled);
        return Optional.ofNullable(
                yaml.<PropertyContainer>load(inputStream)
                        .getMongoReplicaSetProperties()
                        .getEnabled()
        ).orElse(Boolean.TRUE);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (!isEnabled) {
            log.info("{} is disabled", EXTENSION_NAME);
            return;
        }

        val testClassName =
                context.getTestClass().orElseThrow(
                        () -> new ExtensionConfigurationException(
                                String.format("%s is only supported for classes.", EXTENSION_NAME)
                        )
                ).getName();
        val dockerHostName = getDockerHostName();
        val network = Network.newNetwork();
        val store = context.getStore(NAMESPACE);

        if (useHostWorkaround) {
            val dockerHostAdapter = new StoreAdapter(
                    testClassName + "." + dockerHostName,
                    getDockerHostContainer(network, dockerHostName)
            );
            store.getOrComputeIfAbsent(dockerHostAdapter.getKey(), k -> dockerHostAdapter.start());
        }

        val hosts = new String[replicaSetNumber];
        GenericContainer mongoContainer = null;
        for (int i = replicaSetNumber - 1; i >= 0; i--) {
            mongoContainer = getMongoContainer(network);
            val mongoAdapter = new StoreAdapter(testClassName + ".mongo" + i, mongoContainer);
            store.getOrComputeIfAbsent(mongoAdapter.getKey(), k -> mongoAdapter.start());
            hosts[replicaSetNumber - i - 1] = String.format("%s:%d", dockerHostName, mongoContainer.getMappedPort(27017));
        }
        assertNotNull(mongoContainer);

        mongoRsUrl = buildMongoRsUrl(hosts);

        log.debug(
                mongoContainer.execInContainer(
                        "mongo", "--eval", getMongoReplicaSetInitializer(hosts)
                ).getStdout()
        );
        log.debug("Awaiting mongo0 to be a master node for {} attempts", awaitMasterNodeAttempts);
        Container.ExecResult execResultWaitForMaster = waitForMongoMasterNodeInit(mongoContainer);
        log.debug(execResultWaitForMaster.getStdout());

        checkMongoMasterNodeInitResult(execResultWaitForMaster);

        log.debug(
                mongoContainer.execInContainer("mongo", "--eval", "rs.status()").getStdout()
        );

    }

    private void checkMongoMasterNodeInitResult(Container.ExecResult execResultWaitForMaster) {
        if (execResultWaitForMaster.getExitCode() == 1) {
            val errorMessage = String.format(
                    "The master node was not initialized in a set timeout: %d attempts",
                    awaitMasterNodeAttempts
            );
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }

    private Container.ExecResult waitForMongoMasterNodeInit(GenericContainer mongoContainer) throws java.io.IOException, InterruptedException {
        return mongoContainer.execInContainer(
                "mongo", "--eval",
                String.format(
                        "var attempt = 0; " +
                                "while" +
                                "(db.runCommand( { isMaster: 1 } ).ismaster==false) " +
                                "{ " +
                                "if (attempt > %d) {quit(1);} " +
                                "print('awaiting mongo0 to be a master node ' + attempt); sleep(1000);  attempt++; " +
                                " }", awaitMasterNodeAttempts
                )
        );
    }

    private String buildMongoRsUrl(String[] hosts) {
        return String.format(
                "spring.data.mongodb.uri: mongodb://%s/test?replicaSet=docker-rs",
                String.join(",", hosts)
        );
    }

    private GenericContainer getDockerHostContainer(Network network, String dockerHostName) {
        return new GenericContainer<>("qoomon/docker-host:2.3.0")
                .withPrivilegedMode(true)
                .withNetwork(network)
                .withNetworkAliases(dockerHostName);
    }

    private GenericContainer getMongoContainer(Network network) {
        return new GenericContainer<>(mongoDockerImageName)
                .withNetwork(network)
                .withExposedPorts(27017)
                .withCommand("--replSet", "docker-rs")
                .waitingFor(
                        Wait.forLogMessage(".*waiting for connections on port.*", 1)
                );
    }

    private String getMongoReplicaSetInitializer(String[] hosts) {
        val sb = new StringBuilder();
        sb.append(
                "rs.initiate({\n" +
                        "    \"_id\": \"docker-rs\",\n" +
                        "    \"members\": [\n"
        );
        for (int i = 0; i < hosts.length; i++) {
            sb.append(
                    String.format(
                            "        {\"_id\": %d, \"host\": \"%s\"}",
                            i, hosts[i]
                    )
            );
            if (i == hosts.length - 1) {
                sb.append("\n");
            } else {
                sb.append(",\n");
            }
        }
        sb.append("    ]\n});");

        return sb.toString();
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (!isEnabled) {
            log.info("{} is disabled", EXTENSION_NAME);
            return ConditionEvaluationResult.enabled("Check passed");
        }

        return (replicaSetNumber < 0) || (replicaSetNumber > 7)
                ? ConditionEvaluationResult.disabled(
                "Three-member replica sets provide the minimum recommended architecture for a replica set." +
                        "Replica set configuration should contain no more than 7 voting members"
        )
                : ConditionEvaluationResult.enabled("replicaSetNumber check passed");
    }

    /**
     * An adapter for {@link Startable} that implement {@link ExtensionContext.Store.CloseableResource}
     * thereby letting the JUnit automatically stop containers once the current
     * {@link ExtensionContext} is closed.
     */
    @Getter
    private static class StoreAdapter implements ExtensionContext.Store.CloseableResource {
        private final String key;
        private final Startable container;

        private StoreAdapter(String keyName, Startable container) {
            this.key = keyName;
            this.container = container;
        }

        private MongoReplicaSetExtension.StoreAdapter start() {
            container.start();
            return this;
        }

        @Override
        public void close() {
            container.stop();
        }
    }
}
