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

package org.opengroup.osdu.azure.query;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 *
 */
public final class CosmosStorePageRequest extends PageRequest {
    private static final long serialVersionUID = 6093304300037688375L;
    private String requestContinuation;

    /**
     * @param page page
     * @param size size
     * @param arequestContinuation requestContinuation
     */
    public CosmosStorePageRequest(final int page, final int size, final String arequestContinuation) {
        super(page, size,  Sort.unsorted());
        this.requestContinuation = arequestContinuation;
    }

    /**
     * @param page page
     * @param size size
     * @param requestContinuation requestContinuation
     * @return CosmosStorePageRequest
     */
    public static CosmosStorePageRequest of(final int page, final int size, final String requestContinuation) {
        return new CosmosStorePageRequest(page, size, requestContinuation);
    }

    /**
     * @param page page
     * @param size size
     * @param arequestContinuation requestContinuation
     * @param sort sort
     */
    public CosmosStorePageRequest(final int page, final int size, final String arequestContinuation, final Sort sort) {
        super(page, size, sort);
        this.requestContinuation = arequestContinuation;
    }

    /**
     * @param page page
     * @param size size
     * @param requestContinuation requestContinuation
     * @param sort sort
     * @return CosmosStorePageRequest
     */
    public static CosmosStorePageRequest of(final int page, final int size, final String requestContinuation, final Sort sort) {
        return new CosmosStorePageRequest(page, size, requestContinuation, sort);
    }

    /**
     * @return requestContinuation
     */
    public String getRequestContinuation() {
        return this.requestContinuation;
    }

    /**
     * @return hashCode
     */
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (this.requestContinuation != null ? this.requestContinuation.hashCode() : 0);
        return result;
    }

    /**
     * @param obj obj
     * @return equals
     */
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof CosmosStorePageRequest)) {
            return false;
        } else {
            CosmosStorePageRequest that = (CosmosStorePageRequest) obj;
            boolean continuationTokenEquals = this.requestContinuation != null ? this.requestContinuation.equals(that.requestContinuation) : that.requestContinuation == null;
            return continuationTokenEquals && super.equals(that);
        }
    }
}
