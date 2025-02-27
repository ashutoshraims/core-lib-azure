package org.opengroup.osdu.azure.cosmosdb;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;

import com.azure.cosmos.ThrottlingRetryOptions;
import com.azure.identity.DefaultAzureCredential;
import com.azure.security.keyvault.secrets.SecretClient;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.azure.cosmosdb.system.config.SystemCosmosConfig;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.di.CosmosRetryConfiguration;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.common.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Implementation for ICosmosClientFactory.
 */
@Component
@Lazy
public class CosmosClientFactoryImpl implements ICosmosClientFactory {
    private static final String LOGGER_NAME = CosmosClientFactoryImpl.class.getName();
    private static final String SYSTEM_COSMOS_CACHE_KEY = "system_cosmos";
    private static final String DATA_PARTITION_ID = "dataPartitionId";

    @Lazy
    @Autowired
    private PartitionServiceClient partitionService;

    @Autowired
    private SecretClient secretClient;

    @Autowired
    private SystemCosmosConfig systemCosmosConfig;

    private Map<String, CosmosClient> cosmosClientMap;
    private Map<String, CosmosAsyncClient> cosmosAsyncClientMap;

    @Autowired
    private CosmosRetryConfiguration cosmosRetryConfiguration;

    @Autowired
    private MSIConfiguration msiConfiguration;

    @Autowired
    private DefaultAzureCredential defaultAzureCredential;

    /**
     * Initializes the private variables as required.
     */
    @PostConstruct
    public void initialize() {
        cosmosClientMap = new ConcurrentHashMap<>();
        cosmosAsyncClientMap = new ConcurrentHashMap<>();
    }

    /**
     * @param dataPartitionId Data Partition Id
     * @return Cosmos Client instance
     */
    @Override
    public CosmosClient getClient(final String dataPartitionId) {
        Validators.checkNotNullAndNotEmpty(dataPartitionId, DATA_PARTITION_ID);
        String cacheKey = String.format("%s-cosmosClient", dataPartitionId);

        return this.cosmosClientMap.computeIfAbsent(cacheKey, cosmosClient -> createCosmosClient(dataPartitionId));
    }

    /**
     * @param dataPartitionId Data Partition Id
     * @return Cosmos Async Client instance
     */
    @Override
    public CosmosAsyncClient getAsyncClient(final String dataPartitionId) {
        Validators.checkNotNullAndNotEmpty(dataPartitionId, DATA_PARTITION_ID);
        String cacheKey = String.format("%s-cosmosAsyncClient", dataPartitionId);

        return this.cosmosAsyncClientMap.computeIfAbsent(cacheKey, cosmosClient -> createCosmosAsyncClient(dataPartitionId));
    }

    /**
     * @return Cosmos client instance for system resources.
     */
    @Override
    public CosmosClient getSystemClient() {

        return this.cosmosClientMap.computeIfAbsent(
                SYSTEM_COSMOS_CACHE_KEY, cosmosClient -> createSystemCosmosClient()
        );
    }

    /**
     *
     * @param dataPartitionId Data Partition Id
     * @return Cosmos Client Instance
     */
    private CosmosClient createCosmosClient(final String dataPartitionId) {
        PartitionInfoAzure pi = this.partitionService.getPartition(dataPartitionId);

        ThrottlingRetryOptions throttlingRetryOptions = cosmosRetryConfiguration.getThrottlingRetryOptions();
        CosmosClient cosmosClient;

        if (msiConfiguration.getIsEnabled()) {
            cosmosClient = new CosmosClientBuilder()
                    .endpoint(pi.getCosmosEndpoint())
                    .credential(defaultAzureCredential)
                    .throttlingRetryOptions(throttlingRetryOptions)
                    .buildClient();
        } else {
            cosmosClient = new CosmosClientBuilder()
                    .endpoint(pi.getCosmosEndpoint())
                    .key(pi.getCosmosPrimaryKey())
                    .throttlingRetryOptions(throttlingRetryOptions)
                    .buildClient();
        }

        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME)
                .debug("Created CosmosClient for dataPartition {}.", dataPartitionId);
        return cosmosClient;
    }

    /**
     *
     * @param dataPartitionId Data Partition Id
     * @return Cosmos Async Client Instance
     */
    private CosmosAsyncClient createCosmosAsyncClient(final String dataPartitionId) {
        PartitionInfoAzure pi = this.partitionService.getPartition(dataPartitionId);

        ThrottlingRetryOptions throttlingRetryOptions = cosmosRetryConfiguration.getThrottlingRetryOptions();
        CosmosAsyncClient cosmosAsyncClient;

        if (msiConfiguration.getIsEnabled()) {
            cosmosAsyncClient = new CosmosClientBuilder()
                    .endpoint(pi.getCosmosEndpoint())
                    .credential(defaultAzureCredential)
                    .throttlingRetryOptions(throttlingRetryOptions)
                    .buildAsyncClient();
        } else {
            cosmosAsyncClient = new CosmosClientBuilder()
                    .endpoint(pi.getCosmosEndpoint())
                    .key(pi.getCosmosPrimaryKey())
                    .throttlingRetryOptions(throttlingRetryOptions)
                    .buildAsyncClient();
        }

        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME)
                .debug("Created CosmosAsyncClient for dataPartition {}.", dataPartitionId);
        return cosmosAsyncClient;
    }

    /**
     * Method to create the cosmos client for system resources.
     * @return cosmos client.
     */
    private CosmosClient createSystemCosmosClient() {

        CosmosClient cosmosClient;
        if (msiConfiguration.getIsEnabled()) {
            cosmosClient = new CosmosClientBuilder()
                    .endpoint(getSecret(systemCosmosConfig.getCosmosDBAccountKeyName()))
                    .credential(defaultAzureCredential)
                    .buildClient();
        } else {
            cosmosClient = new CosmosClientBuilder()
                    .endpoint(getSecret(systemCosmosConfig.getCosmosDBAccountKeyName()))
                    .key(getSecret(systemCosmosConfig.getCosmosPrimaryKeyName()))
                    .buildClient();
        }

        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME)
                .debug("Created CosmosClient for system resources");

        return cosmosClient;
    }

    /**
     * @param keyName Name of the key to be read from key vault.
     * @return secret value
     */
    private String getSecret(final String keyName) {
        return KeyVaultFacade.getSecretWithValidation(secretClient, keyName);
    }
}
