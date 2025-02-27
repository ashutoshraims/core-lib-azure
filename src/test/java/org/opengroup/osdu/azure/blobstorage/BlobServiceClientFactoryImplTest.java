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
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.common.policy.RequestRetryOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.blobstorage.system.config.SystemBlobStoreConfig;
import org.opengroup.osdu.azure.di.BlobStoreConfiguration;
import org.opengroup.osdu.azure.di.BlobStoreRetryConfiguration;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.logging.ILogger;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
public class BlobServiceClientFactoryImplTest {

    @Mock
    private DefaultAzureCredential credential;
    @Mock
    private PartitionServiceClient partitionService;
    @Mock
    private Map<String, BlobServiceClient> blobServiceClientMap;
    @Mock
    private BlobStoreConfiguration configuration;
    @Mock
    private BlobStoreRetryConfiguration blobStoreRetryConfiguration;
    @Mock
    private ILogger logger;
    @Mock
    private SystemBlobStoreConfig systemBlobStoreConfig;
    @Mock
    private SecretClient secretClient;
    @Mock
    private KeyVaultSecret keyVaultSecret;
    @Mock
    private PartitionInfoAzure partitionInfoAzure;
    @Mock
    private BlobServiceClient blobServiceClient;
    @Mock
    private RequestRetryOptions retryOptions;
    @Mock
    private MSIConfiguration msiConfiguration;
    @InjectMocks
    private BlobServiceClientFactoryImpl sut;

    private static final String ACCOUNT_NAME = "testAccount";
    private static final String ACCOUNT_KEY = "testAccountKey";
    private static final String PARTITION_ID = "dataPartitionId";
    private static final String SYSTEM_STORAGE_ACCOUNT_NAME = "system-storage-account";
    private static final String SYSTEM_STORAGE_ACCOUNT_VALUE = "system-storage-account-value";
    private static final String SYSTEM_STORAGE_KEY_NAME = "system-storage-key";
    private static final String SYSTEM_STORAGE_KEY_VALUE = "system-storage-key-value";

    @BeforeEach
    void init() {
        initMocks(this);
        lenient().doReturn(ACCOUNT_NAME).when(configuration).getStorageAccountName();
        lenient().doReturn(SYSTEM_STORAGE_ACCOUNT_NAME).when(systemBlobStoreConfig).getStorageAccountNameKeyName();
        lenient().doReturn(SYSTEM_STORAGE_KEY_NAME).when(systemBlobStoreConfig).getStorageKeyKeyName();

        lenient().doReturn(SYSTEM_STORAGE_ACCOUNT_VALUE).when(keyVaultSecret).getValue();
        lenient().doReturn(SYSTEM_STORAGE_KEY_VALUE).when(keyVaultSecret).getValue();

        lenient().doReturn(keyVaultSecret).when(secretClient).getSecret(SYSTEM_STORAGE_ACCOUNT_NAME);
        lenient().doReturn(keyVaultSecret).when(secretClient).getSecret(SYSTEM_STORAGE_KEY_NAME);
    }

    @Test
    public void should_throwException_given_nullDataPartitionId() {
        try {
            this.sut.getBlobServiceClient(null);
        } catch (NullPointerException ex) {
            assertEquals("dataPartitionId cannot be null!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

    @Test
    public void should_throwException_given_emptyDataPartitionId() {
        try {
            this.sut.getBlobServiceClient("");
        } catch (IllegalArgumentException ex) {
            assertEquals("dataPartitionId cannot be empty!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

    @Test
    public void should_return_cachedContainer_when_cachedEarlier() {
        final String cacheKey = String.format("%s-blobServiceClient", PARTITION_ID);
        when(this.blobServiceClientMap.containsKey(cacheKey)).thenReturn(true);
        when(this.blobServiceClientMap.get(cacheKey)).thenReturn(blobServiceClient);

        BlobServiceClient client = this.sut.getBlobServiceClient(PARTITION_ID);
        assertNotNull(client);
        verify(this.blobServiceClientMap, times(1)).containsKey(cacheKey);
        verify(this.blobServiceClientMap, times(1)).get(cacheKey);
        verify(this.partitionService, never()).getPartition(PARTITION_ID);
    }

    @Test
    public void should_return_validContainer_system_whenCached() {
        final String cacheKey = "system_storage";
        when(this.blobServiceClientMap.containsKey(cacheKey)).thenReturn(true);
        when(this.blobServiceClientMap.get(cacheKey)).thenReturn(blobServiceClient);

        BlobServiceClient client = this.sut.getSystemBlobServiceClient();
        assertNotNull(client);
        verify(this.blobServiceClientMap, times(1)).containsKey(cacheKey);
        verify(this.blobServiceClientMap, times(1)).get(cacheKey);
        verify(this.partitionService, never()).getPartition(PARTITION_ID);
    }
}
