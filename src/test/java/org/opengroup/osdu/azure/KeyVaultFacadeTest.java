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

package org.opengroup.osdu.azure;

import com.azure.core.exception.ResourceModifiedException;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.logging.CoreLogger;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
class KeyVaultFacadeTest {
    @Mock
    private CoreLoggerFactory coreLoggerFactory;

    @Mock
    private CoreLogger coreLogger;

    @Mock
    private SecretClient kv;

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
    void init() {
        mockSingleton(coreLoggerFactory);
        when(coreLoggerFactory.getLogger(anyString())).thenReturn(coreLogger);
    }

    @AfterEach
    public void takeDown() {
        resetSingleton();
    }

    @Test
    void getSecretWithValidation_checksForNullResponse() {
        // the null response is the default behavior, but its included here to be explicit
        doReturn(null).when(kv).getSecret("secret-name");
        assertThrows(NullPointerException.class, () -> KeyVaultFacade.getSecretWithValidation(kv, "secret-name"));
    }


    @Test
    void getSecretWithValidation_checksForNullValueWithinResponse() {
        KeyVaultSecret secret = mock(KeyVaultSecret.class);
        // the null response is the default behavior, but its included here
        // to be explicit
        doReturn(null).when(secret).getValue();
        doReturn(secret).when(kv).getSecret("secret-name");

        assertThrows(NullPointerException.class, () -> KeyVaultFacade.getSecretWithValidation(kv, "secret-name"));
    }

    @Test
    void getSecretWithValidation_returnsCorrectSecret() {
        KeyVaultSecret secret = mock(KeyVaultSecret.class);
        doReturn("secret-value").when(secret).getValue();
        doReturn(secret).when(kv).getSecret("secret-name");

        String secretValue = KeyVaultFacade.getSecretWithValidation(kv, "secret-name");
        assertEquals("secret-value", secretValue);
    }

    @Test
    void checkIfSecretExists_returnsFalseWhenSecretNotExists() {
        doThrow(new ResourceNotFoundException("Secret not found", null)).when(kv).getSecret("secret-name");

        assertFalse(KeyVaultFacade.checkIfSecretExists(kv, "secret-name"));
    }

    @Test
    void checkIfSecretExists_returnsFalseWhenSecretIsDisabled() {
        doThrow(new ResourceModifiedException("Secret is disabled", null)).when(kv).getSecret("secret-name");

        assertFalse(KeyVaultFacade.checkIfSecretExists(kv, "secret-name"));
    }

    @Test
    void checkIfSecretExists_returnsTrueWhenSecretExists() {
        KeyVaultSecret secret = mock(KeyVaultSecret.class);
        doReturn(secret).when(kv).getSecret("secret-name");

        assertTrue(KeyVaultFacade.checkIfSecretExists(kv, "secret-name"));
    }
}
