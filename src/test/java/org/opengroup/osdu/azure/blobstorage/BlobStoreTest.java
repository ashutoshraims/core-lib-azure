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

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.core.common.model.http.AppException;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
public class BlobStoreTest {
    private static final String PARTITION_ID = "dataPartitionId";
    private static final String FILE_PATH = "filePath";
    private static final String CONTENT = "hello world";
    private static final String STORAGE_CONTAINER_NAME = "containerName";

    @InjectMocks
    private BlobStore blobStore;

    @Mock
    private IBlobServiceClientFactory blobServiceClientFactory;

    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlockBlobClient blockBlobClient;

    @Mock
    private BlockBlobItem blockBlobItem;

    @Mock
    private ILogger logger;

    @BeforeEach
    void init() {
        initMocks(this);
        lenient().when(blobClient.getBlockBlobClient()).thenReturn(blockBlobClient);
        lenient().when(blobContainerClient.getBlobClient(FILE_PATH)).thenReturn(blobClient);
        lenient().when(blobServiceClient.getBlobContainerClient(STORAGE_CONTAINER_NAME)).thenReturn(blobContainerClient);
        lenient().when(blobServiceClientFactory.getBlobServiceClient(PARTITION_ID)).thenReturn(blobServiceClient);
        lenient().doNothing().when(logger).warning(eq("azure-core-lib"), any(), anyMap());
    }

    @Test
    public void readFromStorageContainer_ErrorCreatingBlobContainerClient() {
        doThrow(BlobStorageException.class).when(blobServiceClientFactory).getBlobServiceClient(eq(PARTITION_ID));
        try {
            String content = blobStore.readFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(500, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void readFromStorageContainer_ErrorCreatingBlobContainerClient_FromServiceClient() {
        doThrow(BlobStorageException.class).when(blobServiceClient).getBlobContainerClient(eq(STORAGE_CONTAINER_NAME));
        try {
            String content = blobStore.readFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(500, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void readFromStorageContainer_Success() {
        String content = blobStore.readFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        ArgumentCaptor<ByteArrayOutputStream> outputStream = ArgumentCaptor.forClass(ByteArrayOutputStream.class);

        // validate that the download method is being invoked appropriately.
        verify(blockBlobClient).download(outputStream.capture());
    }

    @Test
    public void readFromStorageContainer_BlobNotFound() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.BLOB_NOT_FOUND);
        doThrow(exception).when(blockBlobClient).download(any());
        try {
            String content = blobStore.readFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(404, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void readFromStorageContainer_InternalError() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.INTERNAL_ERROR);
        doThrow(exception).when(blockBlobClient).download(any());
        try {
            String content = blobStore.readFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(500, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void deleteFromStorageContainer_ErrorCreatingBlobContainerClient() {
        doThrow(BlobStorageException.class).when(blobServiceClientFactory).getBlobServiceClient(eq(PARTITION_ID));
        try {
            blobStore.deleteFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(500, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void deleteFromStorageContainer_BlobNotFound() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.BLOB_NOT_FOUND);
        doThrow(exception).when(blockBlobClient).delete();
        try {
            blobStore.deleteFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(404, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void deleteFromStorageContainer_InternalError() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.INTERNAL_ERROR);
        doThrow(exception).when(blockBlobClient).delete();
        try {
            blobStore.deleteFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(500, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void deleteFromStorageContainer_Success() {
        doNothing().when(blockBlobClient).delete();
        try {
            blobStore.deleteFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (Exception ex) {
            fail("should not get any exception.");
        }
    }

    @Test
    public void writeToStorageContainer_ErrorCreatingBlobContainerClient() {
        doThrow(BlobStorageException.class).when(blobServiceClientFactory).getBlobServiceClient(eq(PARTITION_ID));
        try {
            blobStore.writeToStorageContainer(PARTITION_ID, FILE_PATH, CONTENT, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(500, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void writeToStorageContainer_InternalError() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.INTERNAL_ERROR);
        doThrow(exception).when(blockBlobClient).upload(any(), anyLong(), eq(true));
        try {
            blobStore.writeToStorageContainer(PARTITION_ID, FILE_PATH, CONTENT, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(500, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void writeToStorageContainer_Success() {
        doReturn(blockBlobItem).when(blockBlobClient).upload(any(), anyLong(), eq(true));
        try {
            blobStore.writeToStorageContainer(PARTITION_ID, FILE_PATH, CONTENT, STORAGE_CONTAINER_NAME);
        } catch (Exception ex) {
            fail("should not get any exception.");
        }
    }

    private BlobStorageException mockStorageException(BlobErrorCode errorCode) {
        BlobStorageException mockException = mock(BlobStorageException.class);
        lenient().when(mockException.getErrorCode()).thenReturn(errorCode);
        return mockException;
    }
}
