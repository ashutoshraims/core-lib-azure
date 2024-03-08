package org.opengroup.osdu.azure.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.azure.security.keyvault.secrets.SecretClient;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.di.AzureActiveDirectoryConfiguration;
import org.opengroup.osdu.azure.di.PodIdentityConfiguration;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.IdToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.HashMap;
import java.util.Map;

import static org.opengroup.osdu.azure.util.AuthUtils.getClientSecret;

/**
 * Azure Service Principle token service.
 */
@Component
@Lazy
public class AzureServicePrincipleTokenService {

    @Inject
    @Named("AAD_OBO_API")
    private String aadClientId;
    @Inject
    @Named("APP_DEV_SP_USERNAME")
    private String clientID;
    @Inject
    @Named("APP_DEV_SP_PASSWORD")
    private String clientSecret;
    @Inject
    @Named("APP_DEV_SP_TENANT_ID")
    private String tenantId;

    @Autowired
    private PodIdentityConfiguration podIdentityConfiguration;
    @Autowired
    private AzureActiveDirectoryConfiguration aadConfiguration;
    @Autowired
    private SecretClient sc;

    private final AzureServicePrincipal azureServicePrincipal = new AzureServicePrincipal();

    private Map<String, Object> tokenCache = new HashMap<>();

    /**
     * @return service principle's authorization token
     */
    public String getAuthorizationToken() {
        String accessToken = "";
        try {
            IdToken cachedToken = (IdToken) this.tokenCache.get("token");
            if (!IdToken.refreshToken(cachedToken)) {
                return cachedToken.getTokenValue();
            }
            if (!podIdentityConfiguration.getIsEnabled()) {
                try {
                    accessToken = this.azureServicePrincipal.getIdToken(clientID, clientSecret, tenantId, aadClientId);
                } catch (AppException e) {
                    if (e.getError().getCode() != HttpStatus.SC_UNAUTHORIZED) {
                        throw e;
                    }

                    String newClientSecret = getClientSecret(aadConfiguration, sc);
                    if (clientSecret.equals(newClientSecret)) {
                        throw e;
                    } else {
                        accessToken = this.azureServicePrincipal.getIdToken(clientID, newClientSecret, tenantId, aadClientId);
                        clientSecret = newClientSecret;
                    }
                }
            } else {
                accessToken = this.azureServicePrincipal.getMSIToken();
            }
            IdToken idToken = IdToken.builder()
                    .tokenValue(accessToken)
                    .expirationTimeMillis(JWT.decode(accessToken).getExpiresAt().getTime())
                    .build();
            this.tokenCache.put("token", idToken);
        } catch (JWTDecodeException e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Persistence error", "Invalid token, error decoding", e);
        } catch (Exception e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Persistence error", "Error generating token", e);
        }

        return accessToken;
    }
}
