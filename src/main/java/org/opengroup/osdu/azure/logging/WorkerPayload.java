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

package org.opengroup.osdu.azure.logging;

import lombok.Getter;
import lombok.Setter;

/**
 * Worker Payload.
 */
@Getter
@Setter
public final class WorkerPayload {
    private String id;
    private String name;
    private String data;
    private String logPrefix;

    /**
     * Instantiate payload with specified values.
     * @param messageId unique id to identify a single worker run
     * @param workerName name of the worker service
     * @param workerData any information about the worker task to be logged
     * @param prefix log prefix for worker task
     */
    public WorkerPayload(final String messageId, final String workerName, final String workerData, final String prefix) {
        this.name = workerName;
        this.data = workerData;
        this.id = messageId;
        this.logPrefix = prefix;
    }

    @Override
    public String toString() {
        return String.format("%s : {messageId: %s, workerName: %s, data: %s }", logPrefix, id, name, data);
    }
}
