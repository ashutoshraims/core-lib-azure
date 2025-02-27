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
import com.azure.storage.blob.models.BlobCopyInfo;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.CopyStatusType;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.DependencyLogger;
import org.opengroup.osdu.azure.logging.DependencyPayload;
import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.slf4j.helpers.MessageFormatter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * A simpler interface to interact with Azure blob storage.
 * Usage examples:
 * <pre>
 * {@code
 *      @Autowired
 *      private BlobStore blobStore;
 *
 *      String readFromStorageContainerExample()
 *      {
 *          String content = blobStorage.readFromStorageContainer("dataPartitionId", "filePath", "containerName");
 *          if (content != null)
 *                 return content;
 *      }
 *
 *      void writeToStorageContainerExample()
 *      {
 *          blobStorage.writeToStorageContainer("dataPartitionId", "filePath", "content", "containerName");
 *      }
 *
 *      void deleteFromStorageContainerExample()
 *      {
 *          Boolean success = blobStorage.deleteFromStorageContainer("dataPartitionId", "filePath", "containerName");
 *      }
 *
 *      void getSasTokenExample()
 *      {
 *          int expiryDays = 7;
 *          OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(expiryDays);
 *          BlobSasPermission permissions = (new BlobSasPermission()).setReadPermission(true).setCreatePermission(true);
 *          String sasToken = blobStorage.getSasToken("dataPartitionId", "filePath", "containerName", expiryTime, permissions);
 *      }
 *
 *      void copyFileExample()
 *      {
 *          BlobCopyInfo copyInfo = blobStore.copyFile("dataPartitionId", "filePath", "containerName", "sourceFilePath");
 *          System.out.println("copy info " + copyInfo.getCopyStatus());
 *      }
 * }
 * </pre>
 */
public class BlobStore {
    private static final String LOGGER_NAME = BlobStore.class.getName();

    private static final int POLL_COMPLETION_TIMEOUT_IN_SECONDS = 10;
    private static final int BLOB_LIST_TIMEOUT_IN_SECONDS = 60;
    private IBlobServiceClientFactory blobServiceClientFactory;
    private ILogger logger;
    private DependencyLogger dependencyLogger;

    /**
     * Constructor to create BlobStore.
     *
     * @param factory        Factory that provides blob client.
     * @param loggerInstance logger instance to be used for logging.
     * @param depLogger      dependency logger instance to use for dependency logging.
     */
    public BlobStore(final IBlobServiceClientFactory factory, final ILogger loggerInstance, final DependencyLogger depLogger) {
        this.blobServiceClientFactory = factory;
        this.logger = loggerInstance;
        this.dependencyLogger = depLogger;
    }

    /**
     * @param filePath        Path of file to be read.
     * @param dataPartitionId Data partition id
     * @param containerName   Name of the storage container
     * @return the content of file with provided file path.
     */
    public String readFromStorageContainer(
            final String dataPartitionId,
            final String filePath,
            final String containerName) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        return this.readFromStorageContainerInternal(filePath, containerName, blobContainerClient);
    }

    /**
     * @param filePath        Path of file to be read.
     * @param containerName   Name of the storage container
     * @return the content of file with provided file path.
     */
    public String readFromStorageContainer(
            final String filePath,
            final String containerName) {
        BlobContainerClient blobContainerClient = getSystemBlobContainerClient(containerName);
        return this.readFromStorageContainerInternal(filePath, containerName, blobContainerClient);
    }

    /**
     * @param filePath        Path of file to be deleted.
     * @param dataPartitionId Data partition id
     * @param containerName   Name of the storage container
     * @return boolean indicating whether the deletion of given file was successful or not.
     */
    public boolean deleteFromStorageContainer(
            final String dataPartitionId,
            final String filePath,
            final String containerName) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        return this.deleteFromStorageContainerInternal(filePath, containerName, blobContainerClient);
    }

    /**
     * @param filePath        Path of file to be deleted.
     * @param containerName   Name of the storage container
     * @return boolean indicating whether the deletion of given file was successful or not.
     */
    public boolean deleteFromStorageContainer(
            final String filePath,
            final String containerName) {
        BlobContainerClient blobContainerClient = getSystemBlobContainerClient(containerName);
        return this.deleteFromStorageContainerInternal(filePath, containerName, blobContainerClient);
    }

    /**
     * @param filePath        Path of file to be undeleted.
     * @param dataPartitionId Data partition id
     * @param containerName   Name of the storage container
     * @return boolean indicating whether the undeletion of given file was successful or not.
     */
    public boolean undeleteFromStorageContainer(
            final String dataPartitionId,
            final String filePath,
            final String containerName) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        return this.undeleteFromStorageContainerInternal(filePath, containerName, blobContainerClient);
    }

    /**
     * @param filePath        Path of file to be written at.
     * @param content         Content to be written in the file.
     * @param dataPartitionId Data partition id
     * @param containerName   Name of the storage container
     */
    public void writeToStorageContainer(
            final String dataPartitionId,
            final String filePath,
            final String content,
            final String containerName) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        this.writeToStorageContainerInternal(filePath, content, containerName, blobContainerClient);
    }

    /**
     * @param filePath        Path of file to be written at.
     * @param content         Content to be written in the file.
     * @param containerName   Name of the storage container
     */
    public void writeToStorageContainer(
            final String filePath,
            final String content,
            final String containerName) {
        BlobContainerClient blobContainerClient = getSystemBlobContainerClient(containerName);
        this.writeToStorageContainerInternal(filePath, content, containerName, blobContainerClient);
    }

    /**
     * @param dataPartitionId Data partition id
     * @param containerName   Name of the storage container
     * @return boolean indicating whether the creation of the given container was successful or not.
     *         Throws exception in case of failure.
     */
    public boolean createBlobContainer(
            final String dataPartitionId,
            final String containerName) {
        BlobServiceClient blobServiceClient = blobServiceClientFactory.getBlobServiceClient(dataPartitionId);
        try {
            blobServiceClient.createBlobContainer(containerName);
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug("{}", MessageFormatter.format("Done creating container with name {}", containerName).getMessage());
            return true;
        } catch (BlobStorageException ex) {
            throw handleBlobStorageException(500, "Failed to create blob container", ex);
        }
    }

    /**
     * @param dataPartitionId Data partition id
     * @param containerName   Name of the storage container
     * @return boolean indicating whether the given container exists or not.
     *
     */
    public boolean checkIfBlobContainerExists(
            final String dataPartitionId,
            final String containerName) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        return blobContainerClient.exists();
    }

    /**
     * @param dataPartitionId Data partition id
     * @param containerName   Name of the storage container
     * @return boolean indicating whether the deletion of the given container was successful or not.
     *         Throws exception in case of failure.
     */
    public boolean deleteBlobContainer(
            final String dataPartitionId,
            final String containerName) {
        BlobServiceClient blobServiceClient = blobServiceClientFactory.getBlobServiceClient(dataPartitionId);
        try {
            blobServiceClient.deleteBlobContainer(containerName);
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug("{}", MessageFormatter.format("Done deleting container with name {}", containerName).getMessage());
            return true;
        } catch (BlobStorageException ex) {
            throw handleBlobStorageException(500, "Failed to delete blob container", ex);
        }
    }

    /**
     * @param dataPartitionId Data partition id
     * @param filePath        Path of file (blob) for which SAS token needs to be generated
     * @param containerName   Name of the storage container
     * @param expiryTime      Time after which the token expires
     * @param permissions     Permissions for the given blob
     * @return SAS token for the given blob
     */
    public String getSasToken(final String dataPartitionId, final String filePath, final String containerName, final OffsetDateTime expiryTime, final BlobSasPermission permissions) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();
        return generateSASToken(blockBlobClient, expiryTime, permissions);
    }

    /**
     * This method is used to generate pre-signed url for file (blob).
     * NOTE: Using the below method will require BlobServiceClient to be instantiated using StorageSharedKeyCredential
     * @param dataPartitionId Data partition id
     * @param filePath        Path of file (blob) for which SAS token needs to be generated
     * @param containerName   Name of the storage container
     * @param expiryTime      Time after which the token expires
     * @param permissions     Permissions for the given blob
     * @return Generates Pre-Signed URL for a given blob.
     */
    public String generatePreSignedURL(final String dataPartitionId, final String filePath, final String containerName, final OffsetDateTime expiryTime, final BlobSasPermission permissions) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();
        return blockBlobClient.getBlobUrl() + "?" + generateSASToken(blockBlobClient, expiryTime, permissions);
    }

    /**
     * This method is used to generate pre-signed url for file (blob). NOTE: Using
     * the below method will require BlobServiceClient to be instantiated using
     * StorageSharedKeyCredential
     * @param dataPartitionId Data partition id
     * @param filePath Path of file (blob) for which SAS token needs to be generated
     * @param fileName Name of the file
     * @param contentType Content type of the file
     * @param containerName Name of the storage container
     * @param expiryTime Time after which the token expires
     * @param permissions Permissions for the given blob
     * @return Generates Pre-Signed URL for a given blob.
     */
    public String generatePreSignedURL(final String dataPartitionId, final String filePath, final String containerName,
            final OffsetDateTime expiryTime, final BlobSasPermission permissions, final String fileName,
            final String contentType) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();
        return blockBlobClient.getBlobUrl() + "?"
                + generateSASToken(blockBlobClient, expiryTime, permissions, fileName, contentType);
    }

    /**
     * This method is used to generate pre-signed url for blob container.
     * NOTE: Using the below method will require BlobServiceClient to be instantiated using StorageSharedKeyCredential
     * @param dataPartitionId data partition id
     * @param containerName   Name of the storage container
     * @param expiryTime      Time after which the token expires
     * @param permissions     permissions for the given container
     * @return Generates pre-signed url for a given container
     */
    public String generatePreSignedURL(final String dataPartitionId, final String containerName, final OffsetDateTime expiryTime, final BlobContainerSasPermission permissions) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        return blobContainerClient.getBlobContainerUrl() + "?" + generateSASToken(blobContainerClient, expiryTime, permissions);
    }

    /**
     * Generates pre-signed url to a blob container using the user delegation key.
     *
     * @param dataPartitionId data partition id
     * @param containerName   Name of the storage container
     * @param startTime       Time after which the token is activated (null in case of instant activation)
     * @param expiryTime      Time after which the token expires
     * @param permissions     permissions for the given container
     * @return Generates pre-signed url for a given container
     */
    public String generatePreSignedUrlWithUserDelegationSas(final String dataPartitionId, final String containerName, final OffsetDateTime startTime, final OffsetDateTime expiryTime, final BlobContainerSasPermission permissions) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        BlobServiceClient blobServiceClient = blobServiceClientFactory.getBlobServiceClient(dataPartitionId);
        UserDelegationKey userDelegationKey = blobServiceClient.getUserDelegationKey(startTime, expiryTime);
        BlobServiceSasSignatureValues blobServiceSasSignatureValues = new BlobServiceSasSignatureValues(expiryTime, permissions).setStartTime(startTime);

        final long start = System.currentTimeMillis();
        String sasToken = blobContainerClient.generateUserDelegationSas(blobServiceSasSignatureValues, userDelegationKey);
        final long timeTaken = System.currentTimeMillis() - start;

        logDependency("GENERATE_PRESIGNED_URL_USER_DELEGATION_SAS", blobContainerClient.getBlobContainerName(), blobContainerClient.getBlobContainerUrl(), timeTaken, String.valueOf(HttpStatus.SC_OK), true);
        return blobContainerClient.getBlobContainerUrl() + "?" + sasToken;
    }

    /**
     * Generates pre-signed url to a blob block using the user delegation key.
     *
     * @param dataPartitionId data partition id
     * @param containerName   Name of the storage container
     * @param filePath        file Path
     * @param expiryTime      Time after which the token expires
     * @param permissions     permissions for the given container
     * @return Generates pre-signed url for a given container
     */
    public String generatePreSignedUrlWithUserDelegationSas(final String dataPartitionId, final String containerName, final String filePath, final OffsetDateTime expiryTime, final BlobSasPermission permissions) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        BlobServiceClient blobServiceClient = blobServiceClientFactory.getBlobServiceClient(dataPartitionId);
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();

        OffsetDateTime startTime = OffsetDateTime.now();
        UserDelegationKey userDelegationKey = blobServiceClient.getUserDelegationKey(startTime, expiryTime);
        BlobServiceSasSignatureValues blobServiceSasSignatureValues = new BlobServiceSasSignatureValues(expiryTime, permissions).setStartTime(startTime);

        final long start = System.currentTimeMillis();
        String sasToken = blockBlobClient.generateUserDelegationSas(blobServiceSasSignatureValues, userDelegationKey);
        final long timeTaken = System.currentTimeMillis() - start;

        logDependency("GENERATE_PRESIGNED_URL_USER_DELEGATION_SAS", blockBlobClient.getBlobName(), blockBlobClient.getBlobUrl(), timeTaken, String.valueOf(HttpStatus.SC_OK), true);
        return blockBlobClient.getBlobUrl() + "?" + sasToken;
    }

    /**
     * This method is used to generate pre-signed url for file (blob). NOTE: Using
     * the below method will require BlobServiceClient to be instantiated using
     * the user delegation key.
     * @param dataPartitionId Data partition id
     * @param filePath Path of file (blob) for which SAS token needs to be generated
     * @param fileName Name of the file
     * @param contentType Content type of the file
     * @param containerName Name of the storage container
     * @param expiryTime Time after which the token expires
     * @param permissions Permissions for the given blob
     * @return Generates Pre-Signed URL for a given blob.
     */
    public String generatePreSignedUrlWithUserDelegationSas(final String dataPartitionId, final String filePath, final String containerName,
                                       final OffsetDateTime expiryTime, final BlobSasPermission permissions, final String fileName,
                                       final String contentType) {
        BlobServiceClient blobServiceClient = blobServiceClientFactory.getBlobServiceClient(dataPartitionId);
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();

        OffsetDateTime startTime = OffsetDateTime.now();
        UserDelegationKey userDelegationKey = blobServiceClient.getUserDelegationKey(startTime, expiryTime);
        return blockBlobClient.getBlobUrl() + "?"
                + generateSASTokenWithUserDelegationKey(blockBlobClient, expiryTime, permissions, fileName, contentType, userDelegationKey);
    }

    /**
     * Method is used to copy a file specified at Source URL to the provided destination.
     *
     * @param dataPartitionId Data partition id
     * @param filePath        Path of file (blob) to which the file has to be copied
     * @param containerName   Name of the storage container
     * @param sourceUrl       URL of the file from where the file contents have to be copied
     * @return Blob Copy Final Result.
     */
    public BlobCopyInfo copyFile(final String dataPartitionId, final String filePath, final String containerName,
                                 final String sourceUrl) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();
        final long start = System.currentTimeMillis();
        SyncPoller<BlobCopyInfo, Void> result = blockBlobClient.beginCopy(sourceUrl, Duration.ofSeconds(1));
        BlobCopyInfo blobCopyInfo = result.waitForCompletion().getValue();
        final long timeTaken = System.currentTimeMillis() - start;
        final String target = MessageFormatter.arrayFormat("{}:{}/{}", new String[]{dataPartitionId, containerName, filePath}).getMessage();
        CopyStatusType status = blobCopyInfo == null ? CopyStatusType.FAILED : blobCopyInfo.getCopyStatus();
        logDependency("COPY_FILE", sourceUrl, target, timeTaken, status.toString(), status == CopyStatusType.SUCCESS);

        return blobCopyInfo;
    }

    /**
     * @param filePath              Path of file to be read.
     * @param containerName         Name of the storage container
     * @param blobContainerClient   Blob container client
     * @return the content of file with provided file path.
     */
    private String readFromStorageContainerInternal(
            final String filePath,
            final String containerName,
            final BlobContainerClient blobContainerClient) {
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();
        final long start = System.currentTimeMillis();
        int statusCode = HttpStatus.SC_OK;
        try (ByteArrayOutputStream downloadStream = new ByteArrayOutputStream()) {
            blockBlobClient.download(downloadStream);
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug("{}", MessageFormatter.format("Done reading from {}", filePath).getMessage());
            return downloadStream.toString(StandardCharsets.UTF_8.name());
        } catch (BlobStorageException ex) {
            statusCode = ex.getStatusCode();
            throw handleBlobStorageException(statusCode, "Failed to read specified blob", ex);
        } catch (UnsupportedEncodingException ex) {
            statusCode = HttpStatus.SC_BAD_REQUEST;
            throw handleBlobStoreException(400, MessageFormatter.format("Encoding was not correct for item with name={}", filePath).getMessage(), ex);
        } catch (IOException ex) {
            statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            throw handleBlobStoreException(500, MessageFormatter.format("Malformed document for item with name={}", filePath).getMessage(), ex);
        } finally {
            final long timeTaken = System.currentTimeMillis() - start;
            final String dependencyData = MessageFormatter.arrayFormat("{}/{}", new String[]{containerName, filePath}).getMessage();
            logDependency("READ_FROM_STORAGE_CONTAINER", dependencyData, dependencyData, timeTaken, String.valueOf(statusCode), statusCode == HttpStatus.SC_OK);
        }
    }

    /**
     * @param filePath        Path of file to be deleted.
     * @param containerName   Name of the storage container
     * @param blobContainerClient   Blob container client
     * @return boolean indicating whether the deletion of given file was successful or not.
     */
    private boolean deleteFromStorageContainerInternal(
            final String filePath,
            final String containerName,
            final BlobContainerClient blobContainerClient) {
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();
        final long start = System.currentTimeMillis();
        int statusCode = HttpStatus.SC_OK;
        try {
            blockBlobClient.delete();
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug("{}", MessageFormatter.format("Done deleting blob at {}", filePath).getMessage());
            return true;
        } catch (BlobStorageException ex) {
            statusCode = ex.getStatusCode();
            throw handleBlobStorageException(500, "Failed to delete blob", ex);
        } finally {
            final long timeTaken = System.currentTimeMillis() - start;
            final String dependencyData = MessageFormatter.arrayFormat("{}/{}", new String[]{containerName, filePath}).getMessage();
            logDependency("DELETE_FROM_STORAGE_CONTAINER", dependencyData, dependencyData, timeTaken, String.valueOf(statusCode), statusCode == HttpStatus.SC_OK);
        }
    }

    /**
     * @param filePath        Path of file to be deleted.
     * @param containerName   Name of the storage container
     * @param blobContainerClient   Blob container client
     * @return boolean indicating whether the deletion of given file was successful or not.
     */
    private boolean undeleteFromStorageContainerInternal(
            final String filePath,
            final String containerName,
            final BlobContainerClient blobContainerClient) {
        final long start = System.currentTimeMillis();
        int statusCode = HttpStatus.SC_OK;
        try {
            ListBlobsOptions listBlobsOptions = new ListBlobsOptions().setPrefix(filePath).setDetails(new BlobListDetails().setRetrieveSnapshots(true).setRetrieveVersions(true).setRetrieveDeletedBlobs(true));
            PagedIterable<BlobItem> blobItems = blobContainerClient.listBlobs(listBlobsOptions, Duration.ofSeconds(BLOB_LIST_TIMEOUT_IN_SECONDS));
            if (blobItems == null || !blobItems.iterator().hasNext()) {
                statusCode = HttpStatus.SC_NOT_FOUND;
                throw new AppException(statusCode, "Could not find any item at location " + filePath, "No items found");
            }
            for (BlobItem blobItem : blobItems) {
                if (blobItem.getVersionId() != null && filePath.equals(blobItem.getName())) {
                    BlobClient sourceBlobClient = blobContainerClient.getBlobVersionClient(blobItem.getName(), blobItem.getVersionId());
                    BlobClient destBlobClient = blobContainerClient.getBlobClient(filePath);
                    SyncPoller<BlobCopyInfo, Void> poller = destBlobClient.beginCopy(sourceBlobClient.getBlobUrl(), null);
                    PollResponse<BlobCopyInfo> poll = poller.waitForCompletion(Duration.ofSeconds(POLL_COMPLETION_TIMEOUT_IN_SECONDS));
                    if (destBlobClient.exists() && poll.getStatus().equals(LongRunningOperationStatus.SUCCESSFULLY_COMPLETED)) {
                        break;
                    } else {
                        statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
                        throw new AppException(statusCode, "Unknown error happened while restoring the blob", "Copy job couldn't finish");
                    }
                } else {
                    statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
                    throw new AppException(statusCode, "Unknown error happened while restoring the blob", "Corrupt data");
                }
            }
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug("Done undeleting blob at {}", filePath);
            return true;
        } catch (BlobStorageException ex) {
            statusCode = ex.getStatusCode();
            throw handleBlobStorageException(statusCode, "Failed to undelete blob", ex);
        } finally {
            final long timeTaken = System.currentTimeMillis() - start;
            final String dependencyData = MessageFormatter.arrayFormat("{}/{}", new String[]{containerName, filePath}).getMessage();
            logDependency("UNDELETE_FROM_STORAGE_CONTAINER", dependencyData, dependencyData, timeTaken, String.valueOf(statusCode), statusCode == HttpStatus.SC_OK);
        }
    }

    /**
     * @param filePath        Path of file to be written at.
     * @param content         Content to be written in the file.
     * @param containerName   Name of the storage container
     * @param blobContainerClient   Blob container client
     */
    private void writeToStorageContainerInternal(
            final String filePath,
            final String content,
            final String containerName,
            final BlobContainerClient blobContainerClient) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        int bytesSize = bytes.length;
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();

        final long start = System.currentTimeMillis();
        int statusCode = HttpStatus.SC_OK;
        try (ByteArrayInputStream dataStream = new ByteArrayInputStream(bytes)) {
            blockBlobClient.upload(dataStream, bytesSize, true);
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).debug("{}", MessageFormatter.format("Done uploading file content to {}", filePath).getMessage());
        } catch (BlobStorageException ex) {
            statusCode = ex.getStatusCode();
            throw handleBlobStorageException(500, "Failed to upload file content.", ex);
        } catch (IOException ex) {
            statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            throw handleBlobStoreException(500, MessageFormatter.format("Malformed document for item with name={}", filePath).getMessage(), ex);
        } finally {
            final long timeTaken = System.currentTimeMillis() - start;
            final String dependencyData = MessageFormatter.format("{}/{}", new String[]{containerName, filePath}).getMessage();
            logDependency("WRITE_TO_STORAGE_CONTAINER", dependencyData, dependencyData, timeTaken, String.valueOf(statusCode), statusCode == HttpStatus.SC_OK);
        }
    }

    /**
     * @param blockBlobClient Blob client
     * @param expiryTime      Time after which SAS Token expires
     * @param permissions     Permissions for the given blob
     * @return Generates SAS Token.
     */
    private String generateSASToken(final BlockBlobClient blockBlobClient, final OffsetDateTime expiryTime, final BlobSasPermission permissions) {
        BlobServiceSasSignatureValues blobServiceSasSignatureValues = new BlobServiceSasSignatureValues(expiryTime, permissions);
        final long start = System.currentTimeMillis();
        String sasToken = blockBlobClient.generateSas(blobServiceSasSignatureValues);
        final long timeTaken = System.currentTimeMillis() - start;
        logDependency("GENERATE_SAS_TOKEN", blockBlobClient.getBlobName(), blockBlobClient.getBlobUrl(), timeTaken, String.valueOf(HttpStatus.SC_OK), true);
        return sasToken;
    }

    /**
     * @param blockBlobClient Blob client
     * @param expiryTime Time after which SAS Token expires
     * @param permissions Permissions for the given blob
     * @param fileName name of the file
     * @param contentType Content type of the file
     * @return Generates SAS Token.
     */
    private String generateSASToken(final BlockBlobClient blockBlobClient, final OffsetDateTime expiryTime,
            final BlobSasPermission permissions, final String fileName, final String contentType) {
        BlobServiceSasSignatureValues blobServiceSasSignatureValues = new BlobServiceSasSignatureValues(expiryTime,
                permissions);
        blobServiceSasSignatureValues.setContentType(contentType);
        blobServiceSasSignatureValues.setContentDisposition("attachment; filename= " + fileName);
        final long start = System.currentTimeMillis();
        String sasToken = blockBlobClient.generateSas(blobServiceSasSignatureValues);
        final long timeTaken = System.currentTimeMillis() - start;
        logDependency("GENERATE_SAS_TOKEN", blockBlobClient.getBlobName(), blockBlobClient.getBlobUrl(), timeTaken,
                String.valueOf(HttpStatus.SC_OK), true);
        return sasToken;
    }

    /**
     * @param blockBlobClient Blob client
     * @param expiryTime Time after which SAS Token expires
     * @param permissions Permissions for the given blob
     * @param fileName name of the file
     * @param contentType Content type of the file
     * @param userDelegationKey user Delegation Key
     * @return Generates SAS Token.
     */
    private String generateSASTokenWithUserDelegationKey(final BlockBlobClient blockBlobClient, final OffsetDateTime expiryTime,
                                    final BlobSasPermission permissions, final String fileName, final String contentType, final UserDelegationKey userDelegationKey) {
        BlobServiceSasSignatureValues blobServiceSasSignatureValues = new BlobServiceSasSignatureValues(expiryTime,
                permissions);
        blobServiceSasSignatureValues.setContentType(contentType);
        blobServiceSasSignatureValues.setContentDisposition("attachment; filename= " + fileName);
        final long start = System.currentTimeMillis();
        String sasToken = blockBlobClient.generateUserDelegationSas(blobServiceSasSignatureValues, userDelegationKey);
        final long timeTaken = System.currentTimeMillis() - start;
        logDependency("GENERATE_SAS_TOKEN_WITH_USER_DELEGATION_key", blockBlobClient.getBlobName(), blockBlobClient.getBlobUrl(), timeTaken,
                String.valueOf(HttpStatus.SC_OK), true);
        return sasToken;
    }

    /**
     * @param client      Container client
     * @param expiryTime  Time after which SAS Token expires
     * @param permissions Permissions for the given container
     * @return Generates SAS Token.
     */
    private String generateSASToken(final BlobContainerClient client, final OffsetDateTime expiryTime, final BlobContainerSasPermission permissions) {
        BlobServiceSasSignatureValues blobServiceSasSignatureValues = new BlobServiceSasSignatureValues(expiryTime, permissions);
        final long start = System.currentTimeMillis();
        String sasToken = client.generateSas(blobServiceSasSignatureValues);
        final long timeTaken = System.currentTimeMillis() - start;
        logDependency("GENERATE_SAS_TOKEN", client.getBlobContainerName(), client.getBlobContainerUrl(), timeTaken, String.valueOf(HttpStatus.SC_OK), true);
        return sasToken;
    }

    /**
     * @param dataPartitionId Data partition id.
     * @param containerName   Name of storage container.
     * @return blob container client corresponding to the dataPartitionId.
     */
    private BlobContainerClient getBlobContainerClient(final String dataPartitionId, final String containerName) {
        try {
            BlobServiceClient serviceClient = blobServiceClientFactory.getBlobServiceClient(dataPartitionId);
            return serviceClient.getBlobContainerClient(containerName);
        } catch (AppException ex) {
            throw handleBlobStoreException(ex.getError().getCode(), "Error creating creating blob container client.", ex);
        } catch (Exception ex) {
            throw handleBlobStoreException(500, "Error creating creating blob container client.", ex);
        }
    }

    /**
     * @param containerName   Name of storage container.
     * @return blob container client corresponding for system resources.
     */
    private BlobContainerClient getSystemBlobContainerClient(final String containerName) {
        try {
            BlobServiceClient serviceClient = blobServiceClientFactory.getSystemBlobServiceClient();
            return serviceClient.getBlobContainerClient(containerName);
        } catch (AppException ex) {
            throw handleBlobStoreException(ex.getError().getCode(), "Error creating creating blob container client.", ex);
        } catch (Exception ex) {
            throw handleBlobStoreException(500, "Error creating creating blob container client.", ex);
        }
    }

    /**
     * Logs and returns instance of AppException.
     *
     * @param status       Response status code
     * @param errorMessage Error message
     * @param ex           Original exception
     * @return Instance of AppException
     */
    private AppException handleBlobStorageException(final int status, final String errorMessage, final BlobStorageException ex) {
        if (ex.getErrorCode().equals(BlobErrorCode.BLOB_NOT_FOUND)) {
            throw handleBlobStoreException(404, "Specified blob was not found", ex);
        }
        if (ex.getErrorCode().equals(BlobErrorCode.SERVER_BUSY)) {
            throw handleBlobStoreException(503, "The server is busy, retry this request later", ex);
        }

        throw handleBlobStoreException(status, errorMessage, ex);
    }

    /**
     * Logs and returns instance of AppException.
     *
     * @param status       Response status code
     * @param errorMessage Error message
     * @param ex           Original exception
     * @return Instance of AppException
     */
    private AppException handleBlobStoreException(final int status, final String errorMessage, final Exception ex) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).warn(MessageFormatter.format("{}", errorMessage).getMessage(), ex);
        return new AppException(status, errorMessage, ex.getMessage(), ex);
    }

    /**
     * Log dependency.
     *
     * @param name          the name of the command initiated with this dependency call
     * @param data          the command initiated by this dependency call
     * @param target        the target of this dependency call
     * @param timeTakenInMs the request duration in milliseconds
     * @param resultCode    the result code of the call
     * @param success       indication of successful or unsuccessful call
     */
    private void logDependency(final String name, final String data, final String target, final long timeTakenInMs, final String resultCode, final boolean success) {
        DependencyPayload payload = new DependencyPayload(name, data, Duration.ofMillis(timeTakenInMs), resultCode, success);
        payload.setType("BlobStore");
        payload.setTarget(target);
        dependencyLogger.logDependencyWithPayload(payload);
    }

    /**
     * Method is used to read the properties of a file specified at file path.
     *
     * @param dataPartitionId Data partition id
     * @param filePath        Path of file (blob) to get the properties
     * @param containerName   Name of the storage container
     * @return File (blob) properties Final Result.
     */
    public BlobProperties readBlobProperties(final String dataPartitionId, final String filePath, final String containerName) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();

        final long start = System.currentTimeMillis();
        BlobProperties blobProperties = blockBlobClient.getProperties();
        final long timeTaken = System.currentTimeMillis() - start;
        final String target = MessageFormatter.arrayFormat("{}:{}/{}", new String[]{dataPartitionId, containerName, filePath}).getMessage();
        logDependency("READ_FILE_PROPERTIES", filePath, target, timeTaken, String.valueOf(HttpStatus.SC_OK), true);

        return blobProperties;
    }

    /**
     *
     * @param dataPartitionId Data partition id
     * @param filePath        Path of file (blob) to get the input stream
     * @param containerName   Name of the storage container
     * @return  BlobInputStream Final Result.
     */
    public BlobInputStream getBlobInputStream(final String dataPartitionId, final String filePath, final String containerName) {
        BlobContainerClient blobContainerClient = getBlobContainerClient(dataPartitionId, containerName);
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();
        final long start = System.currentTimeMillis();
        BlobInputStream blobInputStream = blockBlobClient.openInputStream();
        final long timeTaken = System.currentTimeMillis() - start;
        final String target = MessageFormatter.arrayFormat("{}:{}/{}", new String[]{dataPartitionId, containerName, filePath}).getMessage();
        logDependency("READ_BLOB", filePath, target, timeTaken, String.valueOf(HttpStatus.SC_OK), true);

        return blobInputStream;
    }

}
