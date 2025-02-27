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

package org.opengroup.osdu.azure.publisherFacade;

import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.eventgrid.EventGridTopicStore;
import org.opengroup.osdu.azure.publisherFacade.models.PubSubAttributesBuilder;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
public class EventGridPublisherTest {
    private static final String DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID = "data-partition-account-id";
    private static final String CORRELATION_ID = "correlation-id";
    private static final String PARTITION_ID = "partition-id";
    private static final String EVENT_SUBJECT = "subject";
    private static final String EVENT_TYPE = "type";
    private static final String EVENT_DATA_VERSION = "1.0";
    private static final String EVENT_GRID_TOPIC_NAME = "recordstopic";
    @Mock
    private DpsHeaders dpsHeaders;
    @Mock
    private Object batch;
    @Mock
    private PublisherInfo publisherInfo;
    @Mock
    private JaxRsDpsLog logger;
    @Mock
    private EventGridTopicStore eventGridTopicStore;
    @InjectMocks
    private EventGridPublisher sut;

    @BeforeEach
    public void init() throws ServiceBusException, InterruptedException {
        initMocks(this);
        Mockito.lenient().doReturn(EVENT_SUBJECT).when(publisherInfo).getEventGridEventSubject();
        Mockito.lenient().doReturn(EVENT_TYPE).when(publisherInfo).getEventGridEventType();
        Mockito.lenient().doReturn(EVENT_DATA_VERSION).when(publisherInfo).getEventGridEventDataVersion();
        Mockito.lenient().doReturn(DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID).when(dpsHeaders).getPartitionIdWithFallbackToAccountId();
        Mockito.lenient().doReturn(PARTITION_ID).when(dpsHeaders).getPartitionId();
        Mockito.lenient().doReturn(CORRELATION_ID).when(dpsHeaders).getCorrelationId();
        Mockito.lenient().doReturn(EVENT_GRID_TOPIC_NAME).when(publisherInfo).getEventGridTopicName();
        Mockito.lenient().doReturn(batch).when(publisherInfo).getBatch();
    }

    @Test
    public void shouldPublishToEventGrid() {
        try {
            doNothing().when(eventGridTopicStore).publishToEventGridTopic(eq(PARTITION_ID), eq(EVENT_GRID_TOPIC_NAME), any());
            sut.publishToEventGrid(dpsHeaders, publisherInfo);
            verify(eventGridTopicStore, times(1)).publishToEventGridTopic(eq(PARTITION_ID), eq(EVENT_GRID_TOPIC_NAME), any());
        } catch (Exception e) {
            fail("Should not get any exception. Received " + e.getClass());
        }
    }

    @Test
    public void shouldThrowExceptionWhenPublishToEventGridFails() {
        try {
            doThrow(new Exception()).when(eventGridTopicStore).publishToEventGridTopic(eq(PARTITION_ID), eq(EVENT_GRID_TOPIC_NAME), any());
            sut.publishToEventGrid(dpsHeaders, publisherInfo);
            fail("Should throw exception");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }
}
