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

package org.opengroup.osdu.azure.blobstorage;

import com.azure.identity.DefaultAzureCredential;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.policy.RequestRetryOptions;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.azure.blobstorage.system.config.SystemBlobStoreConfig;
import org.opengroup.osdu.azure.di.BlobStoreRetryConfiguration;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.common.Validators;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation for IBlobServiceClientFactory.
 */
public class BlobServiceClientFactoryImpl implements IBlobServiceClientFactory {
    private DefaultAzureCredential defaultAzureCredential;
    private PartitionServiceClient partitionService;
    private Map<String, BlobServiceClient> blobServiceClientMap;
    private static final String SYSTEM_STORAGE_CACHE_KEY = "system_storage";
    private static final String SYSTEM_STORAGE_BLOB_ENDPOINT = "system-storage-blob-endpoint";

    @Autowired
    private BlobStoreRetryConfiguration blobStoreRetryConfiguration;

    @Autowired
    private SecretClient secretClient;

    @Autowired
    private MSIConfiguration msiConfiguration;

    @Autowired
    private SystemBlobStoreConfig systemBlobStoreConfig;

    /**
     * Constructor to initialize instance of {@link BlobServiceClientFactoryImpl}.
     * @param credentials Azure Credentials to use
     * @param partitionServiceClient Partition service client to use
     */
    public BlobServiceClientFactoryImpl(final DefaultAzureCredential credentials,
                                        final PartitionServiceClient partitionServiceClient) {
        this.defaultAzureCredential = credentials;
        this.partitionService = partitionServiceClient;
        blobServiceClientMap = new ConcurrentHashMap<>();
    }

    /**
     * @param dataPartitionId data partition id.
     * @return BlobServiceClient corresponding to the given data partition id.
     */
    @Override
    public BlobServiceClient getBlobServiceClient(final String dataPartitionId) {
        Validators.checkNotNull(defaultAzureCredential, "Credential");
        Validators.checkNotNullAndNotEmpty(dataPartitionId, "dataPartitionId");

        String cacheKey = String.format("%s-blobServiceClient", dataPartitionId);
        if (this.blobServiceClientMap.containsKey(cacheKey)) {
            return this.blobServiceClientMap.get(cacheKey);
        }
        return this.blobServiceClientMap.computeIfAbsent(cacheKey, blobServiceClient -> createBlobServiceClient(dataPartitionId));
    }

    /**
     * @return BlobServiceClient for system resources.
     */
    @Override
    public BlobServiceClient getSystemBlobServiceClient() {
        Validators.checkNotNull(defaultAzureCredential, "Credential");

        if (this.blobServiceClientMap.containsKey(SYSTEM_STORAGE_CACHE_KEY)) {
            return this.blobServiceClientMap.get(SYSTEM_STORAGE_CACHE_KEY);
        }

        return this.blobServiceClientMap.computeIfAbsent(SYSTEM_STORAGE_CACHE_KEY, blobServiceClient -> createSystemBlobServiceClient());
    }

    /**
     * @param dataPartitionId data partition id.
     * @return BlobServiceClient
     */
    private BlobServiceClient createBlobServiceClient(final String dataPartitionId) {
        PartitionInfoAzure pi = this.partitionService.getPartition(dataPartitionId);
        String endpoint = pi.getStorageBlobEndpoint();
        BlobServiceClientBuilder blobServiceClientBuilder = getBlobServiceClientBuilder(endpoint);

        if (msiConfiguration.getIsEnabled()) {
            return blobServiceClientBuilder.credential(defaultAzureCredential)
                    .buildClient();
        } else {
            StorageSharedKeyCredential storageSharedKeyCredential = new StorageSharedKeyCredential(
                    pi.getStorageAccountName(),
                    pi.getStorageAccountKey()
            );
            return blobServiceClientBuilder.credential(storageSharedKeyCredential)
                    .buildClient();
        }
    }

    /**
     * @return BlobServiceClient for system resources.
     */
    private BlobServiceClient createSystemBlobServiceClient() {

        String endpoint = KeyVaultFacade.getSecretWithDefault(secretClient, SYSTEM_STORAGE_BLOB_ENDPOINT, null);

        if (endpoint == null) {
            endpoint = String.format("https://%s.blob.core.windows.net", getSecret(systemBlobStoreConfig.getStorageAccountNameKeyName()));
        }

        BlobServiceClientBuilder blobServiceClientBuilder = getBlobServiceClientBuilder(endpoint);

        if (msiConfiguration.getIsEnabled()) {
            return blobServiceClientBuilder.credential(defaultAzureCredential)
                    .buildClient();
        } else {
            StorageSharedKeyCredential storageSharedKeyCredential = new StorageSharedKeyCredential(
                    getSecret(systemBlobStoreConfig.getStorageAccountNameKeyName()),
                    getSecret(systemBlobStoreConfig.getStorageKeyKeyName())
            );
            return blobServiceClientBuilder.credential(storageSharedKeyCredential)
                    .buildClient();
        }
    }

    /**
     * @param storageAccountEndpoint EndpointName of the Storage account.
     * @return BlobServiceClientBuilder for system resources.
     */
    private BlobServiceClientBuilder getBlobServiceClientBuilder(final String storageAccountEndpoint) {
        RequestRetryOptions requestRetryOptions = blobStoreRetryConfiguration.getRequestRetryOptions();
        return new BlobServiceClientBuilder()
                .endpoint(storageAccountEndpoint)
                .retryOptions(requestRetryOptions);
    }

    /**
     * @param keyName Name of the key to be read from key vault.
     * @return secret value
     */
    private String getSecret(final String keyName) {
        return KeyVaultFacade.getSecretWithValidation(secretClient, keyName);
    }
}
