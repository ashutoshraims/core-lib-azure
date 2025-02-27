package org.opengroup.osdu.azure.eventgridmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.cache.EventGridManagerCache;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.azure.util.AzureTokenCredentialsService;
import org.opengroup.osdu.core.common.partition.Property;
import org.springframework.beans.factory.annotation.Autowired;

import com.azure.security.keyvault.secrets.SecretClient;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.eventgrid.v2020_04_01_preview.implementation.EventGridManager;

@ExtendWith(MockitoExtension.class)
class EventGridManagerFactoryImplTest {

  private static final String VALID_DATA_PARTITION_ID = "validDataPartitionId";
  public static final String SUBSCRIPTION_ID = "subscription-id";
  @Mock
  private AzureTokenCredentialsService credentialsService;
  @Mock
  private PartitionServiceClient partitionService;
  @Mock
  private EventGridManagerCache clientCache;

  @InjectMocks
  private EventGridManagerFactoryImpl factory;

  @Test
  void should_throwException_given_nullDataPartitionId() {

    NullPointerException nullPointerException = Assertions.assertThrows(NullPointerException.class,
        () -> this.factory.getManager(null));
    assertEquals("partitionId cannot be null!", nullPointerException.getMessage());
  }

  @Test
  void should_throwException_given_emptyDataPartitionId() {

    IllegalArgumentException illegalArgumentException = Assertions.assertThrows(IllegalArgumentException.class,
        () -> this.factory.getManager(""));
    assertEquals("partitionId cannot be empty!", illegalArgumentException.getMessage());
  }

  @Test
  void should_return_validClient_given_validPartitionId() {
    // Setup
    when(this.partitionService.getPartition(VALID_DATA_PARTITION_ID)).thenReturn(
        PartitionInfoAzure.builder()
            .idConfig(Property.builder().value(VALID_DATA_PARTITION_ID).build())
            .azureSubscriptionIdConfig(Property.builder().value(SUBSCRIPTION_ID).build()).build());
    AzureTokenCredentials azureTokenCredentials = Mockito.mock(AzureTokenCredentials.class);
    when(azureTokenCredentials.defaultSubscriptionId()).thenReturn(SUBSCRIPTION_ID);
    when(azureTokenCredentials.environment()).thenReturn(AzureEnvironment.AZURE);
    when(credentialsService.getAppTokenCredentials(SUBSCRIPTION_ID)).thenReturn(azureTokenCredentials);
    when(this.clientCache.containsKey(any())).thenReturn(false);

    // Act
    EventGridManager eventGridClient = this.factory.getManager(VALID_DATA_PARTITION_ID);

    // Assert
    assertNotNull(eventGridClient);
    verify(this.clientCache, times(1)).put(any(), any());
  }
}