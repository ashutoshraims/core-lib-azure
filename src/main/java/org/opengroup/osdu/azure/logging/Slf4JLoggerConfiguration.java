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

package org.opengroup.osdu.azure.logging;

import org.opengroup.osdu.core.common.model.http.HeadersToLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * Creating beans needed for Slf4JLogger.
 */
@Configuration
public class Slf4JLoggerConfiguration {
    /**
     * Bean for HeadersToLog used in {@link Slf4JLogger}.
     * @return {@link HeadersToLog} instance
     */
    @Bean
    public HeadersToLog headersToLog() {
        return new HeadersToLog(Collections.emptyList());
    }
}
