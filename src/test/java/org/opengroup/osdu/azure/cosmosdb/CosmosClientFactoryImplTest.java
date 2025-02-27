package org.opengroup.osdu.azure.cosmosdb;

import com.azure.cosmos.CosmosClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
public class CosmosClientFactoryImplTest {

    @Mock
    private Map<String, CosmosClient> cosmosClientMap;
    @Mock
    private PartitionServiceClient partitionService;
    @InjectMocks
    private CosmosClientFactoryImpl sut;

    private static final String PARTITION_ID = "dataPartitionId";

    @BeforeEach
    void init() {
        initMocks(this);
    }

    @Test
    public void should_throwException_given_nullDataPartitionId() {
        try {
            this.sut.getClient(null);
        } catch (NullPointerException ex) {
            assertEquals("dataPartitionId cannot be null!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

    @Test
    public void should_throwException_given_emptyDataPartitionId() {
        try {
            this.sut.getClient("");
        } catch (IllegalArgumentException ex) {
            assertEquals("dataPartitionId cannot be empty!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

}
