/** Copyright © Microsoft Corporation

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License
 **/

package org.opengroup.osdu.azure.resiliency;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration for CircuitBreaker.
 */
@Configuration
@ConfigurationProperties("azure.circuitbreaker")
@Getter
@Setter
public class AzureCircuitBreakerConfiguration {

    private boolean enable = false;

    private boolean defaultCircuitBreaker = false;
    private int slidingWindowSize = 50;
    private int minimumNumberOfCalls = 50;
    private int failureRate = 50;
    private String slidingWindowType = "TIME_BASED";
    private int permittedCallsInHalfOpenState = 10;

    private CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Create CircuitBreakerRegistry.
     */
    @PostConstruct
    private void setCBR() {
        if (this.isDefaultCircuitBreaker()) {
            // Create a CircuitBreakerRegistry with a custom global configuration
            this.circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        } else {
            // Refer to documentation to understand meaning of these values
            // https://resilience4j.readme.io/docs/circuitbreaker
            CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig.Builder()
                    .failureRateThreshold(this.getFailureRate())
                    .minimumNumberOfCalls(this.getMinimumNumberOfCalls())
                    .slidingWindowSize(this.getSlidingWindowSize())
                    .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.valueOf(this.getSlidingWindowType()))
                    .permittedNumberOfCallsInHalfOpenState(this.getPermittedCallsInHalfOpenState())
                    .build();

            this.circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        }
    }
}
