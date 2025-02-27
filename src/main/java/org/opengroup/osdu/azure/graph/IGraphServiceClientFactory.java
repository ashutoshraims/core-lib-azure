package org.opengroup.osdu.azure.graph;

import com.microsoft.graph.serviceclient.GraphServiceClient;


/**
 *  Interface for Graph Service Client Factory to return appropriate graph service client.
 *  instances for each tenant based on data partition id
 */
public interface IGraphServiceClientFactory {
    /**
     * getGraphServiceClient.
     * @param dataPartitionId data partition id
     * @return Graph service client
     */
    GraphServiceClient getGraphServiceClient(String dataPartitionId);
}
