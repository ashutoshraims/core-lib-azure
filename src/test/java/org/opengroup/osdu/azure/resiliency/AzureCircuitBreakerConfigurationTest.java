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

import io.jsonwebtoken.lang.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AzureCircuitBreakerConfigurationTest {

    @InjectMocks
    @Spy
    AzureCircuitBreakerConfiguration circuitBreakerConfiguration;

    @BeforeEach
    public void prepare() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MockitoAnnotations.initMocks(this);
        // Using reflection to call Postconstruct method of AzureCircuitBreakerConfiguration class
        Method postConstruct =  AzureCircuitBreakerConfiguration.class.getDeclaredMethod("setCBR",null); // methodName,parameters
        postConstruct.setAccessible(true);
        postConstruct.invoke(circuitBreakerConfiguration);
    }

    @Test
    public void should_create_CircuitBreakerRegistry() {
        Assert.isTrue(circuitBreakerConfiguration.getCircuitBreakerRegistry()!=null);
    }
}
