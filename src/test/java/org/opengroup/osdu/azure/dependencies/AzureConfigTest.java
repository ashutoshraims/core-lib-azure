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

package org.opengroup.osdu.azure.dependencies;

import com.azure.core.credential.TokenCredential;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AzureConfigTest {
    @Test
    void tokenCredentialIsCorrectType() {
        TokenCredential credential = new AzureOSDUConfig().azureCredential();
        assertNotNull(credential);
    }

    @Test
    void nullAndEmptyChecksAreMade() {
        AzureOSDUConfig config = new AzureOSDUConfig();
        assertThrows(NullPointerException.class, () -> config.keyVaultSecretsClient(null, "foo"));
        assertThrows(NullPointerException.class, () -> config.keyVaultSecretsClient(config.azureCredential(), null));
        assertThrows(IllegalArgumentException.class, () -> config.keyVaultSecretsClient(config.azureCredential(), ""));
    }
}
