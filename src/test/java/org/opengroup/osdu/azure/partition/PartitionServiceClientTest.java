package org.opengroup.osdu.azure.partition;

import com.azure.security.keyvault.secrets.SecretClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.util.AzureServicePrincipleTokenService;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.IPartitionFactory;
import org.opengroup.osdu.core.common.partition.PartitionException;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.opengroup.osdu.core.common.partition.PartitionService;
import org.opengroup.osdu.core.common.partition.Property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PartitionServiceClientTest {

    @Mock
    private SecretClient secretClient;
    @Mock
    private IPartitionFactory partitionFactory;
    @Mock
    private AzureServicePrincipleTokenService tokenService;
    @Mock
    private DpsHeaders headers;
    @InjectMocks
    private PartitionServiceClient sut;

    private static final String PARTITION_ID = "dataPartitionId";

    @Test
    public void should_throwException_given_nullDataPartitionId() {
        try {
            sut.getPartition(null);
        } catch (NullPointerException ex) {
            assertEquals("partitionId cannot be null!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

    @Test
    public void should_throwException_given_emptyDataPartitionId() {
        try {
            sut.getPartition("");
        } catch (IllegalArgumentException ex) {
            assertEquals("partitionId cannot be empty!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

    @Test
    public void should_return_validPartition_given_partitionExists() throws PartitionException {
        PartitionService partitionService = mock(PartitionService.class);
        when(tokenService.getAuthorizationToken()).thenReturn("token");
        when(partitionFactory.create(any(DpsHeaders.class))).thenReturn(partitionService);

        final String storageAccountKey = "testStorageAccountKey";
        final String cosmosEndpoint = "testCosmosEndpoint";
        Map<String, Property> properties = new HashMap<>();
        properties.put("id", Property.builder().value(PARTITION_ID).build());
        properties.put("cosmos-endpoint", Property.builder().value(cosmosEndpoint).build());
        properties.put("storage-account-key", Property.builder().value(storageAccountKey).build());
        PartitionInfo partitionInfo = PartitionInfo.builder().properties(properties).build();
        when(partitionService.get(PARTITION_ID)).thenReturn(partitionInfo);

        PartitionInfoAzure partitionInfoAzure = sut.getPartition(PARTITION_ID);
        assertNotNull(partitionInfoAzure);
        assertEquals(cosmosEndpoint, partitionInfoAzure.getCosmosEndpoint());
        assertEquals(storageAccountKey, partitionInfoAzure.getStorageAccountKey());
        verify(headers, never()).put(eq(DpsHeaders.AUTHORIZATION), anyString());
    }

    @Test
    public void should_throwException_when_partitionNotFound() {
        PartitionService partitionService = mock(PartitionService.class);
        when(tokenService.getAuthorizationToken()).thenReturn("token");
        when(partitionFactory.create(any(DpsHeaders.class))).thenReturn(partitionService);

        try {
            when(partitionService.get(PARTITION_ID)).thenThrow(new PartitionException("unknown error", new HttpResponse()));
            sut.getPartition(PARTITION_ID);
        } catch (AppException ex) {
            assertEquals("unknown error", ex.getOriginalException().getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

    @Test
    public void should_return_ListPartitions() throws PartitionException {
        PartitionService partitionService = mock(PartitionService.class);
        when(tokenService.getAuthorizationToken()).thenReturn("token");
        when(partitionFactory.create(any(DpsHeaders.class))).thenReturn(partitionService);

        List<String> partitions = new ArrayList<>();
        partitions.add("tenant1");
        partitions.add("tenant2");
        when(partitionService.list()).thenReturn(partitions);

        List<String> partitionList = sut.listPartitions();
        assertNotNull(partitionList);
        assertEquals(partitions.size(), partitionList.size());
        verify(headers, never()).put(eq(DpsHeaders.AUTHORIZATION), anyString());
    }
}
