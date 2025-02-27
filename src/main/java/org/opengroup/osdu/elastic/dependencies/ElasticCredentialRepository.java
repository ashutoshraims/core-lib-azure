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

package org.opengroup.osdu.elastic.dependencies;

import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import org.opengroup.osdu.azure.cache.ElasticCredentialsCache;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Implementation of {@link IElasticRepository} for azure.
 */
@AllArgsConstructor
@Component
@Lazy
public class ElasticCredentialRepository implements IElasticRepository {

    @Autowired
    @Lazy
    private PartitionServiceClient partitionService;

    @Autowired
    @Lazy
    private ElasticCredentialsCache clusterSettingsCache;

    /**
     * OSDU application logger.
     */
    @Inject
    private JaxRsDpsLog log;

    /**
     * Returns Elasticsearch cluster settings.
     *
     * @param tenantInfo The tenant for which the credentials represent
     * @return Elasticsearch credentials
     */
    @Override
    public ClusterSettings getElasticClusterSettings(final TenantInfo tenantInfo) {
        String cacheKey = this.clusterSettingsCache.getCacheKey(tenantInfo.getName());
        if (this.clusterSettingsCache.containsKey(cacheKey)) {
            return this.clusterSettingsCache.get(cacheKey);
        }

        PartitionInfoAzure partitionInfo = partitionService.getPartition(tenantInfo.getDataPartitionId());
        URL esURL = getElasticURL(partitionInfo);
        String username = partitionInfo.getElasticUsername();
        String password = partitionInfo.getElasticPassword();
        boolean sslEnabled = Strings.isNullOrEmpty(partitionInfo.getElasticSslEnabled()) ? true : Boolean.parseBoolean(partitionInfo.getElasticSslEnabled());
        ClusterSettings clusterSettings = buildSettings(esURL, username, password, sslEnabled);

        clusterSettingsCache.put(cacheKey, clusterSettings);

        return clusterSettings;
    }

    /**
     * Construct the cluster settings.
     *
     * @param esURL      URL for ES cluster
     * @param username   Username for ES cluster
     * @param password   Password for ES cluster
     * @param sslEnabled ES cluster ssl communication enabled or not
     * @return {@link ClusterSettings} representing the cluster
     */
    private ClusterSettings buildSettings(
            final URL esURL,
            final String username,
            final String password,
            final boolean sslEnabled) {
        ClusterSettings.ClusterSettingsBuilder builder = ClusterSettings.builder()
                .host(esURL.getHost())
                .port(esURL.getPort())
                .userNameAndPassword(String.format("%s:%s", username, password));

        if (!sslEnabled) {
            return builder
                    .https(false)
                    .tls(false)
                    .build();
        }

        failIfNotHTTPS(esURL);
        return builder
                .https(true)
                .tls(true)
                .build();
    }

    /**
     * Fail if the URL is not HTTPS.
     *
     * @param u A URL
     */
    private void failIfNotHTTPS(final URL u) {
        if (!u.getProtocol().toLowerCase().equals("https")) {
            String error = String.format(
                    "Failing to initialize Elasticsearch settings for cluster with endpoint %s."
                            + "Traffic is not using HTTPS, which is insecure.", u);
            log.warning(error);
            throw new IllegalStateException(error);
        }
    }

    /**
     * @param partitionInfo partition configuration
     * @return fully parsed {@link URL} pointing to the Elasticsearch cluster.
     */
    private URL getElasticURL(final PartitionInfoAzure partitionInfo) {
        String qualifiedEndpoint = partitionInfo.getElasticEndpoint();
        try {
            return new URL(qualifiedEndpoint);
        } catch (MalformedURLException e) {
            String error = "Cannot parse Elasticsearch endpoint from KeyVault";
            log.warning(error, e);
            throw new IllegalStateException(error, e);
        }
    }
}
