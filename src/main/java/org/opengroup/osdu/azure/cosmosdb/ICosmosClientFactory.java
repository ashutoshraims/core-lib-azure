package org.opengroup.osdu.azure.cosmosdb;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClient;

/**
 *  Interface for Cosmos Client Factory to return appropriate cosmos client.
 *  instances for each tenant based on data partition id
 */
public interface ICosmosClientFactory {

    /**
     * @param dataPartitionId Data Partition Id
     * @return Cosmos client instance
     */
    CosmosClient getClient(String dataPartitionId);

    /**
     * @param dataPartitionId Data Partition Id
     * @return Cosmos async client instance
     */
    CosmosAsyncClient getAsyncClient(String dataPartitionId);

    /**
     * @return Cosmos client instance for system resources.
     */
    CosmosClient getSystemClient();

}
