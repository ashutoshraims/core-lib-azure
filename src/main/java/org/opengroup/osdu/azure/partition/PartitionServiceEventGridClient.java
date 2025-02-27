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
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.azure.cache.PartitionServiceEventGridCache;
import org.opengroup.osdu.azure.di.MSIConfiguration;
import org.opengroup.osdu.azure.util.AzureServicePrincipleTokenService;
import org.opengroup.osdu.common.Validators;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.IPartitionFactory;
import org.opengroup.osdu.core.common.partition.IPartitionProvider;
import org.opengroup.osdu.core.common.partition.PartitionException;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.opengroup.osdu.core.common.partition.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Partition service client for Event Grid implementation.
 */
@Service
@Lazy
public class PartitionServiceEventGridClient {
    private static final String ACCESS_KEY_REGEX = "^eventgrid-([a-zA-Z0-9]*)topic-accesskey$";
    private static final String TOPIC_NAME_REGEX = "^eventgrid-([a-zA-Z0-9]*)topic$";
    private static final Logger LOGGER = LoggerFactory.getLogger(PartitionServiceEventGridClient.class.getName());
    private final Gson gson = new Gson();

    @Autowired
    private IPartitionFactory partitionFactory;
    @Autowired
    private SecretClient secretClient;
    @Autowired
    private AzureServicePrincipleTokenService tokenService;
    @Autowired
    private DpsHeaders headers;
    @Autowired
    private PartitionServiceEventGridCache partitionServiceEventGridCache;
    @Autowired
    private MSIConfiguration msiConfiguration;

    /**
     * Get TopicInfo for a given topic.
     *
     * @param partitionId partitionId
     * @param topicName   topicName
     * @return EventGridTopicPartitionInfoAzure
     * @throws AppException       exception from the configuration
     * @throws PartitionException AppException Exception thrown by {@link IPartitionFactory}
     */
    public EventGridTopicPartitionInfoAzure getEventGridTopicInPartition(final String partitionId, final String topicName) throws AppException, PartitionException {
        Validators.checkNotNullAndNotEmpty(partitionId, "partitionId");
        Validators.checkNotNullAndNotEmpty(topicName, "topicName");

        Map<String, EventGridTopicPartitionInfoAzure> eventGridTopicPartitionInfoAzure;

        String cacheKey = String.format("%s-%s-eventGridTopicPartitionInfoAzure", partitionId, topicName);
        if (partitionServiceEventGridCache.containsKey(cacheKey)) {
            eventGridTopicPartitionInfoAzure = partitionServiceEventGridCache.get(cacheKey);
        } else {
            eventGridTopicPartitionInfoAzure = getAllRelevantEventGridTopicsInPartition(partitionId, topicName);
            partitionServiceEventGridCache.put(cacheKey, eventGridTopicPartitionInfoAzure);
        }

        if (!eventGridTopicPartitionInfoAzure.containsKey(topicName)) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Invalid EventGrid Partition configuration for the partition " + partitionId, "Please refer to wiki here <>");
        }
        return eventGridTopicPartitionInfoAzure.get(topicName);
    }

    /**
     * Get relevant event grid topics.
     *
     * @param partitionId Partition Id
     * @param topicName Topic Name
     * @return Event Grid Topics
     * @throws AppException       Exception thrown by {@link IPartitionFactory}
     * @throws PartitionException Exception thrown by {@link IPartitionFactory}
     */
    Map<String, EventGridTopicPartitionInfoAzure> getAllRelevantEventGridTopicsInPartition(final String partitionId, final String topicName) throws AppException, PartitionException {
        PartitionInfo partitionInfo = getPartitionInfo(partitionId);
        Map<String, Property> propertyMap = partitionInfo.getProperties();
        Map<String, EventGridTopicPartitionInfoAzure> topics = new HashMap<>();

        for (Map.Entry<String, Property> property : propertyMap.entrySet()) {
            if (isEventGridProperty(property) && property.getKey().contains(topicName)) {
                StringTokenizer stringTokenizer = new StringTokenizer(property.getKey(), "-");
                if (stringTokenizer.countTokens() == 2) {
                    addEventGridTopicName(topics, property, stringTokenizer);
                } else if (stringTokenizer.countTokens() == 3) {
                    if (!msiConfiguration.getIsEnabled()) {
                        addEventGridAccessKey(topics, property, stringTokenizer);
                    }
                } else {
                    throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Invalid EventGrid Partition configuration for the partition " + partitionId, "Please reconfigure the partition service");
                }
            }
        }
        return topics;
    }

    /**
     * @param partitionId partitionId
     * @return PartitionInfo
     * @throws PartitionException Exception thrown by {@link IPartitionFactory}
     */
    PartitionInfo getPartitionInfo(final String partitionId) throws PartitionException {
        IPartitionProvider serviceClient = getServiceClient();
        return serviceClient.get(partitionId);
    }

    /**
     * Util to identify if property is for event grid topic.
     *
     * @param property property
     * @return isEventGridProperty
     */
    private boolean isEventGridProperty(final Map.Entry<String, Property> property) {
        return property.getKey().matches((ACCESS_KEY_REGEX)) || property.getKey().matches((TOPIC_NAME_REGEX));
    }

    /**
     * Populate the map.
     *
     * @param topics          topics
     * @param property        properties
     * @param stringTokenizer tokenizer util
     */
    private void addEventGridAccessKey(final Map<String, EventGridTopicPartitionInfoAzure> topics,
                                       final Map.Entry<String, Property> property,
                                       final StringTokenizer stringTokenizer) {
        stringTokenizer.nextToken();
        String secret = getSecretValue(property);
        String key = stringTokenizer.nextToken();
        if (topics.containsKey(key)) {
            EventGridTopicPartitionInfoAzure topic = topics.get(key);
            topic.setTopicAccessKey(secret);
        } else {
            EventGridTopicPartitionInfoAzure topic = new EventGridTopicPartitionInfoAzure();
            topic.setTopicAccessKey(secret);
            topics.put(key, topic);
        }
    }

    /**
     * Populate the map.
     *
     * @param topics          topics
     * @param property        properties
     * @param stringTokenizer tokenizer util
     */
    private void addEventGridTopicName(final Map<String, EventGridTopicPartitionInfoAzure> topics,
                                       final Map.Entry<String, Property> property,
                                       final StringTokenizer stringTokenizer) {
        stringTokenizer.nextToken();
        String key = stringTokenizer.nextToken();
        String secretValue = getSecretValue(property);
        if (topics.containsKey(key)) {
            EventGridTopicPartitionInfoAzure topic = topics.get(key);
            topic.setTopicName(secretValue);
        } else {
            EventGridTopicPartitionInfoAzure topic = new EventGridTopicPartitionInfoAzure();
            topic.setTopicName(secretValue);
            topics.put(key, topic);
        }
    }

    /**
     * Get the secret value from KeyVault.
     * @param property property
     * @return secret
     */
    private String getSecretValue(final Map.Entry<String, Property> property) {
        JsonElement jsonElement = gson.toJsonTree(property.getValue());
        Property p = gson.fromJson(jsonElement, Property.class);
        if (p.isSensitive()) {
            return KeyVaultFacade.getSecretWithValidation(this.secretClient, String.valueOf(p.getValue()));
        }
        return String.valueOf(p.getValue());
    }

    /**
     * Get Service client for Partition Service.
     *
     * @return PartitionServiceClient
     */
    private IPartitionProvider getServiceClient() {
        DpsHeaders newHeaders = DpsHeaders.createFromMap(headers.getHeaders());
        newHeaders.put(DpsHeaders.AUTHORIZATION, "Bearer " + tokenService.getAuthorizationToken());
        return partitionFactory.create(newHeaders);
    }
}

