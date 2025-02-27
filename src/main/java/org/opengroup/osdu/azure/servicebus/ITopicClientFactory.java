package org.opengroup.osdu.azure.servicebus;

import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;

/**
 * Interface for Topic Client Factory to return appropriate topic client.
 * instances for each tenant based on data partition and topic id
 */
public interface ITopicClientFactory {

    /**
     * @param dataPartitionId Data Partition Id
     * @param topicName      Service Bus topic
     * @return A client configured to communicate with a Service Bus Topic
     * @throws ServiceBusException  Exception thrown by {@link TopicClient}
     * @throws InterruptedException Exception thrown by {@link TopicClient}
     */
    TopicClient getClient(String dataPartitionId, String topicName) throws ServiceBusException, InterruptedException;
}