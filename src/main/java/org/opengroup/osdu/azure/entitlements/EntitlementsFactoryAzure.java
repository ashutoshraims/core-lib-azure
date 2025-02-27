// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License

package org.opengroup.osdu.azure.entitlements;

import org.opengroup.osdu.core.common.entitlements.EntitlementsAPIConfig;
import org.opengroup.osdu.core.common.entitlements.EntitlementsService;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsService;
import org.opengroup.osdu.core.common.http.IHttpClient;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyMapper;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Implements IEntitlementsFactory.
 */
@Component
@Primary
@ConditionalOnProperty(value = "azure.entitlements.factory.enabled", havingValue = "true", matchIfMissing = true)
public class EntitlementsFactoryAzure implements IEntitlementsFactory {

    private final EntitlementsAPIConfig config;
    private final HttpResponseBodyMapper mapper;
    private final IHttpClient client;

    /**
     * Constructor Injection for above 3 fields.
     *
     * @param entitlementsConfig EntitlementsAPIConfig
     * @param httpMapper HttpResponseBodyMapper
     * @param httpClient IHttpClient
     */
    @Autowired
    public EntitlementsFactoryAzure(final EntitlementsAPIConfig entitlementsConfig, final HttpResponseBodyMapper httpMapper, final IHttpClient httpClient) {
        this.config = entitlementsConfig;
        this.mapper = httpMapper;
        this.client = httpClient;
    }

    /**
     * returns instance of EntitlementsService.
     *
     * @param headers DpsHeaders
     * @return IEntitlementsService
     */
    @Override
    public IEntitlementsService create(final DpsHeaders headers) {
        Objects.requireNonNull(headers, "headers cannot be null");

        return new EntitlementsService(this.config,
                this.client,
                headers,
                this.mapper);
    }
}
