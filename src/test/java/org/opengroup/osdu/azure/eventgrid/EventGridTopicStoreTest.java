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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.partition.EventGridTopicPartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceEventGridClient;
import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.partition.PartitionException;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
class EventGridTopicStoreTest {

    private static final String VALID_DATA_PARTIION_ID = "validDataPartitionId";
    private static final String VALID_TOPIC = "validTopic";
    private static final String INVALID_URI = "://invalidUri";
    private static final String VALID_KEY = "validkey";
    @Mock
    EventGridTopicClientFactoryImpl eventGridTopicClientFactory;
    @Mock
    EventGridClient eventGridClient;
    @InjectMocks
    EventGridTopicStore sut;
    @Mock
    private ILogger logger;
    @Mock
    private PartitionServiceEventGridClient partitionService;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    public void should_throwException_given_invalidURI() throws PartitionException {
        doReturn(EventGridTopicPartitionInfoAzure.builder()
                .topicName(INVALID_URI)
                .topicAccessKey(VALID_KEY).build())
                .when(this.partitionService).getEventGridTopicInPartition(anyString(), anyString());


        AppException appException = Assertions.assertThrows(AppException.class,
                () -> this.sut.publishToEventGridTopic(VALID_DATA_PARTIION_ID, VALID_TOPIC, new ArrayList<>()));

        assertEquals("PartitionInfo for Event Grid Topic " + VALID_TOPIC, appException.getError().getMessage());
        verify(this.eventGridClient, times(0)).publishEvents(any(), any());
    }

    @Test
    public void should_should_invoke_publishEvents() throws PartitionException {

        doReturn(EventGridTopicPartitionInfoAzure.builder()
                .topicName(VALID_TOPIC)
                .topicAccessKey(VALID_KEY).build())
                .when(this.partitionService).getEventGridTopicInPartition(anyString(), anyString());
        when(this.eventGridTopicClientFactory.getClient(VALID_DATA_PARTIION_ID, "validTopic")).thenReturn(this.eventGridClient);

        this.sut.publishToEventGridTopic(VALID_DATA_PARTIION_ID, "validTopic", new ArrayList<>());

        verify(this.eventGridClient, times(1)).publishEvents(any(), any());
    }
}
