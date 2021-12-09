package org.eclipse.dataspaceconnector.cosmos;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;

import java.util.List;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;

public class CosmosClientFactory {

    // the cosmos emulator uses a well-known fixed key:
    private static final String COSMOS_EMULATOR_MASTERKEY = "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==";
    private static final String DEFAULT_TRUSTSTORE_PASSWORD = "test123";
    private static final String DEFAULT_EMULATOR_HOST = "https://localhost:8081";
    private static final String TRUSTSTORE_PATH_PROPERTY = "edc.cosmos.emulator.truststore";
    private static final String EMULATOR_HOST_PROPERTY = "edc.cosmos.emulator.host";
    private static final String TRUSTSTORE_PASSWORD_PROPERTY = "edc.cosmos.emulator.truststore.password";

    public static CosmosClient createDefaultEmulatorClient() {

        // set the truststore password
        var pwd = propOrEnv(TRUSTSTORE_PASSWORD_PROPERTY, DEFAULT_TRUSTSTORE_PASSWORD);
        System.setProperty("javax.net.ssl.trustStorePassword", pwd);

        // set the path to the truststore
        var trustStorePath = propOrEnv(TRUSTSTORE_PATH_PROPERTY, null);
        Objects.requireNonNull(trustStorePath, TRUSTSTORE_PATH_PROPERTY + " cannot be empty, please point it toward a valid Java TrustStore!");
        System.setProperty("javax.net.ssl.trustStore", trustStorePath);

        // update the host if desired. in docker networks that might not be localhost.
        var host = propOrEnv(EMULATOR_HOST_PROPERTY, DEFAULT_EMULATOR_HOST);
        Objects.requireNonNull(host, EMULATOR_HOST_PROPERTY + " property cannot be empty!");

        return new CosmosClientBuilder()
                .endpoint(host)
                .key(COSMOS_EMULATOR_MASTERKEY)
                .preferredRegions(List.of("westeurope"))
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .buildClient();

    }

}
