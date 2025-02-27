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
// limitations under the License.

package org.opengroup.osdu.azure.multitenancy;

import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * Implementation for ITenantFactory.
 */
@Component
@Lazy
@ConditionalOnProperty(value = "tenantFactoryImpl.required", havingValue = "true", matchIfMissing = false)
public class TenantFactoryImpl implements ITenantFactory {

    @Autowired
    @Lazy
    private PartitionServiceClient partitionService;

    @Autowired
    private ILogger logger;

    @Autowired
    private String appDevSpUsername;

    private static final String LOG_PREFIX = "azure-core-lib";

    private Map<String, TenantInfo> tenants = new ConcurrentHashMap<>();

    /**
     * @param tenantName Tenant name
     * @return true or false depending on whether tenant is present
     */
    @Override
    public boolean exists(final String tenantName) {
        if (!this.tenants.containsKey(tenantName)) {
            initPartition(tenantName);
        }
        return this.tenants.containsKey(tenantName);
    }

    /**
     * @param tenantName Tenant name
     * @return tenantInfo object
     */
    @Override
    public TenantInfo getTenantInfo(final String tenantName) {
        if (!this.tenants.containsKey(tenantName)) {
            initPartition(tenantName);
        }
        return this.tenants.get(tenantName);
    }

    /**
     * @return list of tenantInfo objects for all the tenants
     */
    public Collection<TenantInfo> listTenantInfo() {
        return this.partitionService.listPartitions().stream().map(this::buildTenantInfo).collect(Collectors.toList());
    }

    /**
     * Build TenanInfo Object.
     *
     * @param partitionId Partition Id
     * @return TenantInfo object
     */
    private TenantInfo buildTenantInfo(final String partitionId) {
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setName(partitionId);
        tenantInfo.setDataPartitionId(partitionId);
        return tenantInfo;
    }


    /**
     * @param tenantName        Tenant name
     * @param host              Host name
     * @param port              Port
     * @param expireTimeSeconds Expiry time in seconds
     * @param classOfV          Class reference
     * @param <V>               Template class
     * @return cache
     */
    public <V> ICache<String, V> createCache(final String tenantName, final String host, final int port, final int expireTimeSeconds, final Class<V> classOfV) {
        return null;
    }

    /**
     * Flush the cache.
     */
    public void flushCache() {

    }

    /**
     * Initialise the local cache for tenants.
     *
     * @param tenantId Tenant name
     */
    private void initPartition(final String tenantId) {
        PartitionInfoAzure partitionInfo;
        try {
            partitionInfo = this.partitionService.getPartition(tenantId);
        } catch (AppException e) {
            this.logger.error(LOG_PREFIX, String.format("Error getting tenant: %s via partition service.", tenantId), e, Collections.emptyMap());
            return;
        }

        TenantInfo ti = new TenantInfo();
        String tenantName = partitionInfo.getId();
        ti.setName(tenantName);
        ti.setComplianceRuleSet(partitionInfo.getComplianceRuleset());
        ti.setServiceAccount(appDevSpUsername); //set serviceprincipalAppId in Azure side instead of ServiceAccount in GCP
        ti.setDataPartitionId(tenantName);
        this.tenants.put(tenantName, ti);
    }
}