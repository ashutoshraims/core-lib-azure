package org.opengroup.osdu.azure.partition;

import com.azure.security.keyvault.secrets.SecretClient;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.util.AzureServicePrincipleTokenService;
import org.opengroup.osdu.common.Validators;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.IPartitionFactory;
import org.opengroup.osdu.core.common.partition.IPartitionProvider;
import org.opengroup.osdu.core.common.partition.PartitionException;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Partition service client implementation.
 */
@Service
@Lazy
public class PartitionServiceClient {

    @Autowired
    private IPartitionFactory partitionFactory;
    @Autowired
    private SecretClient secretClient;
    @Autowired
    private AzureServicePrincipleTokenService tokenService;
    @Autowired
    private DpsHeaders headers;

    private final Gson gson = new Gson();

    /**
     * Get partition info.
     *
     * @param partitionId Partition Id
     * @return Partition info
     * @throws AppException Exception thrown by {@link IPartitionFactory}
     */
    public PartitionInfoAzure getPartition(final String partitionId) throws AppException {
        Validators.checkNotNullAndNotEmpty(partitionId, "partitionId");
        Validators.checkValidDataPartition(partitionId);
        try {
            IPartitionProvider serviceClient = getServiceClient();
            PartitionInfo partitionInfo = serviceClient.get(partitionId);
            return convert(partitionInfo);
        } catch (PartitionException e) {
            throw new AppException(HttpStatus.SC_FORBIDDEN, "Service unavailable", String.format("Error getting partition info for data-partition: %s", partitionId), e);
        }
    }

    /**
     * @param partitionInfo generic partitionInfo object
     * @return Azure specific partitionInfo
     */
    private PartitionInfoAzure convert(final PartitionInfo partitionInfo) {
        JsonElement jsonElement = gson.toJsonTree(partitionInfo.getProperties());
        PartitionInfoAzure infoAzure = gson.fromJson(jsonElement, PartitionInfoAzure.class);
        infoAzure.configureSecretClient(secretClient);
        return infoAzure;
    }

    /**
     * List of all partitions.
     *
     * @return List of Partitions
     * @throws AppException Exception thrown by {@link IPartitionFactory}
     */
    public List<String> listPartitions() throws AppException {
        try {
            IPartitionProvider serviceClient = getServiceClient();
            return serviceClient.list();
        } catch (PartitionException e) {
            throw new AppException(HttpStatus.SC_FORBIDDEN, "Service unavailable", "Error getting list of partitions", e);
        }
    }

    /**
     * Get Service client for Partition Service.
     *
     * @return PartitionServiceClient
     */
    private IPartitionProvider getServiceClient() {
        DpsHeaders newHeaders = DpsHeaders.createFromMap(headers.getHeaders());
        newHeaders.put(DpsHeaders.AUTHORIZATION, "Bearer " + tokenService.getAuthorizationToken());
        return partitionFactory.create(newHeaders);
    }
}