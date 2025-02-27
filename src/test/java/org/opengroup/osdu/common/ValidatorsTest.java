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

package org.opengroup.osdu.common;

import org.junit.jupiter.api.Test;
import org.opengroup.osdu.core.common.model.http.AppException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidatorsTest {
    @Test
    void checkNotNull_ignoresNonNull() {
        Validators.checkNotNull("non-null-data", "foo");
    }

    @Test
    void checkNotNull_catchesNull() {
        assertThrows(NullPointerException.class, () -> Validators.checkNotNull(null, "foo"));
    }

    @Test
    void checkNotNullAndNotEmpty_catchesNull() {
        assertThrows(NullPointerException.class, () -> Validators.checkNotNullAndNotEmpty(null, "foo"));
    }

    @Test
    void checkNotNullAndNotEmpty_catchesEmpty() {
        assertThrows(IllegalArgumentException.class, () -> Validators.checkNotNullAndNotEmpty("", "foo"));
    }

    @Test
    void checkNotNullAndNotEmpty_ignoresNonNullAndNonEmpty() {
        Validators.checkNotNullAndNotEmpty("non-null-data", "foo");
    }

    @Test
    void checkValidDataPartition_ignoresValidPattern() {
        Validators.checkValidDataPartition("valid-string");
    }

    @Test
    void checkValidDataPartition_catchesInvalidPattern() {
        assertThrows(AppException.class, () -> Validators.checkValidDataPartition("invalid-}-string"));
    }
}
