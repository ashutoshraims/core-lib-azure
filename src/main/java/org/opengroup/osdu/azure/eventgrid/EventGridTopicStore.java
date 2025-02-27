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
import com.microsoft.azure.eventgrid.models.EventGridEvent;
import lombok.SneakyThrows;
import org.opengroup.osdu.azure.partition.EventGridTopicPartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceEventGridClient;
import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * A simpler interface to interact with Azure Event Grid Topic.
 * Usage examples:
 * <pre>
 * {@code
 *      @Autowired
 *      private EventGridTopicStore eventGridTopicStore;
 *
 *      String publishToEventGridTopic()
 *      {
 *          List<EventGridEvent> eventsList = new ArrayList<>();
 *          eventsList.add(new EventGridEvent(
 *                     UUID.randomUUID().toString(),
 *                     "subject",
 *                     "data",
 *                     "event type",
 *                     DateTime.now(),
 *                     "0.1"
 *            ));
 *
 *          eventGridTopicStore.publishToEventGridTopic("dataPartitionId", "topicName", eventList);
 *      }
 * }
 * </pre>
 */
@Component
@ConditionalOnProperty(value = "azure.eventgrid.topic.enabled", havingValue = "true", matchIfMissing = true)
public class EventGridTopicStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventGridTopicStore.class.getName());
    @Autowired
    private IEventGridTopicClientFactory eventGridTopicClientFactory;
    @Autowired
    private ILogger logger;
    @Autowired
    private PartitionServiceEventGridClient eventGridPartitionClient;

    /**
     * @param dataPartitionId Data partition id
     * @param topicName       Topic name
     * @param eventsList      List of Event Grid Events
     */
    @SneakyThrows
    public void publishToEventGridTopic(final String dataPartitionId, final String topicName, final List<EventGridEvent> eventsList) {
        EventGridTopicPartitionInfoAzure eventGridTopicPartitionInfoAzure = this.eventGridPartitionClient.getEventGridTopicInPartition(dataPartitionId, topicName);

        String endpoint;
        try {
            endpoint = String.format("https://%s/", new URI(eventGridTopicPartitionInfoAzure.getTopicName()).getHost());
        } catch (URISyntaxException e) {
            throw new AppException(500, "Invalid Event Grid endpoint URI", "PartitionInfo for Event Grid Topic " + topicName, e);
        }
        EventGridClient eventGridClient = eventGridTopicClientFactory.getClient(dataPartitionId, topicName);
        eventGridClient.publishEvents(endpoint, eventsList);


    }
}
