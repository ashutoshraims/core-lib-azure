package org.opengroup.osdu.azure.graph;


import com.azure.identity.DefaultAzureCredential;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.opengroup.osdu.common.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 *  Implementation for Graph Service Client Factory Interface to return appropriate cosmos client.
 *  Caches the client for each tenant based on data partition id
 */
@Component
@Lazy
public class GraphServiceClientFactory implements IGraphServiceClientFactory {
    private Map<String, GraphServiceClient> graphServiceClientMap;
    private static String cacheKey = "%s-graphServiceClient";
    private static String[] scope = {"https://graph.microsoft.com/.default"};
    @Autowired
    private DefaultAzureCredential azureCredential;

    /**
     * Initializes the private variables as required.
     */
    @PostConstruct
    public void initialize() {
        graphServiceClientMap = new ConcurrentHashMap<>();
    }

    /**
     * get the graph service client.
     * @param dataPartitionId data partition id
     * @return graph service client
     */
    @Override
    public GraphServiceClient getGraphServiceClient(final String dataPartitionId) {
        Validators.checkNotNullAndNotEmpty(dataPartitionId, "dataPartitionId");
        return graphServiceClientMap.computeIfAbsent(
                String.format(cacheKey, dataPartitionId),
                cosmosClient -> createGraphServiceClient()
        );
    }

    /***
     * create a graph service client.
     * @return graph service client
     */
    private GraphServiceClient createGraphServiceClient() {
        return new GraphServiceClient(azureCredential, scope);
    }
}
