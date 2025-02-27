package org.opengroup.osdu.azure.servicebus;

import com.microsoft.azure.servicebus.ClientSettings;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.SubscriptionClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import com.microsoft.azure.servicebus.security.ManagedIdentityTokenProvider;
import org.opengroup.osdu.azure.cache.SubscriptionClientCache;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.common.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Implementation for ISubscriptionClientFactory.
 */
@Component
@Lazy
public class SubscriptionClientFactoryImpl implements ISubscriptionClientFactory {

    @Autowired
    private PartitionServiceClient partitionService;

    @Autowired
    private SubscriptionClientCache clientCache;

    @Autowired
    private MSIConfiguration msiConfiguration;


    /**
     * @param dataPartitionId  Data Partition Id
     * @param topicName        Service Bus Topic Name
     * @param subscriptionName Service Bus Subscription Name
     * @return A client configured to communicate with a Service Bus Subscription
     * @throws ServiceBusException  Exception thrown by {@link SubscriptionClient}
     * @throws InterruptedException Exception thrown by {@link SubscriptionClient}
     */
    @Override
    public SubscriptionClient getClient(final String dataPartitionId, final String topicName, final String subscriptionName) throws ServiceBusException, InterruptedException {
        Validators.checkNotNullAndNotEmpty(dataPartitionId, "dataPartitionId");
        Validators.checkNotNullAndNotEmpty(topicName, "topicName");
        Validators.checkNotNullAndNotEmpty(subscriptionName, "subscriptionName");

        String entityPath = String.format("%s/subscriptions/%s", topicName, subscriptionName);
        String cacheKey = String.format("%s-%s", dataPartitionId, entityPath);
        if (this.clientCache.containsKey(cacheKey)) {
            return this.clientCache.get(cacheKey);
        }

        PartitionInfoAzure pi = this.partitionService.getPartition(dataPartitionId);
        SubscriptionClient subscriptionClient;

        if (msiConfiguration.getIsEnabled()) {
            String serviceBusNamespace = pi.getSbNamespace();
            subscriptionClient = getSubscriptionClientMSI(entityPath, serviceBusNamespace);
        } else {
            String serviceBusConnectionString = pi.getSbConnection();
            subscriptionClient = getSubscriptionClient(entityPath, serviceBusConnectionString);
        }

        this.clientCache.put(cacheKey, subscriptionClient);
        return subscriptionClient;
    }

    /***
     * @param entityPath                 Service Bus entity path
     * @param serviceBusConnectionString Service Bus Connection String
     * @return SubscriptionClient object
     * @throws InterruptedException Exception thrown by {@link SubscriptionClient}
     * @throws ServiceBusException  Exception thrown by {@link SubscriptionClient}
     */
    SubscriptionClient getSubscriptionClient(final String entityPath, final String serviceBusConnectionString) throws InterruptedException, ServiceBusException {
        ConnectionStringBuilder connectionStringBuilder = new ConnectionStringBuilder(
                serviceBusConnectionString,
                entityPath
        );
        return new SubscriptionClient(connectionStringBuilder, ReceiveMode.PEEKLOCK);
    }

    /***
     * @param entityPath                 Service Bus entity path
     * @param namespace                  Service Bus namespace
     * @return SubscriptionClient object
     * @throws InterruptedException Exception thrown by {@link SubscriptionClient}
     * @throws ServiceBusException  Exception thrown by {@link SubscriptionClient}
     */
    SubscriptionClient getSubscriptionClientMSI(final String entityPath, final String namespace) throws InterruptedException, ServiceBusException {
        return new SubscriptionClient(namespace, entityPath, new ClientSettings(new ManagedIdentityTokenProvider()), ReceiveMode.PEEKLOCK);
    }
}