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

import com.microsoft.azure.eventgrid.models.EventGridEvent;
import org.joda.time.DateTime;
import org.opengroup.osdu.azure.eventgrid.EventGridTopicStore;
import org.opengroup.osdu.azure.publisherFacade.models.PubSubAttributesBuilder;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of Event Grid publisher.
 */
@Component
@ConditionalOnProperty(value = "azure.pubsub.publish", havingValue = "true", matchIfMissing = false)
public class EventGridPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventGridPublisher.class);
    @Autowired
    private EventGridTopicStore eventGridTopicStore;

    /**
     * @param publisherInfo Contains Event grid batch and publishing details
     * @param headers       DpsHeaders
     */
    public void publishToEventGrid(final DpsHeaders headers, final PublisherInfo publisherInfo) {
        List<EventGridEvent> eventsList = new ArrayList<>();
        PubSubAttributesBuilder pubSubBuilder = PubSubAttributesBuilder.builder().dpsHeaders(headers).build();
        HashMap<String, Object> data = pubSubBuilder.createAttributesMap();
        data.put("data", publisherInfo.getBatch());

        String messageId = UUID.randomUUID().toString();
        eventsList.add(new EventGridEvent(
                messageId,
                publisherInfo.getEventGridEventSubject(),
                data,
                publisherInfo.getEventGridEventType(),
                DateTime.now(),
                publisherInfo.getEventGridEventDataVersion()
        ));
        LOGGER.debug(String.format("Event generated: %s", messageId));
        // If a record change is not published (publishToEventGridTopic throws) we fail the job.
        // This is done to make sure no notifications are missed.

        // Event Grid has a capability to publish multiple events in an array. This will have perf implications,
        // hence publishing one event at a time. If we are confident about the perf capabilities of consumer services,
        // we can publish more more than one event in an array.
        eventGridTopicStore.publishToEventGridTopic(headers.getPartitionId(), publisherInfo.getEventGridTopicName(), eventsList);
    }

}
