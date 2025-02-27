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

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.polling.LongRunningOperationStatus;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.opengroup.osdu.azure.logging.CoreLogger;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.DependencyLogger;
import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.core.common.model.http.AppException;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BlobStoreTest {
    private static final String PARTITION_ID = "dataPartitionId";
    private static final String FILE_PATH = "filePath";
    private static final String CONTENT = "hello world";
    private static final String STORAGE_CONTAINER_NAME = "containerName";
    private static final String SOURCE_FILE_URL = "http://someURL";
    private static final String FILE_NAME="test.json";
    private static final String FILE_CONTENT_TYPE = "application/json";

    @Mock
    private CoreLoggerFactory coreLoggerFactory;

    @Mock
    private CoreLogger coreLogger;

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
    private BlobProperties blobProperties;

    @Mock
    private BlobInputStream blobInputStream;

    @Mock
    private BlockBlobItem blockBlobItem;

    @Mock
    private BlobItem blobItem;

    @Mock
    private ILogger logger;

    @Mock
    private BlobCopyInfo blobCopyInfo;

    @Mock
    private SyncPoller<BlobCopyInfo, Void> syncPoller;

    @Mock
    private PollResponse<BlobCopyInfo> pollResponse;

    @Mock
    private BlobSasPermission blobSasPermission;

    @Mock
    private DependencyLogger dependencyLogger;

    @InjectMocks
    private BlobStore blobStore;

    /**
     * Workaround for inability to mock static methods like getInstance().
     *
     * @param mock CoreLoggerFactory mock instance
     */
    private void mockSingleton(CoreLoggerFactory mock) {
        try {
            Field instance = CoreLoggerFactory.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reset workaround for inability to mock static methods like getInstance().
     */
    private void resetSingleton() {
        try {
            Field instance = CoreLoggerFactory.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
            instance.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void init() {
        initMocks(this);

        mockSingleton(coreLoggerFactory);
        when(coreLoggerFactory.getLogger(anyString())).thenReturn(coreLogger);

        lenient().when(blobClient.getBlockBlobClient()).thenReturn(blockBlobClient);
        lenient().when(blobContainerClient.getBlobClient(FILE_PATH)).thenReturn(blobClient);
        lenient().when(blobServiceClient.getBlobContainerClient(STORAGE_CONTAINER_NAME)).thenReturn(blobContainerClient);
        lenient().when(blobServiceClientFactory.getBlobServiceClient(PARTITION_ID)).thenReturn(blobServiceClient);
        lenient().when(blobServiceClientFactory.getSystemBlobServiceClient()).thenReturn(blobServiceClient);
        lenient().doNothing().when(logger).warning(eq("azure-core-lib"), any(), anyMap());
        lenient().when(blockBlobClient.getProperties()).thenReturn(blobProperties);
        lenient().when(blockBlobClient.openInputStream()).thenReturn(blobInputStream);
    }

    @AfterEach
    public void takeDown() {
        resetSingleton();
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
    public void readFromStorageContainer_ErrorCreatingBlobContainerClient_System() {
        doThrow(BlobStorageException.class).when(blobServiceClientFactory).getSystemBlobServiceClient();
        try {
            String content = blobStore.readFromStorageContainer(FILE_PATH, STORAGE_CONTAINER_NAME);
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
    public void readFromStorageContainer_ErrorCreatingBlobContainerClient_FromServiceClient_System() {
        doThrow(BlobStorageException.class).when(blobServiceClient).getBlobContainerClient(eq(STORAGE_CONTAINER_NAME));
        try {
            String content = blobStore.readFromStorageContainer(FILE_PATH, STORAGE_CONTAINER_NAME);
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
    public void readFromStorageContainer_Success_System() {
        String content = blobStore.readFromStorageContainer(FILE_PATH, STORAGE_CONTAINER_NAME);
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
    public void readFromStorageContainer_BlobNotFound_System() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.BLOB_NOT_FOUND);
        doThrow(exception).when(blockBlobClient).download(any());
        try {
            String content = blobStore.readFromStorageContainer(FILE_PATH, STORAGE_CONTAINER_NAME);
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
            assertEquals(exception.getStatusCode(), ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void readFromStorageContainer_InternalError_System() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.INTERNAL_ERROR);
        doThrow(exception).when(blockBlobClient).download(any());
        try {
            String content = blobStore.readFromStorageContainer(FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(exception.getStatusCode(), ex.getError().getCode());
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
    public void deleteFromStorageContainer_ErrorCreatingBlobContainerClient_System() {
        doThrow(BlobStorageException.class).when(blobServiceClientFactory).getSystemBlobServiceClient();
        try {
            blobStore.deleteFromStorageContainer(FILE_PATH, STORAGE_CONTAINER_NAME);
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
    public void deleteFromStorageContainer_BlobNotFound_System() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.BLOB_NOT_FOUND);
        doThrow(exception).when(blockBlobClient).delete();
        try {
            blobStore.deleteFromStorageContainer(FILE_PATH, STORAGE_CONTAINER_NAME);
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
    public void deleteFromStorageContainer_InternalError_System() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.INTERNAL_ERROR);
        doThrow(exception).when(blockBlobClient).delete();
        try {
            blobStore.deleteFromStorageContainer(FILE_PATH, STORAGE_CONTAINER_NAME);
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
    public void deleteFromStorageContainer_Success_System() {
        doNothing().when(blockBlobClient).delete();
        try {
            blobStore.deleteFromStorageContainer(FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (Exception ex) {
            fail("should not get any exception.");
        }
    }

    @Test
    public void undeleteFromStorageContainer_ErrorCreatingBlobContainerClient() {
        doThrow(BlobStorageException.class).when(blobServiceClientFactory).getBlobServiceClient(eq(PARTITION_ID));
        try {
            blobStore.undeleteFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(500, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void undeleteFromStorageContainer_BlobNotFound() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.BLOB_NOT_FOUND);
        doThrow(exception).when(blobContainerClient).listBlobs(any(ListBlobsOptions.class), any(Duration.class));
        try {
            blobStore.undeleteFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(404, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void undeleteFromStorageContainer_InternalError() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.INTERNAL_ERROR);
        doThrow(exception).when(blobContainerClient).listBlobs(any(ListBlobsOptions.class), any(Duration.class));
        try {
            blobStore.undeleteFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(exception.getStatusCode(), ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void undeleteFromStorageContainer_InternalError_NullResult() {
        when(blobContainerClient.listBlobs(any(ListBlobsOptions.class), any(Duration.class))).thenReturn(null);
        try {
            blobStore.undeleteFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(404, ex.getError().getCode());
            assertEquals(ex.getError().getMessage(),"No items found");
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void undeleteFromStorageContainer_InternalError_EmptyResult() {
        Iterator mockIterator = mock(Iterator.class);
        PagedIterable mockPagedTableEntities = mock(PagedIterable.class);
        when(mockPagedTableEntities.iterator()).thenReturn(mockIterator);
        when(blobContainerClient.listBlobs(any(ListBlobsOptions.class), any(Duration.class))).thenReturn(mockPagedTableEntities);

        try {
            blobStore.undeleteFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(404, ex.getError().getCode());
            assertEquals(ex.getError().getMessage(),"No items found");
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void undeleteFromStorageContainer_InternalError_CorruptData() {
        Iterator mockIterator = mock(Iterator.class);
        when(mockIterator.hasNext()).thenReturn(true);
        when(mockIterator.next()).thenReturn(mock(BlobItem.class));

        PagedIterable mockPagedTableEntities = mock(PagedIterable.class);
        when(mockPagedTableEntities.iterator()).thenReturn(mockIterator);
        when(blobContainerClient.listBlobs(any(ListBlobsOptions.class), any(Duration.class))).thenReturn(mockPagedTableEntities);

        try {
            blobStore.undeleteFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(500, ex.getError().getCode());
            assertEquals(ex.getError().getMessage(),"Corrupt data");
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void undeleteFromStorageContainer_InternalError_CopyJobFailed() {
        Iterator<BlobItem> mockIterator = mock(Iterator.class);
        when(mockIterator.hasNext()).thenReturn(true);
        BlobItem blobItem = new BlobItem();
        blobItem.setVersionId("version1");
        blobItem.setName("filePath");
        when(mockIterator.next()).thenReturn(blobItem);

        PagedIterable<BlobItem> blobItems = mock(PagedIterable.class);
        when(blobItems.iterator()).thenReturn(mockIterator);

        when(blobContainerClient.listBlobs(any(ListBlobsOptions.class), any(Duration.class))).thenReturn(blobItems);
        when(blobContainerClient.getBlobVersionClient("filePath", "version1")).thenReturn(blobClient);
        when(blobContainerClient.getBlobClient("filePath")).thenReturn(blobClient);

        SyncPoller<BlobCopyInfo, Void> poller = mock(SyncPoller.class);
        PollResponse<BlobCopyInfo> poll = new PollResponse<>(LongRunningOperationStatus.FAILED, null, null);
        when(blobClient.beginCopy(any(), any())).thenReturn(poller);
        when(poller.waitForCompletion(any(Duration.class))).thenReturn(poll);
        when(blobClient.exists()).thenReturn(true);

        try {
            blobStore.undeleteFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(500, ex.getError().getCode());
            assertEquals(ex.getError().getMessage(),"Copy job couldn't finish");
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void undeleteFromStorageContainer_Success() {
        Iterator<BlobItem> mockIterator = mock(Iterator.class);
        when(mockIterator.hasNext()).thenReturn(true);
        BlobItem blobItem = new BlobItem();
        blobItem.setVersionId("version1");
        blobItem.setName("filePath");
        when(mockIterator.next()).thenReturn(blobItem);

        PagedIterable<BlobItem> blobItems = mock(PagedIterable.class);
        when(blobItems.iterator()).thenReturn(mockIterator);

        when(blobContainerClient.listBlobs(any(ListBlobsOptions.class), any(Duration.class))).thenReturn(blobItems);
        when(blobContainerClient.getBlobVersionClient("filePath", "version1")).thenReturn(blobClient);
        when(blobContainerClient.getBlobClient("filePath")).thenReturn(blobClient);

        SyncPoller<BlobCopyInfo, Void> poller = mock(SyncPoller.class);
        PollResponse<BlobCopyInfo> poll = new PollResponse<>(LongRunningOperationStatus.SUCCESSFULLY_COMPLETED, null, null);
        when(blobClient.beginCopy(any(), any())).thenReturn(poller);
        when(poller.waitForCompletion(any(Duration.class))).thenReturn(poll);
        when(blobClient.exists()).thenReturn(true);

        try {
            blobStore.undeleteFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (Exception ex) {
            fail("should not get different error code");
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
    public void writeToStorageContainer_ErrorCreatingBlobContainerClient_System() {
        doThrow(BlobStorageException.class).when(blobServiceClientFactory).getSystemBlobServiceClient();
        try {
            blobStore.writeToStorageContainer(FILE_PATH, CONTENT, STORAGE_CONTAINER_NAME);
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
    public void writeToStorageContainer_InternalError_System() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.INTERNAL_ERROR);
        doThrow(exception).when(blockBlobClient).upload(any(), anyLong(), eq(true));
        try {
            blobStore.writeToStorageContainer(FILE_PATH, CONTENT, STORAGE_CONTAINER_NAME);
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

    @Test
    public void writeToStorageContainer_System_Success() {
        doReturn(blockBlobItem).when(blockBlobClient).upload(any(), anyLong(), eq(true));
        try {
            blobStore.writeToStorageContainer(FILE_PATH, CONTENT, STORAGE_CONTAINER_NAME);
        } catch (Exception ex) {
            fail("should not get any exception.");
        }
    }

    @Test
    public void createBlobContainer_ServerBusyError() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.SERVER_BUSY);
        doThrow(exception).when(blobServiceClient).createBlobContainer(anyString());
        String containerName = "containerName";
        try {
            blobStore.createBlobContainer(PARTITION_ID, containerName);
        } catch (AppException ex) {
            assertEquals(503, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void deleteBlobContainer_ServerBusyError() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.SERVER_BUSY);
        doThrow(exception).when(blobServiceClient).deleteBlobContainer(anyString());
        String containerName = "containerName";
        try {
            blobStore.deleteBlobContainer(PARTITION_ID, containerName);
        } catch (AppException ex) {
            assertEquals(503, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void readFromStorageContainer_ServerBusyError() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.SERVER_BUSY);
        doThrow(exception).when(blockBlobClient).download(any());
        try {
            String content = blobStore.readFromStorageContainer(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(503, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void writeToStorageContainer_ServerBusyError() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.SERVER_BUSY);
        doThrow(exception).when(blockBlobClient).upload(any(), anyLong(), eq(true));
        try {
            blobStore.writeToStorageContainer(PARTITION_ID, FILE_PATH, CONTENT, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(503, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void deleteFromStorageContainer_ServerBusyError() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.SERVER_BUSY);
        doThrow(exception).when(blockBlobClient).delete();
        try {
            blobStore.deleteFromStorageContainer(FILE_PATH, STORAGE_CONTAINER_NAME);
        } catch (AppException ex) {
            assertEquals(503, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void copyFile_Success() {
        String copyId = "copyId";
        doReturn(copyId).when(blobCopyInfo).getCopyId();
        doReturn(CopyStatusType.SUCCESS).when(blobCopyInfo).getCopyStatus();
        doReturn(blobCopyInfo).when(pollResponse).getValue();
        doReturn(pollResponse).when(syncPoller).waitForCompletion();
        doReturn(syncPoller).when(blockBlobClient).beginCopy(SOURCE_FILE_URL, Duration.ofSeconds(1));

        BlobCopyInfo copyInfo = blobStore.copyFile(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME, SOURCE_FILE_URL);
        assertEquals(copyInfo.getCopyId(), copyId);
    }

    @Test
    public void copyFile_Failure() {
        doReturn(null).when(pollResponse).getValue();
        doReturn(pollResponse).when(syncPoller).waitForCompletion();
        doReturn(syncPoller).when(blockBlobClient).beginCopy(SOURCE_FILE_URL, Duration.ofSeconds(1));

        BlobCopyInfo copyInfo = blobStore.copyFile(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME, SOURCE_FILE_URL);
        assertEquals(copyInfo, null);
    }

    @Test
    public void createBlobContainer_Failure() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.INTERNAL_ERROR);
        doThrow(exception).when(blobServiceClient).createBlobContainer(anyString());
        String containerName = "containerName";
        try {
            blobStore.createBlobContainer(PARTITION_ID, containerName);
        } catch (AppException ex) {
            assertEquals(500, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void createBlobContainer_Success() {
        String containerName = "containerName";
        boolean status = blobStore.createBlobContainer(PARTITION_ID, containerName);

        ArgumentCaptor<String> containerNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(blobServiceClient).createBlobContainer(containerNameCaptor.capture());

        assertEquals(containerNameCaptor.getValue(), containerName);
        assertEquals(status, true);
    }

    @Test
    public void deleteBlobContainer_Failure() {
        BlobStorageException exception = mockStorageException(BlobErrorCode.INTERNAL_ERROR);
        doThrow(exception).when(blobServiceClient).deleteBlobContainer(anyString());
        String containerName = "containerName";
        try {
            blobStore.deleteBlobContainer(PARTITION_ID, containerName);
        } catch (AppException ex) {
            assertEquals(500, ex.getError().getCode());
        } catch (Exception ex) {
            fail("should not get different error code");
        }
    }

    @Test
    public void deleteBlobContainer_Success() {
        String containerName = "containerName";
        boolean status = blobStore.deleteBlobContainer(PARTITION_ID, containerName);

        ArgumentCaptor<String> containerNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(blobServiceClient).deleteBlobContainer(containerNameCaptor.capture());

        assertEquals(containerNameCaptor.getValue(), containerName);
        assertEquals(status, true);
    }

    @Test
    public void getSasToken_NullSasTokenObtained() {
        int expiryDays = 1;
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(expiryDays);
        String sasToken = blobStore.getSasToken(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME, expiryTime, blobSasPermission);
        assertNull(sasToken);
    }

    @Test
    public void getSasToken_whenBlobSasTokenProvided_thenReturnsValidSasToken() {
        String blobSasToken = "blobSasToken";

        doReturn(blobSasToken).when(blockBlobClient).generateSas(any(BlobServiceSasSignatureValues.class));

        int expiryDays = 1;
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(expiryDays);
        BlobSasPermission blobSasPermission = (new BlobSasPermission()).setReadPermission(true).setCreatePermission(true);

        String obtainedBlobSasToken = blobStore.getSasToken(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME, expiryTime, blobSasPermission);

        ArgumentCaptor<BlobServiceSasSignatureValues> blobServiceSasSignatureValuesArgumentCaptor = ArgumentCaptor.forClass(BlobServiceSasSignatureValues.class);
        verify(blockBlobClient).generateSas(blobServiceSasSignatureValuesArgumentCaptor.capture());

        assertEquals(blobSasPermission.toString(), blobServiceSasSignatureValuesArgumentCaptor.getValue().getPermissions());
        assertEquals(expiryTime, blobServiceSasSignatureValuesArgumentCaptor.getValue().getExpiryTime());
        assertEquals(blobSasToken, obtainedBlobSasToken);
    }

    @Test
    public void generatePreSignedURLForContainer_NullPreSignedTokenObtained() {
        int expiryDays = 1;
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(expiryDays);
        BlobContainerSasPermission blobContainerSasPermission = (new BlobContainerSasPermission()).setReadPermission(true).setCreatePermission(true);
        String obtainedPreSignedUrl = blobStore.generatePreSignedURL(PARTITION_ID, STORAGE_CONTAINER_NAME, expiryTime, blobContainerSasPermission);
        assertEquals("null?null", obtainedPreSignedUrl);
    }

    @Test
    public void generatePreSignedURLForContainer_whenContainerPreSignedUrl_thenReturnsValidSasToken() {
        String containerSasToken = "containerSasToken";
        String containerUrl = "containerUrl";
        String containerPreSignedUrl = containerUrl + "?" + containerSasToken;

        doReturn(containerSasToken).when(blobContainerClient).generateSas(any(BlobServiceSasSignatureValues.class));
        doReturn(containerUrl).when(blobContainerClient).getBlobContainerUrl();

        int expiryDays = 1;
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(expiryDays);
        BlobContainerSasPermission blobContainerSasPermission = (new BlobContainerSasPermission()).setReadPermission(true).setCreatePermission(true);
        String obtainedPreSignedUrl = blobStore.generatePreSignedURL(PARTITION_ID, STORAGE_CONTAINER_NAME, expiryTime, blobContainerSasPermission);

        ArgumentCaptor<BlobServiceSasSignatureValues> blobServiceSasSignatureValuesArgumentCaptor = ArgumentCaptor.forClass(BlobServiceSasSignatureValues.class);
        verify(blobContainerClient).generateSas(blobServiceSasSignatureValuesArgumentCaptor.capture());

        assertEquals(blobContainerSasPermission.toString(), blobServiceSasSignatureValuesArgumentCaptor.getValue().getPermissions());
        assertEquals(expiryTime, blobServiceSasSignatureValuesArgumentCaptor.getValue().getExpiryTime());
        assertEquals(containerPreSignedUrl, obtainedPreSignedUrl);
    }

    @Test
    public void generatePreSignedUrlWithUserDelegationSas_NullPreSignedTokenObtained() {
        int expiryDays = 1;
        OffsetDateTime startTime = OffsetDateTime.now();
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(expiryDays);
        BlobContainerSasPermission blobContainerSasPermission = (new BlobContainerSasPermission()).setReadPermission(true).setCreatePermission(true);
        String obtainedPreSignedUrl = blobStore.generatePreSignedUrlWithUserDelegationSas(PARTITION_ID, STORAGE_CONTAINER_NAME, startTime, expiryTime, blobContainerSasPermission);
        assertEquals("null?null", obtainedPreSignedUrl);
    }

    @Test
    public void generatePreSignedURLlWithUserDelegationSas_whenContainerPreSignedUrl_thenReturnsValidSasToken() {
        UserDelegationKey userDelegationKey = mock(UserDelegationKey.class);

        String containerSasToken = "containerSasToken";
        String containerUrl = "containerUrl";
        String containerPreSignedUrl = containerUrl + "?" + containerSasToken;

        doReturn(userDelegationKey).when(blobServiceClient).getUserDelegationKey(any(OffsetDateTime.class), any(OffsetDateTime.class));
        doReturn(containerUrl).when(blobContainerClient).getBlobContainerUrl();
        doReturn(containerSasToken).when(blobContainerClient).generateUserDelegationSas(any(BlobServiceSasSignatureValues.class), any(UserDelegationKey.class));

        int expiryDays = 1;
        OffsetDateTime startTime = OffsetDateTime.now();
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(expiryDays);
        BlobContainerSasPermission blobContainerSasPermission = (new BlobContainerSasPermission()).setReadPermission(true).setCreatePermission(true);
        String obtainedPreSignedUrl = blobStore.generatePreSignedUrlWithUserDelegationSas(PARTITION_ID, STORAGE_CONTAINER_NAME, startTime, expiryTime, blobContainerSasPermission);

        ArgumentCaptor<BlobServiceSasSignatureValues> blobServiceSasSignatureValuesArgumentCaptor = ArgumentCaptor.forClass(BlobServiceSasSignatureValues.class);
        ArgumentCaptor<UserDelegationKey> userDelegationKeyArgumentCaptor = ArgumentCaptor.forClass(UserDelegationKey.class);
        verify(blobContainerClient).generateUserDelegationSas(blobServiceSasSignatureValuesArgumentCaptor.capture(), userDelegationKeyArgumentCaptor.capture());

        assertEquals(blobContainerSasPermission.toString(), blobServiceSasSignatureValuesArgumentCaptor.getValue().getPermissions());
        assertEquals(userDelegationKey, userDelegationKeyArgumentCaptor.getValue());
        assertEquals(startTime, blobServiceSasSignatureValuesArgumentCaptor.getValue().getStartTime());
        assertEquals(expiryTime, blobServiceSasSignatureValuesArgumentCaptor.getValue().getExpiryTime());
        assertEquals(containerPreSignedUrl, obtainedPreSignedUrl);
    }

    @Test
    public void generatePreSignedUrlWithUserDelegationSasBlockBlob_NullPreSignedTokenObtained() {
        int expiryDays = 1;
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(expiryDays);
        BlobSasPermission blobSasPermission = (new BlobSasPermission()).setReadPermission(true).setCreatePermission(true);
        String obtainedPreSignedUrl = blobStore.generatePreSignedUrlWithUserDelegationSas(PARTITION_ID, STORAGE_CONTAINER_NAME, FILE_PATH, expiryTime, blobSasPermission);
        assertEquals("null?null", obtainedPreSignedUrl);
    }

    @Test
    public void generatePreSignedURLlWithUserDelegationSasBlockBlob_whenContainerPreSignedUrl_thenReturnsValidSasToken() {
        UserDelegationKey userDelegationKey = mock(UserDelegationKey.class);

        String containerSasToken = "containerSasToken";
        String containerUrl = "containerUrl";
        String containerPreSignedUrl = containerUrl + "?" + containerSasToken;

        doReturn(userDelegationKey).when(blobServiceClient).getUserDelegationKey(any(OffsetDateTime.class), any(OffsetDateTime.class));
        doReturn(containerUrl).when(blockBlobClient).getBlobUrl();
        doReturn(containerSasToken).when(blockBlobClient).generateUserDelegationSas(any(BlobServiceSasSignatureValues.class), any(UserDelegationKey.class));

        int expiryDays = 1;
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(expiryDays);
        BlobSasPermission blobSasPermission = (new BlobSasPermission()).setReadPermission(true).setCreatePermission(true);
        String obtainedPreSignedUrl = blobStore.generatePreSignedUrlWithUserDelegationSas(PARTITION_ID, STORAGE_CONTAINER_NAME, FILE_PATH, expiryTime, blobSasPermission);

        ArgumentCaptor<BlobServiceSasSignatureValues> blobServiceSasSignatureValuesArgumentCaptor = ArgumentCaptor.forClass(BlobServiceSasSignatureValues.class);
        ArgumentCaptor<UserDelegationKey> userDelegationKeyArgumentCaptor = ArgumentCaptor.forClass(UserDelegationKey.class);
        verify(blockBlobClient).generateUserDelegationSas(blobServiceSasSignatureValuesArgumentCaptor.capture(), userDelegationKeyArgumentCaptor.capture());

        assertEquals(userDelegationKey, userDelegationKeyArgumentCaptor.getValue());
        assertEquals(expiryTime, blobServiceSasSignatureValuesArgumentCaptor.getValue().getExpiryTime());
        assertEquals(containerPreSignedUrl, obtainedPreSignedUrl);
    }

    @Test
    public void generatePreSignedURLForBlob_whenBlobPreSignedUrl_thenReturnsValidSasToken() {
        String blobSasToken = "blobSasToken";
        String blobUrl = "blobUrl";
        String blobPreSignedUrl = blobUrl + "?" + blobSasToken;

        doReturn(blobSasToken).when(blockBlobClient).generateSas(any(BlobServiceSasSignatureValues.class));
        doReturn(blobUrl).when(blockBlobClient).getBlobUrl();

        int expiryDays = 1;
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(expiryDays);
        BlobSasPermission blobSasPermission = (new BlobSasPermission()).setReadPermission(true).setCreatePermission(true);
        String obtainedPreSignedUrl = blobStore.generatePreSignedURL(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME, expiryTime, blobSasPermission, FILE_NAME, FILE_CONTENT_TYPE);

        ArgumentCaptor<BlobServiceSasSignatureValues> blobServiceSasSignatureValuesArgumentCaptor = ArgumentCaptor.forClass(BlobServiceSasSignatureValues.class);
        verify(blockBlobClient).generateSas(blobServiceSasSignatureValuesArgumentCaptor.capture());

        assertEquals(blobSasPermission.toString(), blobServiceSasSignatureValuesArgumentCaptor.getValue().getPermissions());
        assertEquals(expiryTime, blobServiceSasSignatureValuesArgumentCaptor.getValue().getExpiryTime());
        assertEquals(blobPreSignedUrl, obtainedPreSignedUrl);
    }

    @Test
    public void generatePreSignedURLWithUserDelegationSasForBlob_NullPreSignedTokenObtained() {
        int expiryDays = 1;
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(expiryDays);
        BlobSasPermission blobSasPermission = (new BlobSasPermission()).setReadPermission(true).setCreatePermission(true);
        String obtainedPreSignedUrl = blobStore.generatePreSignedUrlWithUserDelegationSas(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME, expiryTime, blobSasPermission, FILE_NAME, FILE_CONTENT_TYPE);
        assertEquals("null?null", obtainedPreSignedUrl);
    }

    @Test
    public void generatePreSignedURLWithUserDelegationSasForBlob_whenBlobPreSignedUrl_thenReturnsValidSasToken() {
        String blobSasToken = "blobSasToken";
        String blobUrl = "blobUrl";
        String blobPreSignedUrl = blobUrl + "?" + blobSasToken;
        UserDelegationKey userDelegationKey = mock(UserDelegationKey.class);
        doReturn(userDelegationKey).when(blobServiceClient).getUserDelegationKey(any(OffsetDateTime.class), any(OffsetDateTime.class));
        doReturn(blobUrl).when(blockBlobClient).getBlobUrl();
        doReturn(blobSasToken).when(blockBlobClient).generateUserDelegationSas(any(BlobServiceSasSignatureValues.class), any(UserDelegationKey.class));

        int expiryDays = 1;
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(expiryDays);
        BlobSasPermission blobSasPermission = (new BlobSasPermission()).setReadPermission(true).setCreatePermission(true);
        String obtainedPreSignedUrl = blobStore.generatePreSignedUrlWithUserDelegationSas(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME, expiryTime, blobSasPermission, FILE_NAME, FILE_CONTENT_TYPE);

        ArgumentCaptor<BlobServiceSasSignatureValues> blobServiceSasSignatureValuesArgumentCaptor = ArgumentCaptor.forClass(BlobServiceSasSignatureValues.class);
        verify(blockBlobClient).generateUserDelegationSas(blobServiceSasSignatureValuesArgumentCaptor.capture(), eq(userDelegationKey));

        assertEquals(blobSasPermission.toString(), blobServiceSasSignatureValuesArgumentCaptor.getValue().getPermissions());
        assertEquals(expiryTime, blobServiceSasSignatureValuesArgumentCaptor.getValue().getExpiryTime());
        assertEquals(blobPreSignedUrl, obtainedPreSignedUrl);
    }

    @Test
    public void generatePreSignedURLForBlob_NullPreSignedTokenObtained() {
        int expiryDays = 1;
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(expiryDays);
        BlobSasPermission blobSasPermission = (new BlobSasPermission()).setReadPermission(true).setCreatePermission(true);
        String obtainedPreSignedUrl = blobStore.generatePreSignedURL(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME, expiryTime, blobSasPermission, FILE_NAME, FILE_CONTENT_TYPE);
        assertEquals("null?null", obtainedPreSignedUrl);
    }

    @Test
    public void getBlobProperties_Success() {
        BlobProperties blobProperties = blobStore.readBlobProperties(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        assertNotNull(blobProperties);
    }
    @Test
    public void getBlobInputStream_Success() {
        BlobInputStream blobInputStream = blobStore.getBlobInputStream(PARTITION_ID, FILE_PATH, STORAGE_CONTAINER_NAME);
        assertNotNull(blobInputStream);
    }

    private BlobStorageException mockStorageException(BlobErrorCode errorCode) {
        BlobStorageException mockException = mock(BlobStorageException.class);
        lenient().when(mockException.getErrorCode()).thenReturn(errorCode);
        return mockException;
    }
}
