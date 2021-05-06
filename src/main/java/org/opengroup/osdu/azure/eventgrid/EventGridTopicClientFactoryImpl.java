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
import com.microsoft.azure.eventgrid.TopicCredentials;
import com.microsoft.azure.eventgrid.implementation.EventGridClientImpl;
import org.opengroup.osdu.azure.cache.EventGridTopicClientCache;
import org.opengroup.osdu.azure.partition.EventGridTopicPartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceEventGridClient;
import org.opengroup.osdu.common.Validators;
import org.opengroup.osdu.core.common.partition.PartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementation for IEventGridTopicClientFactory.
 */
@Component
@ConditionalOnProperty(value = "service.eventgrid.topic.enabled", havingValue = "true", matchIfMissing = true)
public class EventGridTopicClientFactoryImpl implements IEventGridTopicClientFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventGridTopicClientFactoryImpl.class.getName());

    @Autowired
    private PartitionServiceEventGridClient partitionService;

    @Autowired
    private EventGridTopicClientCache clientCache;

    /**
     *
     * @param dataPartitionId Data partition id
     * @param topicName       Topic name
     * @return EventGridClient
     * @throws PartitionException partitionException
     */
    @Override
    public EventGridClient getClient(final String dataPartitionId, final String topicName) throws PartitionException {
        Validators.checkNotNullAndNotEmpty(dataPartitionId, "dataPartitionId");
        Validators.checkNotNull(topicName, "topicName");

        String cacheKey = String.format("%s-%s", dataPartitionId, topicName);
        if (this.clientCache.containsKey(cacheKey)) {
            return this.clientCache.get(cacheKey);
        }
        EventGridTopicPartitionInfoAzure eventGridTopicPartitionInfoAzure =
                this.partitionService.getEventGridTopicInPartition(dataPartitionId, topicName);


        TopicCredentials topicCredentials =
                new TopicCredentials(eventGridTopicPartitionInfoAzure.getTopicAccessKey());

        EventGridClient eventGridClient = new EventGridClientImpl(topicCredentials);
        this.clientCache.put(cacheKey, eventGridClient);
        return eventGridClient;
    }
}