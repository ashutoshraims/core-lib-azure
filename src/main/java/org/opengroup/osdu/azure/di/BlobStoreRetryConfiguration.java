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

package org.opengroup.osdu.azure.di;

import com.azure.storage.common.policy.RequestRetryOptions;
import com.azure.storage.common.policy.RetryPolicyType;
import lombok.Getter;
import lombok.Setter;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

/**
 * Config for BlogStorage Retry.
 */
@Configuration
@ConfigurationProperties("azure.blobstore")
@Getter
@Setter
public class BlobStoreRetryConfiguration {

    public static final String LOGGER_NAME = BlobStoreRetryConfiguration.class.getName();

    private final RequestRetryOptions defaultRequestRetryOptions = new RequestRetryOptions();
    private static final String DEFAULT_STRING_VALUE = "";

    private int maxTries = defaultRequestRetryOptions.getMaxTries();
    private long tryTimeoutInSeconds = defaultRequestRetryOptions.getTryTimeoutDuration().getSeconds();
    private long retryDelayInMs = defaultRequestRetryOptions.getRetryDelay().toMillis();
    private long maxRetryDelayInMs = defaultRequestRetryOptions.getMaxRetryDelay().toMillis();
    private String retryPolicyTypeValue = DEFAULT_STRING_VALUE;

    /**
     * Checks whether an string variable value is configured or not.
     * @param val string value to be checked
     * @return true if value is configured in app.properties
     */
    private boolean valueConfigured(final String val) {
        if (val == null || val.equals(DEFAULT_STRING_VALUE)) {
            return false;
        }
        return true;
    }

    /**
     * Method to get RequestRetryOptions object based on configuration set in applicaiton.properties.
     * @return RequestRetryOption object with appropriate configurations.
     */
    public RequestRetryOptions getRequestRetryOptions() {

        // Check whether the variables have been set, else keep them as null.
        // Value has to be sent as null incase where they are not configured to use the default configurations (As specified in RequestRetryOptions.class)
        // https://azure.github.io/azure-storage-java-async/com/microsoft/azure/storage/blob/RequestRetryOptions.html
        RetryPolicyType retryPolicyType;
        try {
            retryPolicyType = valueConfigured(retryPolicyTypeValue) ? RetryPolicyType.valueOf(retryPolicyTypeValue) : RetryPolicyType.EXPONENTIAL;
        } catch (Exception ex) { // For wrong values of Retry Policy, it will default to Exponential.
            retryPolicyType = RetryPolicyType.EXPONENTIAL;
        }


        RequestRetryOptions requestRetryOptions = new RequestRetryOptions(retryPolicyType, maxTries, Duration.ofSeconds(tryTimeoutInSeconds), Duration.ofMillis(retryDelayInMs), Duration.ofMillis(maxRetryDelayInMs), null);


        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info(String.format("Retry Options on BlobStorage with RetryPolicyType = %s , maxTries = %d , tryTimeout = %d , retryDelay = %d , maxRetryDelay = %d .",
                retryPolicyType, requestRetryOptions.getMaxTries(), requestRetryOptions.getTryTimeoutDuration().getSeconds(), requestRetryOptions.getRetryDelay().toMillis(), requestRetryOptions.getMaxRetryDelay().toMillis()));

        return requestRetryOptions;
    }
}
