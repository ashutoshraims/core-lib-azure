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
// limitations under the License

package org.opengroup.osdu.azure.httpconfig;

import org.apache.http.client.config.RequestConfig;
import org.opengroup.osdu.azure.di.RetryAndTimeoutConfiguration;
import org.opengroup.osdu.core.common.http.HttpClientHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Extends HttpClientHandler.
 */
@Primary
@Component
public class HttpClientHandlerAzure extends HttpClientHandler {

    private RetryAndTimeoutConfiguration configuration;

    /**
     * Constuctor injection for RetryAndTimeoutConfiguration.
     *
     * @param retryAndTimeoutConfiguration of type RetryAndTimeoutConfiguration
     */
    @Autowired
    public HttpClientHandlerAzure(final RetryAndTimeoutConfiguration retryAndTimeoutConfiguration) {
        this.configuration = retryAndTimeoutConfiguration;
        super.REQUEST_CONFIG = RequestConfig.custom()
                .setConnectTimeout(configuration.getConnectTimeout())
                .setConnectionRequestTimeout(configuration.getRequestTimeout())
                .setSocketTimeout(configuration.getSocketTimeout()).build();

        super.RETRY_COUNT = configuration.getMaxRetry();
    }

}
