package org.opengroup.osdu.azure.util;

import com.azure.security.keyvault.secrets.SecretClient;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.opengroup.osdu.azure.di.AzureActiveDirectoryConfiguration;
import org.opengroup.osdu.azure.di.PodIdentityConfiguration;
import org.opengroup.osdu.azure.di.WorkloadIdentityConfiguration;
import org.opengroup.osdu.core.common.model.http.AppException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AzureServicePrincipleTokenServiceTest {

    private static final String clientSecret = "client-secret";

    @Mock
    private PodIdentityConfiguration podIdentityConfiguration;

    @Mock
    private AzureActiveDirectoryConfiguration aadConfiguration;

    @Mock
    private SecretClient sc;

    @Mock
    private AzureServicePrincipal azureServicePrincipal;

    @Mock
    private Map<String, Object> tokenCache;

    @Mock
    private WorkloadIdentityConfiguration workloadIdentityConfiguration;

    @InjectMocks
    private AzureServicePrincipleTokenService azureServicePrincipleTokenService;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetAuthorizationToken_Error() {
        when(podIdentityConfiguration.getIsEnabled()).thenReturn(false);
        when(workloadIdentityConfiguration.getIsEnabled()).thenReturn(false);
        when(this.azureServicePrincipal.getIdToken(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenThrow(new AppException(HttpStatus.SC_NOT_FOUND, "error", "message"));

        assertThrows(AppException.class, () -> azureServicePrincipleTokenService.getAuthorizationToken());
    }

    @Test
    public void should_throw_AppException_if_both_client_secret_equals() {
        when(podIdentityConfiguration.getIsEnabled()).thenReturn(false);
        when(workloadIdentityConfiguration.getIsEnabled()).thenReturn(false);
        when(aadConfiguration.getClientSecret()).thenReturn(clientSecret);
        when(azureServicePrincipal.getIdToken(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenThrow(new AppException(HttpStatus.SC_UNAUTHORIZED, "error", "message"));

        assertThrows(AppException.class, () -> azureServicePrincipleTokenService.getAuthorizationToken());
    }
}