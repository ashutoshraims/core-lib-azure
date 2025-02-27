
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

package org.opengroup.osdu.azure.eventgrid;

import com.microsoft.azure.eventgrid.EventGridClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.cache.EventGridTopicClientCache;
import org.opengroup.osdu.azure.di.EventGridTopicRetryConfiguration;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.partition.EventGridTopicPartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceEventGridClient;
import org.opengroup.osdu.core.common.partition.PartitionException;
import org.opengroup.osdu.core.common.partition.Property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventGridTopicClientFactoryImplTest {

    private static final String VALID_TOPIC_NAME = "eventgrid-validtopic";
    private static final String VALID_TOPICKEY_NAME = "eventgrid-validtopic-accesskey";
    private static final String VALID_DATA_PARTIION_ID = "validDataPartitionId";

    @Mock
    private PartitionServiceEventGridClient partitionService;

    @Mock
    private EventGridTopicRetryConfiguration eventGridTopicRetryConfiguration;

    @InjectMocks
    private EventGridTopicClientFactoryImpl sut;

    @Mock
    private EventGridTopicClientCache clientCache;

    @Mock
    private MSIConfiguration msiConfiguration;

    @Test
    public void should_throwException_given_nullDataPartitionId() {

        NullPointerException nullPointerException = Assertions.assertThrows(NullPointerException.class,
                () -> this.sut.getClient(null, "recordsChanged"));
        assertEquals("dataPartitionId cannot be null!", nullPointerException.getMessage());
    }

    @Test
    public void should_throwException_given_emptyDataPartitionId() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(IllegalArgumentException.class,
                () -> this.sut.getClient("", "recordsChanged"));
        assertEquals("dataPartitionId cannot be empty!", illegalArgumentException.getMessage());
    }

    @Test
    public void should_throwException_given_nullTopicName() {

        NullPointerException nullPointerException = Assertions.assertThrows(NullPointerException.class,
                () -> this.sut.getClient(VALID_DATA_PARTIION_ID, null));
        assertEquals("topicName cannot be null!", nullPointerException.getMessage());
    }

    @Test
    public void should_return_validClient_given_validPartitionId_without_retrytimeout() throws PartitionException {
        // Setup
        when(this.partitionService.getEventGridTopicInPartition(VALID_DATA_PARTIION_ID, "validtopic")).thenReturn(
                EventGridTopicPartitionInfoAzure.builder()
                        .topicName(VALID_TOPIC_NAME)
                        .topicAccessKey(VALID_TOPICKEY_NAME).build());

        when(this.clientCache.containsKey(any())).thenReturn(false);
        when(this.eventGridTopicRetryConfiguration.isTimeoutConfigured()).thenReturn(false);
        when(this.msiConfiguration.getIsEnabled()).thenReturn(false);

        // Act
        EventGridClient eventGridClient = this.sut.getClient(VALID_DATA_PARTIION_ID, "validtopic");

        // Assert
        assertNotNull(eventGridClient);
        verify(this.clientCache, times(1)).put(any(), any());
        verify(this.eventGridTopicRetryConfiguration,times(0)).getLongRunningOperationRetryTimeout();
    }

    @Test
    public void should_return_validClient_given_validPartitionId_with_retrytimeout() throws PartitionException {
        // Setup
        when(this.partitionService.getEventGridTopicInPartition(VALID_DATA_PARTIION_ID, "validtopic")).thenReturn(
                EventGridTopicPartitionInfoAzure.builder()
                        .topicName(VALID_TOPIC_NAME)
                        .topicAccessKey(VALID_TOPICKEY_NAME).build());

        when(this.clientCache.containsKey(any())).thenReturn(false);
        when(this.eventGridTopicRetryConfiguration.isTimeoutConfigured()).thenReturn(true);
        when(this.eventGridTopicRetryConfiguration.getLongRunningOperationRetryTimeout()).thenReturn(20);
        when(this.msiConfiguration.getIsEnabled()).thenReturn(false);

        // Act
        EventGridClient eventGridClient = this.sut.getClient(VALID_DATA_PARTIION_ID, "validtopic");

        // Assert
        assertNotNull(eventGridClient);
        assertEquals(eventGridClient.longRunningOperationRetryTimeout(),  20);
        verify(this.clientCache, times(1)).put(any(), any());
        verify(this.eventGridTopicRetryConfiguration,times(1)).getLongRunningOperationRetryTimeout();
    }
}

