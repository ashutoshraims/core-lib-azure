package org.opengroup.osdu.azure.servicebus;

import com.azure.identity.DefaultAzureCredential;
import com.microsoft.azure.servicebus.ClientSettings;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.opengroup.osdu.azure.dependencies.DefaultAzureServiceBusCredential;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.common.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation for ITopicClientFactory.
 */
@Component
@Lazy
public class TopicClientFactoryImpl implements ITopicClientFactory {

    private static final String LOGGER_NAME = TopicClientFactoryImpl.class.getName();
    private Map<String, TopicClient> topicClientMap;

    @Autowired
    private DefaultAzureCredential defaultAzureCredential;

    @Autowired
    private PartitionServiceClient partitionService;

    @Autowired
    private MSIConfiguration msiConfiguration;

    /**
     * Initializes the private variables as required.
     */
    @PostConstruct
    public void initialize() {
        topicClientMap = new ConcurrentHashMap<>();
    }

    /**
     * @param dataPartitionId Data Partition Id
     * @param topicName       Service Bus topic
     * @return A client configured to communicate with a Service Bus Topic
     * @throws ServiceBusException  Exception thrown by {@link TopicClient}
     * @throws InterruptedException Exception thrown by {@link TopicClient}
     */
    @Override
    public TopicClient getClient(final String dataPartitionId, final String topicName) throws ServiceBusException, InterruptedException {
        Validators.checkNotNull(defaultAzureCredential, "Credential");
        Validators.checkNotNullAndNotEmpty(dataPartitionId, "dataPartitionId");
        Validators.checkNotNullAndNotEmpty(topicName, "topicName");

        String cacheKey = String.format("%s-%s", dataPartitionId, topicName);
        if (this.topicClientMap.containsKey(cacheKey)) {
            return this.topicClientMap.get(cacheKey);
        }
        TopicClient topicClient = createTopicClient(dataPartitionId, topicName);
        if (topicClient != null) {
            topicClientMap.put(cacheKey, topicClient);
        }
        return topicClient;
    }

    /**
     * @param dataPartitionId Data Partition Id
     * @param topicName       Service Bus topic
     * @return A client configured to communicate with a Service Bus Topic
     * @throws ServiceBusException  Exception thrown by {@link TopicClient}
     * @throws InterruptedException Exception thrown by {@link TopicClient}
     */
    private TopicClient createTopicClient(final String dataPartitionId, final String topicName) throws ServiceBusException, InterruptedException {
        PartitionInfoAzure pi = this.partitionService.getPartition(dataPartitionId);

        if (msiConfiguration.getIsEnabled()) {
            final ClientSettings clientSettings = new ClientSettings(
                    new DefaultAzureServiceBusCredential(defaultAzureCredential));
            return new TopicClient(pi.getSbNamespace(), topicName, clientSettings);
        } else {
            ConnectionStringBuilder connectionStringBuilder = new ConnectionStringBuilder(
                    pi.getSbConnection(),
                    topicName
            );
            return new TopicClient(connectionStringBuilder);
        }
    }
}