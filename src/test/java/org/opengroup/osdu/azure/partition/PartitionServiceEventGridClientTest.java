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
import org.opengroup.osdu.azure.cache.PartitionServiceEventGridCache;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.util.AzureServicePrincipleTokenService;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.IPartitionFactory;
import org.opengroup.osdu.core.common.partition.IPartitionProvider;
import org.opengroup.osdu.core.common.partition.PartitionException;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.opengroup.osdu.core.common.partition.Property;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @Mock
    private PartitionServiceEventGridCache partitionServiceEventGridCache;
    @Mock
    private MSIConfiguration msiConfiguration;

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
        doReturn(false).when(msiConfiguration).getIsEnabled();

        // Act
        Map<String, EventGridTopicPartitionInfoAzure> eventGridTopicPartitionInfoAzureMap =
                partitionServiceClientSpy.getAllRelevantEventGridTopicsInPartition("tenant1", "recordstopic");

        // Assert
        assertEquals(eventGridTopicPartitionInfoAzureMap.size(), 1);
        assertTrue(eventGridTopicPartitionInfoAzureMap.containsKey(topicId1));

        // Validate that the EventGridTopicPartitionInfo is mapped correctly.
        assertEquals(eventGridTopicPartitionInfoAzureMap.get(topicId1).getTopicName(), eventGridTopicName1);
        assertEquals(eventGridTopicPartitionInfoAzureMap.get(topicId1).getTopicAccessKey(), eventGridTopicAccessKey1);
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
        doReturn(false).when(msiConfiguration).getIsEnabled();

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

    @Test
    public void should_get_eventGridTopic_from_cache_when_available() throws PartitionException{
        String testPartitionId = "testPartition";
        String testTopicName = "testTopic";

        Map<String, EventGridTopicPartitionInfoAzure> testEventGridTopicPartitionInfoAzure = new HashMap<>();
        EventGridTopicPartitionInfoAzure testEventGrid = new EventGridTopicPartitionInfoAzure();
        testEventGrid.setTopicName(testTopicName);
        testEventGrid.setTopicAccessKey("testAccessKey");
        testEventGridTopicPartitionInfoAzure.put(testTopicName, testEventGrid);

        String cacheKey = String.format("%s-%s-eventGridTopicPartitionInfoAzure", testPartitionId, testTopicName);
        when(partitionServiceEventGridCache.containsKey(cacheKey)).thenReturn(true);
        when(partitionServiceEventGridCache.get(cacheKey)).thenReturn(testEventGridTopicPartitionInfoAzure);
        EventGridTopicPartitionInfoAzure result = sut.getEventGridTopicInPartition(testPartitionId, testTopicName);

        assertEquals(result.getTopicName(), testTopicName);
        assertEquals(result.getTopicAccessKey(), "testAccessKey");
    }

    @Test
    public void shouldNotModifyDpsHeaders() throws PartitionException {
        when(tokenService.getAuthorizationToken()).thenReturn("token");
        when(partitionFactory.create(any(DpsHeaders.class))).thenReturn(mock(IPartitionProvider.class));

        sut.getPartitionInfo("test");

        verify(headers, never()).put(DpsHeaders.AUTHORIZATION, "Bearer token");
    }
}
