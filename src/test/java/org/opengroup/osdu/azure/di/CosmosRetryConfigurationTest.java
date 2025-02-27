/** Copyright © Microsoft Corporation

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 **/

package org.opengroup.osdu.azure.di;

import com.azure.cosmos.ThrottlingRetryOptions;
import io.jsonwebtoken.lang.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.logging.CoreLogger;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;

import java.lang.reflect.Field;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@ExtendWith(MockitoExtension.class)
public class CosmosRetryConfigurationTest {

    @InjectMocks
    CosmosRetryConfiguration cosmosRetryConfiguration;
    @Mock
    private CoreLoggerFactory coreLoggerFactory;
    @Mock
    private CoreLogger coreLogger;

    ThrottlingRetryOptions defaultRetryOptions = new ThrottlingRetryOptions();

    /**
     * Workaround for inability to mock static methods like getInstance().
     *
     * @param mock CoreLoggerFactory mock instance
     */
    private void mockSingleton(CoreLoggerFactory mock) {
        try {
            Field instance = CoreLoggerFactory.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reset workaround for inability to mock static methods like getInstance().
     */
    private void resetSingleton() {
        try {
            Field instance = CoreLoggerFactory.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
            instance.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        openMocks(this);
        mockSingleton(coreLoggerFactory);
        when(coreLoggerFactory.getLogger(anyString())).thenReturn(coreLogger);
    }

    @AfterEach
    public void takeDown() {
        resetSingleton();
    }

    @Test
    public void should_set_maxRetryCount_ThrottlingOptions() {
        int maxRetryValue = 1;

        cosmosRetryConfiguration.setMaxRetryCount(maxRetryValue);
        ThrottlingRetryOptions throttlingRetryOptions = cosmosRetryConfiguration.getThrottlingRetryOptions();

        Assert.isTrue(throttlingRetryOptions.getMaxRetryAttemptsOnThrottledRequests() == maxRetryValue);
        Assert.isTrue(throttlingRetryOptions.getMaxRetryWaitTime().equals(defaultRetryOptions.getMaxRetryWaitTime()));
    }

    @Test
    public void should_set_RetryWaitTimeout_ThrottlingOptions() {
        int retryWaitTimeoutValue = 20;

        cosmosRetryConfiguration.setRetryWaitTimeout(retryWaitTimeoutValue);
        ThrottlingRetryOptions throttlingRetryOptions = cosmosRetryConfiguration.getThrottlingRetryOptions();

        Assert.isTrue(throttlingRetryOptions.getMaxRetryWaitTime().equals(Duration.ofSeconds(retryWaitTimeoutValue)));
        Assert.isTrue(throttlingRetryOptions.getMaxRetryAttemptsOnThrottledRequests() == defaultRetryOptions.getMaxRetryAttemptsOnThrottledRequests());
    }

    @Test
    public void should_set_RetryWaitTimeout_MaxRetry_ThrottlingOptions() {
        int retryWaitTimeoutValue = 20;
        int maxRetryValue = 1;

        cosmosRetryConfiguration.setRetryWaitTimeout(retryWaitTimeoutValue);
        cosmosRetryConfiguration.setMaxRetryCount(maxRetryValue);

        ThrottlingRetryOptions throttlingRetryOptions = cosmosRetryConfiguration.getThrottlingRetryOptions();

        Assert.isTrue(throttlingRetryOptions.getMaxRetryAttemptsOnThrottledRequests() == maxRetryValue);
        Assert.isTrue(throttlingRetryOptions.getMaxRetryWaitTime().equals(Duration.ofSeconds(retryWaitTimeoutValue)));


    }

    @Test
    public void should_not_set_RetryWaitTimeout_MaxRetry_ThrottlingOptions() {
        ThrottlingRetryOptions throttlingRetryOptions = cosmosRetryConfiguration.getThrottlingRetryOptions();

        Assert.isTrue(throttlingRetryOptions.getMaxRetryAttemptsOnThrottledRequests() == defaultRetryOptions.getMaxRetryAttemptsOnThrottledRequests());
        Assert.isTrue(throttlingRetryOptions.getMaxRetryWaitTime().equals(defaultRetryOptions.getMaxRetryWaitTime()));
    }
}
