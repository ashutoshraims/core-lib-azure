package org.opengroup.osdu.azure.cosmosdb;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosBulkExecutionOptions;
import com.azure.cosmos.models.CosmosBulkItemResponse;
import com.azure.cosmos.models.CosmosBulkOperationResponse;
import com.azure.cosmos.models.CosmosItemOperation;
import com.azure.cosmos.models.CosmosPatchOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.logging.DependencyLogger;
import org.opengroup.osdu.azure.logging.DependencyLoggingOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.opengroup.osdu.azure.logging.DependencyType.COSMOS_STORE;

@ExtendWith(MockitoExtension.class)
public class CosmosStoreBulkOperationsTest {

    private static final String COSMOS_DB = "cosmosdb";
    private static final String COLLECTION = "collection";
    private static final String DATA_PARTITION_ID = "data-partition-id";

    @Mock
    CosmosClient cosmosClient;
    @Mock
    CosmosContainer cosmosContainer;
    @Mock
    CosmosPatchOperations cosmosPatchOperations;
    @Mock
    CosmosItemOperation cosmosItemOperation;
    @Mock
    CosmosBulkExecutionOptions cosmosBulkExecutionOptions;
    @Mock
    CosmosBulkOperationResponse cosmosBulkOperationResponse;
    @Mock
    CosmosBulkItemResponse cosmosBulkItemResponse;
    @Mock
    CosmosDatabase cosmosDatabase;
    @Mock
    private DependencyLogger dependencyLogger;
    @Mock
    private ICosmosClientFactory cosmosClientFactory;
    @Captor
    ArgumentCaptor<DependencyLoggingOptions> loggingOptionsArgumentCaptor;
    @Captor
    ArgumentCaptor<List<CosmosItemOperation>> cosmosItemOperations;

    @InjectMocks
    private CosmosStoreBulkOperations sut;

    @BeforeEach
    void init() {
        openMocks(this);

        lenient().doReturn(cosmosClient).when(cosmosClientFactory).getClient(DATA_PARTITION_ID);
        lenient().doReturn(cosmosBulkItemResponse).when(cosmosBulkOperationResponse).getResponse();
        lenient().doReturn(cosmosItemOperation).when(cosmosBulkOperationResponse).getOperation();
        lenient().when(cosmosBulkOperationResponse.getResponse().isSuccessStatusCode()).thenReturn(true);
        lenient().when(cosmosItemOperation.getItem()).thenReturn("item");
        lenient().when(cosmosBulkItemResponse.getStatusCode()).thenReturn(200);
        lenient().when(cosmosBulkItemResponse.getRequestCharge()).thenReturn(0.0);
        when(cosmosClient.getDatabase(COSMOS_DB)).thenReturn(cosmosDatabase);
        when(cosmosDatabase.getContainer(COLLECTION)).thenReturn(cosmosContainer);
        cosmosBulkExecutionOptions.setMaxMicroBatchConcurrency(1);
    }

    @Test
    void bulkPatch_Success() {
        List<CosmosItemOperation> cosmosItemOperations = new ArrayList<>();
        cosmosItemOperations.add(cosmosItemOperation);
        List<CosmosBulkOperationResponse> bulkPatchResponse = new ArrayList<>();
        bulkPatchResponse.add(cosmosBulkOperationResponse);
        Map<String, CosmosPatchOperations> cosmosPatchOperationsPerDoc = new HashMap<>();
        cosmosPatchOperationsPerDoc.put("id1", cosmosPatchOperations);
        Map<String, String> partitionKeyForDoc = new HashMap<>();
        partitionKeyForDoc.put("id1", "id1");

        lenient().doReturn(bulkPatchResponse).when(cosmosContainer).executeBulkOperations(cosmosItemOperations, cosmosBulkExecutionOptions);

        this.sut.bulkPatchWithCosmosClient(DATA_PARTITION_ID, COSMOS_DB, COLLECTION, cosmosPatchOperationsPerDoc, partitionKeyForDoc, 1);

        verify(this.cosmosClientFactory, times(1)).getClient(DATA_PARTITION_ID);
        verify(dependencyLogger, times(1)).logDependency(loggingOptionsArgumentCaptor.capture());
        DependencyLoggingOptions actualLoggingOptions = loggingOptionsArgumentCaptor.getValue();
        verifyDependencyLogging(actualLoggingOptions, "PATCH_ITEMS", "partition_key=[id1]", "cosmosdb/collection", 0.0, 200, true);
    }

    @Test
    void bulkMultiPatch_Success() {
        Map<String, List<CosmosPatchOperations>> cosmosPatchOperationsPerDoc = new HashMap<>();
        cosmosPatchOperationsPerDoc.put("id1", new ArrayList<>(Arrays.asList(cosmosPatchOperations, cosmosPatchOperations)));
        Map<String, String> partitionKeyForDoc = new HashMap<>();
        partitionKeyForDoc.put("id1", "id1");
        List<CosmosBulkOperationResponse> bulkPatchResponse = new ArrayList<>();
        bulkPatchResponse.add(cosmosBulkOperationResponse);

        lenient().doReturn(bulkPatchResponse).when(cosmosContainer).executeBulkOperations(anyList(), any(CosmosBulkExecutionOptions.class));

        sut.bulkMultiPatchWithCosmosClient(DATA_PARTITION_ID, COSMOS_DB, COLLECTION, cosmosPatchOperationsPerDoc, partitionKeyForDoc, 1);
        verify(cosmosClientFactory).getClient(DATA_PARTITION_ID);
        verify(cosmosContainer).executeBulkOperations(cosmosItemOperations.capture(), any(CosmosBulkExecutionOptions.class));
        List<CosmosItemOperation> itemOperations = cosmosItemOperations.getValue();
        assertEquals(2, itemOperations.size());
        verify(dependencyLogger).logDependency(loggingOptionsArgumentCaptor.capture());
        DependencyLoggingOptions actualLoggingOptions = loggingOptionsArgumentCaptor.getValue();
        verifyDependencyLogging(actualLoggingOptions, "PATCH_ITEMS", "partition_key=[id1]", "cosmosdb/collection", 0.0, 200, true);
    }

    private void verifyDependencyLogging(DependencyLoggingOptions capturedLoggingOptions, String name, String data, String target, double requestCharge, int resultCode, boolean success) {
        assertEquals(COSMOS_STORE, capturedLoggingOptions.getType());
        assertEquals(name, capturedLoggingOptions.getName());
        assertEquals(data, capturedLoggingOptions.getData());
        assertEquals(target, capturedLoggingOptions.getTarget());
        assertEquals(requestCharge, capturedLoggingOptions.getRequestCharge());
        assertEquals(resultCode, capturedLoggingOptions.getResultCode());
        assertEquals(success, capturedLoggingOptions.isSuccess());
    }
}
