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
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.common.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation for IBlobContainerClientFactory.
 */
@Component
@Lazy
public class BlobContainerClientFactoryImpl implements IBlobContainerClientFactory {

    private Map<String, BlobContainerClient> blobContainerClientMap;

    @Autowired
    private DefaultAzureCredential defaultAzureCredential;

    @Autowired
    private PartitionServiceClient partitionService;

    @Autowired
    private MSIConfiguration msiConfiguration;

    /**
     * Initializes the private variables as required.
     */
    @PostConstruct
    public void initialize() {
        blobContainerClientMap = new ConcurrentHashMap<>();
    }

    /**
     * @param dataPartitionId Data partition id
     * @param containerName   Blob container name
     * @return the blob container client instance.
     */
    @Override
    public BlobContainerClient getClient(final String dataPartitionId, final String containerName) {
        Validators.checkNotNull(defaultAzureCredential, "Credential");
        Validators.checkNotNullAndNotEmpty(dataPartitionId, "dataPartitionId");
        Validators.checkNotNullAndNotEmpty(containerName, "containerName");

        String cacheKey = String.format("%s-%s", dataPartitionId, containerName);
        if (this.blobContainerClientMap.containsKey(cacheKey)) {
            return this.blobContainerClientMap.get(cacheKey);
        }
        return this.blobContainerClientMap.computeIfAbsent(cacheKey, blobContainerClient -> createBlobContainerClient(dataPartitionId, containerName));
    }

    /**
     * @param dataPartitionId Data partition id
     * @param containerName   Blob container name
     * @return the blob container client instance.
     */
    private BlobContainerClient createBlobContainerClient(final String dataPartitionId, final String containerName) {
        PartitionInfoAzure pi = this.partitionService.getPartition(dataPartitionId);

        BlobContainerClientBuilder blobContainerClientBuilder = new BlobContainerClientBuilder()
                .endpoint(pi.getStorageBlobEndpoint())
                .containerName(containerName);

        if (msiConfiguration.getIsEnabled()) {
            return blobContainerClientBuilder.credential(defaultAzureCredential)
                    .buildClient();
        } else {
            StorageSharedKeyCredential storageSharedKeyCredential = new StorageSharedKeyCredential(
                    pi.getStorageAccountName(),
                    pi.getStorageAccountKey()
            );

            return blobContainerClientBuilder.credential(storageSharedKeyCredential)
                    .buildClient();
        }
    }
}
