// Copyright 2017-2024, SLB
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

package org.opengroup.osdu.azure.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LogSamplerTest {

    @Mock
    private LogSamplerConfiguration logSamplerConfiguration;

    @InjectMocks
    LogSampler logSampler;

    @Test
    public void shouldReturnFalseWhenSamplingInfoWhenSamplingSetTo100() {
        when(logSamplerConfiguration.getInfoSamplingPercentage()).thenReturn(100);
        assertFalse(logSampler.shouldSampleInfoLog());
    }

    @Test
    public void shouldReturnFalseWhenSamplingDependencyWhenSamplingSetTo100() {
        when(logSamplerConfiguration.getDependencySamplingPercentage()).thenReturn(100);
        assertFalse(logSampler.shouldSampleDependencyLog());
    }

    @Test
    public void shouldReturnTrueWhenSamplingInfoWhenSamplingSetTo0() {
        when(logSamplerConfiguration.getInfoSamplingPercentage()).thenReturn(0);
        assertTrue(logSampler.shouldSampleInfoLog());
    }

    @Test
    public void shouldReturnTrueWhenSamplingDependencyWhenSamplingSetTo0() {
        when(logSamplerConfiguration.getDependencySamplingPercentage()).thenReturn(0);
        assertTrue(logSampler.shouldSampleDependencyLog());
    }
}
