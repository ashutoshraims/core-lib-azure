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

package org.opengroup.osdu.azure.partition;

import com.azure.security.keyvault.secrets.SecretClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.util.AzureServicePrincipleTokenService;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.IPartitionFactory;
import org.opengroup.osdu.core.common.partition.PartitionException;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.opengroup.osdu.core.common.partition.Property;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class PartitionServiceEventGridClientTest {
    private static final String PARTITION_ID = "dataPartitionId";
    @Mock
    private SecretClient secretClient;
    @Mock
    private IPartitionFactory partitionFactory;
    @Mock
    private AzureServicePrincipleTokenService tokenService;
    @Mock
    private DpsHeaders headers;
    @InjectMocks
    private PartitionServiceEventGridClient sut;

    @Test
    public void should_returnAllEventGridTopics_ListPartitions() throws PartitionException {
        // Setup
        final String eventGridTopicName1 = "testEventGridTopicName1";
        final String eventGridTopicAccessKey1 = "testEventGridTopicAccessKey1";
        final String eventGridTopicName2 = "testEventGridTopicName2";
        final String eventGridTopicAccessKey2 = "testEventGridTopicAccessKey2";
        final String topicId1 = "recordstopic";
        final String topicId2 = "testtopic";
        Map<String, Property> properties = new HashMap<>();
        properties.put("id", Property.builder().value(PARTITION_ID).build());

        // Valid property names
        properties.put("eventgrid-recordstopic", Property.builder().value(eventGridTopicName1).build());
        properties.put("eventgrid-recordstopic-accesskey", Property.builder().value(eventGridTopicAccessKey1).build());
        properties.put("eventgrid-testtopic", Property.builder().value(eventGridTopicName2).build());
        properties.put("eventgrid-testtopic-accesskey", Property.builder().value(eventGridTopicAccessKey2).build());
        // Invalid Names. These should not get picked.
        properties.put("event_grid-testtopic-accesskey", Property.builder().value(eventGridTopicName2).build());
        properties.put("eventgrid-testtopic-accesskey-", Property.builder().value(eventGridTopicName2).build());

        PartitionInfo partitionInfo = PartitionInfo.builder().properties(properties).build();
        PartitionServiceEventGridClient partitionServiceClientSpy = Mockito.spy(sut);
        doReturn(partitionInfo).when(partitionServiceClientSpy).getPartitionInfo(anyString());

        // Act
        Map<String, EventGridTopicPartitionInfoAzure> eventGridTopicPartitionInfoAzureMap =
                partitionServiceClientSpy.getAllEventGridTopicsInPartition("tenant1");

        // Assert
        assertEquals(eventGridTopicPartitionInfoAzureMap.size(), 2);
        assertTrue(eventGridTopicPartitionInfoAzureMap.containsKey(topicId1));
        assertTrue(eventGridTopicPartitionInfoAzureMap.containsKey(topicId2));

        // Validate that the EventGridTopicPartitionInfo is mapped correctly.
        assertEquals(eventGridTopicPartitionInfoAzureMap.get(topicId1).getTopicName(), eventGridTopicName1);
        assertEquals(eventGridTopicPartitionInfoAzureMap.get(topicId1).getTopicAccessKey(), eventGridTopicAccessKey1);
        assertEquals(eventGridTopicPartitionInfoAzureMap.get(topicId2).getTopicName(), eventGridTopicName2);
        assertEquals(eventGridTopicPartitionInfoAzureMap.get(topicId2).getTopicAccessKey(), eventGridTopicAccessKey2);
    }

    @Test
    public void should_throwWhenInvalid_getEventGridTopicInPartition() throws PartitionException {
        final String eventGridTopicName1 = "testEventGridTopicName1";
        final String eventGridTopicAccessKey1 = "testEventGridTopicAccessKey1";
        Map<String, Property> properties = new HashMap<>();
        properties.put("id", Property.builder().value(PARTITION_ID).build());

        // Valid property names
        properties.put("eventgrid-recordstopic", Property.builder().value(eventGridTopicName1).build());
        properties.put("eventgrid-recordstopic-accesskey", Property.builder().value(eventGridTopicAccessKey1).build());

        PartitionInfo partitionInfo = PartitionInfo.builder().properties(properties).build();
        PartitionServiceEventGridClient partitionServiceClientSpy = Mockito.spy(sut);
        doReturn(partitionInfo).when(partitionServiceClientSpy).getPartitionInfo(anyString());

        // Act
        EventGridTopicPartitionInfoAzure eventGridTopicPartitionInfoAzure =
                partitionServiceClientSpy.getEventGridTopicInPartition("tenant1", "recordstopic");

        // Assert
        assertEquals(eventGridTopicPartitionInfoAzure.getTopicName(), eventGridTopicName1);
        assertEquals(eventGridTopicPartitionInfoAzure.getTopicAccessKey(), eventGridTopicAccessKey1);

        // Assert negative
        AppException exception = assertThrows(AppException.class, () -> partitionServiceClientSpy.getEventGridTopicInPartition("tenant1", "recordschangedtopic"));
        assertEquals(500, exception.getError().getCode());
    }
}
