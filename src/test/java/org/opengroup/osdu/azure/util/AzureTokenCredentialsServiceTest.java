package org.opengroup.osdu.azure.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.security.keyvault.secrets.SecretClient;
import com.microsoft.azure.credentials.AzureTokenCredentials;

@ExtendWith(MockitoExtension.class)
class AzureTokenCredentialsServiceTest {

  public static final String SUBSCRIPTION_ID = "subscription-id";
  @Mock
  private SecretClient secretClient;

  @InjectMocks
  private AzureTokenCredentialsService azureTokenCredentialsService;

  @Test
  void should_return_credential() {
    AzureTokenCredentials credentials = azureTokenCredentialsService.getAppTokenCredentials(SUBSCRIPTION_ID);
    assertNotNull(credentials);
  }
}