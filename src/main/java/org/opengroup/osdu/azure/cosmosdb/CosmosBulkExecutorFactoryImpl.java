package org.opengroup.osdu.azure.cosmosdb;

import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.ConsistencyLevel;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.DocumentClientException;
import com.microsoft.azure.documentdb.DocumentCollection;
import com.microsoft.azure.documentdb.bulkexecutor.DocumentBulkExecutor;
import org.opengroup.osdu.azure.cache.CosmosBulkExecutorCache;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.common.Validators;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * A factory class to generate DocumentBulkExecutor objects to perform bulk operations.
 */
@Component
@Lazy
public class CosmosBulkExecutorFactoryImpl implements ICosmosBulkExecutorFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CosmosBulkExecutorFactoryImpl.class.getName());

    @Lazy
    @Autowired
    private PartitionServiceClient partitionService;

    @Lazy
    @Autowired
    private CosmosBulkExecutorCache cosmosBulkExecutorCache;

    @Autowired
    private int documentClientMaxPoolSize;

    @Autowired
    private int bulkExecutorMaxRUs;

    private final String unformattedCollectionLink = "/dbs/%s/colls/%s";
    private final String unformattedCosmosBulkExecutorCacheKey = "%s-%s-%s-cosmosBulkExecutor";
    private final String unformattedDocumentClientCacheKey = "%s-documentClient";

    /**
     *
     * @param dataPartitionId name of data partition.
     * @param cosmosDBName name of cosmos db.
     * @param collectionName name of collection in cosmos.
     * @return DocumentBulkExecutor to perform bulk Cosmos opartions.
     * @throws Exception if there is an error creating the DocumentBulkExecutor object.
     */
    public DocumentBulkExecutor getClient(final String dataPartitionId,
                                          final String cosmosDBName,
                                          final String collectionName) {
        Validators.checkNotNullAndNotEmpty(dataPartitionId, "dataPartitionId");
        Validators.checkNotNullAndNotEmpty(cosmosDBName, "cosmosDBName");
        Validators.checkNotNullAndNotEmpty(collectionName, "collectionName");

        String cacheKey = String.format(unformattedCosmosBulkExecutorCacheKey, dataPartitionId, cosmosDBName, collectionName);
        if (this.cosmosBulkExecutorCache.containsKey(cacheKey)) {
            return this.cosmosBulkExecutorCache.get(cacheKey);
        }

        PartitionInfoAzure pi = this.partitionService.getPartition(dataPartitionId);

        DocumentClient client = getDocumentClient(pi.getCosmosEndpoint(),
                pi.getCosmosPrimaryKey());

        String collectionLink = String.format(unformattedCollectionLink, cosmosDBName, collectionName);
        try {

            DocumentCollection collection = client.readCollection(collectionLink, null).getResource();
            DocumentBulkExecutor executor = DocumentBulkExecutor.builder().from(
                    client,
                    cosmosDBName,
                    collectionName,
                    collection.getPartitionKey(),
                    bulkExecutorMaxRUs
            ).build();
            cosmosBulkExecutorCache.put(String.format(unformattedCosmosBulkExecutorCacheKey, dataPartitionId, cosmosDBName, collectionName), executor);

            // Set client retry options to 0 because retries are handled by DocumentBulkExecutor class.
            client.getConnectionPolicy().getRetryOptions().setMaxRetryAttemptsOnThrottledRequests(0);
            client.getConnectionPolicy().getRetryOptions().setMaxRetryWaitTimeInSeconds(0);

            return executor;
        } catch (DocumentClientException e) {
            String errorMessage = "Unexpectedly failed to create DocumentCollection object";
            LOGGER.warn(errorMessage, e);
            throw new AppException(500, errorMessage, e.getMessage(), e);
        } catch (Exception e) {
            String errorMessage = "Unexpectedly failed create DocumentBulkExecutor";
            LOGGER.warn(errorMessage, e);
            throw new AppException(500, errorMessage, e.getMessage(), e);
        }
    }

    /**
     *
     * @param cosmosEndpoint endpoint to Cosmos db.
     * @param cosmosPrimaryKey primary key for connection to Cosmos db.
     * @return DocumentClient object.
     */
    private DocumentClient getDocumentClient(final String cosmosEndpoint,
                                             final String cosmosPrimaryKey) {

        ConnectionPolicy policy = new ConnectionPolicy();
        policy.setMaxPoolSize(documentClientMaxPoolSize);

        // Initialize with these values for retries. These are overridden once the DocumentBulkExecutor object is created
        policy.getRetryOptions().setMaxRetryWaitTimeInSeconds(30);
        policy.getRetryOptions().setMaxRetryAttemptsOnThrottledRequests(9);

        DocumentClient client = new DocumentClient(
                cosmosEndpoint,
                cosmosPrimaryKey,
                policy,
                ConsistencyLevel.Session
        );

        return client;
    }


}
