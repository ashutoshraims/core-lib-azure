//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.elastic.dependencies;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.cache.ElasticCredentialsCache;
import org.opengroup.osdu.azure.partition.PartitionInfoAzure;
import org.opengroup.osdu.azure.partition.PartitionServiceClient;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.partition.Property;

import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElasticCredentialRepositoryTest {

    @Mock
    private ElasticCredentialsCache cache;

    @Mock
    private PartitionServiceClient partitionServiceClient;

    @Mock
    private JaxRsDpsLog log;

    @InjectMocks
    private ElasticCredentialRepository sut;

    private TenantInfo tenant;

    private static final String partitionId = "qa";

    @BeforeEach
    public void init() {
        tenant = new TenantInfo();
        tenant.setDataPartitionId(partitionId);
    }

    @Test
    void getClusterSettings_checksForValidURL_andLogsIfFails() {
        PartitionInfoAzure expectedPartitionInfo = PartitionInfoAzure.builder()
                .idConfig(Property.builder().value(partitionId).build())
                .elasticEndpointConfig(Property.builder().value("not-a-url").build())
                .build();
        when(partitionServiceClient.getPartition(partitionId)).thenReturn(expectedPartitionInfo);
        assertThrows(IllegalStateException.class, () -> sut.getElasticClusterSettings(tenant));

        verify(log, times(1)).warning(any(String.class), any(MalformedURLException.class));
    }

    @Test
    void getClusterSettings_checksForHTTPS_andLogsIfFails() {
        PartitionInfoAzure expectedPartitionInfo = PartitionInfoAzure.builder()
                .idConfig(Property.builder().value(partitionId).build())
                .elasticEndpointConfig(Property.builder().value("http://es-endpoint.com:80").build())
                .elasticUsernameConfig(Property.builder().value("es-user").build())
                .elasticPasswordConfig(Property.builder().value("es-pass").build())
                .build();
        when(partitionServiceClient.getPartition(partitionId)).thenReturn(expectedPartitionInfo);
        assertThrows(IllegalStateException.class, () -> sut.getElasticClusterSettings(tenant));

        verify(log, times(1)).warning(any(String.class));
    }

    @Test
    void getClusterSettings_assemblesFromSecretsProperly() {
        PartitionInfoAzure expectedPartitionInfo = PartitionInfoAzure.builder()
                .idConfig(Property.builder().value(partitionId).build())
                .elasticEndpointConfig(Property.builder().value("https://es-endpoint.com:443").build())
                .elasticUsernameConfig(Property.builder().value("es-user").build())
                .elasticPasswordConfig(Property.builder().value("es-pass").build())
                .build();
        when(partitionServiceClient.getPartition(partitionId)).thenReturn(expectedPartitionInfo);

        ClusterSettings settings = sut.getElasticClusterSettings(tenant);
        assertTrue(settings.isHttps());
        assertTrue(settings.isHttps());
        assertEquals(443, settings.getPort());
        assertEquals("es-endpoint.com", settings.getHost());
    }
}
