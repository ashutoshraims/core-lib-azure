package org.opengroup.osdu.azure.eventgridmanager;

import org.opengroup.osdu.azure.cache.EventGridManagerCache;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.azure.util.AzureTokenCredentialsService;
import org.opengroup.osdu.common.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.eventgrid.v2020_04_01_preview.implementation.EventGridManager;
import com.microsoft.rest.LogLevel;

/**
 * Interface for Event Grid Manager Factory to return appropriate EventGridManager based on the data partition id.
 */
@Component
@ConditionalOnProperty(value = "azure.eventgrid.manager.enabled", havingValue = "true", matchIfMissing = true)
public class EventGridManagerFactoryImpl implements EventGridManagerFactory {

  @Autowired
  private PartitionServiceClient partitionService;
  @Autowired
  private EventGridManagerCache clientCache;
  @Autowired
  private AzureTokenCredentialsService credentialsService;

  /**
   * @param partitionId partition id
   * @return EventGridManager
   */
  public EventGridManager getManager(final String partitionId) {
    Validators.checkNotNullAndNotEmpty(partitionId, "partitionId");

    String cacheKey = partitionId + "-eventGridManager";
    if (clientCache.containsKey(cacheKey)) {
      return clientCache.get(cacheKey);
    }

    PartitionInfoAzure pi = partitionService.getPartition(partitionId);
    AzureTokenCredentials credentials = credentialsService.getAppTokenCredentials(pi.getAzureSubscriptionId());
    EventGridManager eventGridManager = EventGridManager.configure().withLogLevel(LogLevel.BASIC)
        .authenticate(credentials, credentials.defaultSubscriptionId());
    clientCache.put(cacheKey, eventGridManager);
    return eventGridManager;
  }
}
