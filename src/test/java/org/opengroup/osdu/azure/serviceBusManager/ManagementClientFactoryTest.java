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

package org.opengroup.osdu.azure.serviceBusManager;

import com.microsoft.azure.servicebus.management.ManagementClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ManagementClientFactoryTest {
    private static final String dataPartitionId = "data-partition-id";
    @Mock
    private Map<String, ManagementClient> managementClientMap;

    @Mock
    private ManagementClient managementClient;

    @InjectMocks
    private ManagementClientFactoryImpl sut;

    @Test
    public void shouldThrowExceptionGivenEmptyDataPartitionId() {
        try {
            this.sut.getManager("");
        } catch (IllegalArgumentException ex) {
            assertEquals("partitionId cannot be empty!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

    @Test
    public void shouldThrowExceptionGivenNullDataPartitionId() {
        try {
            this.sut.getManager(null);
        } catch (NullPointerException ex) {
            assertEquals("partitionId cannot be null!", ex.getMessage());
        } catch (Exception ex) {
            fail("Should not get any other exception. Received " + ex.getClass());
        }
    }

    @Test
    public void shouldReturnManagementClientIfPresentInCache() {
        lenient().when(managementClientMap.containsKey(dataPartitionId + "-serviceBusManagementClient")).thenReturn(true);
        lenient().when(managementClientMap.get(dataPartitionId + "-serviceBusManagementClient")).thenReturn(managementClient);
        ManagementClient client = sut.getManager(dataPartitionId);
        assertEquals(managementClient, client);
    }
}
