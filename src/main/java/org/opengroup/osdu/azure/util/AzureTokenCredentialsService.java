package org.opengroup.osdu.azure.util;

import java.util.HashMap;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.springframework.stereotype.Component;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;

/**
 *  Class to generate the ApplicationTokenCredentials for Event Grid Manager.
 */
@Component
public class AzureTokenCredentialsService {

  @Inject
  @Named("APP_DEV_SP_USERNAME")
  private String clientID;
  @Inject
  @Named("APP_DEV_SP_PASSWORD")
  private String clientSecret;
  @Inject
  @Named("APP_DEV_SP_TENANT_ID")
  private String tenantId;

  /**
   * @param subscriptionId string
   * @return ApplicationTokenCredentials
   */
  public AzureTokenCredentials getAppTokenCredentials(final String subscriptionId) {
    AzureEnvironment azureEnvironment = new AzureEnvironment(new HashMap<>());
    azureEnvironment.endpoints().putAll(AzureEnvironment.AZURE.endpoints());
    return new ApplicationTokenCredentials(clientID, tenantId, clientSecret, azureEnvironment)
        .withDefaultSubscriptionId(subscriptionId);
  }

}
