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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Contains tests for {@link Slf4jLoggerFactory}
 */
@ExtendWith(MockitoExtension.class)
public class Slf4JLoggerFactoryTest {
    private static final String LOGGER_NAME1 = Slf4JLoggerFactoryTest.class.getName();
    private static final String LOGGER_NAME2 = Slf4jLoggerFactory.class.getName();

    @InjectMocks
    private Slf4jLoggerFactory slf4jLoggerFactory;

    @Test
    public void testSameLoggerInstanceReturnedWhenCalledWithSameName() {
        final Logger logger1 = slf4jLoggerFactory.getLogger(LOGGER_NAME1);
        final Logger logger2 = slf4jLoggerFactory.getLogger(LOGGER_NAME1);
        assertSame(logger1, logger2, "when called with same name, it should return same instance");
    }

    @Test
    public void testDifferentLoggerInstanceReturnedWhenCalledWithDifferentName() {
        final Logger logger1 = slf4jLoggerFactory.getLogger(LOGGER_NAME1);
        final Logger logger2 = slf4jLoggerFactory.getLogger(LOGGER_NAME2);
        assertNotEquals(logger1, logger2, "when called with different names, it should return different instances");
    }
}
