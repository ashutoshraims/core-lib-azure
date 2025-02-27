package org.opengroup.osdu.azure.servicebus;

import com.azure.identity.DefaultAzureCredential;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
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
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
public class TopicClientFactoryImplTest {

    @Mock
    private DefaultAzureCredential credential;
    @Mock
    private PartitionServiceClient partitionService;
    @Mock
    private Map<String, TopicClient> topicClientMap;
    @InjectMocks
    private TopicClientFactoryImpl sut;

    private static final String PARTITION_ID = "dataPartitionId";
    private static final String TOPIC_NAME = "testTopic";

    @BeforeEach
    void init() {
        initMocks(this);
    }

    @Test
    public void should_throwException_given_nullTopicName() {
        try {
            this.sut.getClient(PARTITION_ID, null);
        } catch (NullPointerException ex) {
            assertEquals("topicName cannot be null!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

    @Test
    public void should_throwException_given_emptyTopicName() {
        try {
            this.sut.getClient(PARTITION_ID, "");
        } catch (IllegalArgumentException ex) {
            assertEquals("topicName cannot be empty!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

    @Test
    public void should_return_cachedClient_when_cachedEarlier() throws ServiceBusException, InterruptedException {
        TopicClient topicClient = mock(TopicClient.class);
        final String cacheKey = String.format("%s-%s", PARTITION_ID, TOPIC_NAME);
        when(this.topicClientMap.containsKey(cacheKey)).thenReturn(true);
        when(this.topicClientMap.get(cacheKey)).thenReturn(topicClient);

        this.sut.getClient(PARTITION_ID, TOPIC_NAME);
        verify(this.partitionService, never()).getPartition(PARTITION_ID);
    }
}
