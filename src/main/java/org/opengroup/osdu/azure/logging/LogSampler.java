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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Util class for log sampling.
 */
@Component
public class LogSampler {

    private Random random = new Random();

    @Autowired(required = false)
    private LogSamplerConfiguration logSamplerConfiguration;

    /**
     * Returns if an info log should be sampled or not.
     * @return true or false.
     */
    public boolean shouldSampleInfoLog() {
        if (logSamplerConfiguration != null && getRandomNumberBetween1And100() > logSamplerConfiguration.getInfoSamplingPercentage()) {
            return true;
        }
        return false;
    }

    /**
     * Returns if a dependency log should be sampled or not.
     * @return true or false.
     */
    public boolean shouldSampleDependencyLog() {
        if (logSamplerConfiguration != null && getRandomNumberBetween1And100() > logSamplerConfiguration.getDependencySamplingPercentage()) {
            return true;
        }
        return false;
    }

    /**
     * Returns a random number between 1 and 100, inclusive.
     * @return an int between 1 and 100, inclusive.
     */
    private int getRandomNumberBetween1And100() {
        return random.nextInt(100) + 1;
    }
}
