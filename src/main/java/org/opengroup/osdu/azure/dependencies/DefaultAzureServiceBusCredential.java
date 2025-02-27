//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.azure.dependencies;

import com.microsoft.azure.servicebus.security.SecurityToken;
import com.microsoft.azure.servicebus.security.SecurityTokenType;
import com.microsoft.azure.servicebus.security.TokenProvider;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * A Service Bus {@link TokenProvider} that uses {@link DefaultAzureCredential} that is available in com
 * .azure:azure-identity module. This class provides a convenient mechanism to authenticate service bus using the latest
 * Azure Identity SDK.
 */

public class DefaultAzureServiceBusCredential extends TokenProvider {

    private static final String SERVICE_BUS_SCOPE = "https://servicebus.azure.net/.default";
    private final DefaultAzureCredential defaultAzureCredential;
    private final Map<String, SecurityToken> tokenCache = new ConcurrentHashMap<>();

    /**
     * Creates an instance of DefaultAzureServiceBusCredential.
     */
    public DefaultAzureServiceBusCredential() {
        this(new DefaultAzureCredentialBuilder().build());
    }

    /**
     * Creates an instance of DefaultAzureServiceBusCredential using the provided {@link DefaultAzureCredential}.
     *
     * @param credential The {@link DefaultAzureCredential} to use.
     */
    public DefaultAzureServiceBusCredential(final DefaultAzureCredential credential) {
        this.defaultAzureCredential = credential;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<SecurityToken> getSecurityTokenAsync(final String audience) {
        TokenRequestContext tokenRequestContext = new TokenRequestContext().addScopes(SERVICE_BUS_SCOPE);
        if (tokenCache.containsKey(audience) && tokenCache.get(audience).getValidUntil().isAfter(Instant.now())) {
            return Mono.just(tokenCache.get(audience)).toFuture();
        }

        return defaultAzureCredential
                .getToken(tokenRequestContext)
                .flatMap(accessToken -> {
                    SecurityToken securityToken = new SecurityToken(SecurityTokenType.JWT, audience, accessToken.getToken(),
                            Instant.now(), accessToken.getExpiresAt().toInstant());
                    tokenCache.put(audience, securityToken);
                    return Mono.just(securityToken);
                }).toFuture();
    }

}
