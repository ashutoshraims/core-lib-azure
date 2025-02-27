package org.opengroup.osdu.azure.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class CoreLoggerFactoryTest {

    @Test
    public void should_have_coreLoggerType_when_applicationInsightsEnabledFlagIsFalse() {
        System.setProperty("APPINSIGHTS_LOGGING_ENABLED", "false");
        CoreLoggerFactory.resetFactory();
        LoggerType loggerType = CoreLoggerFactory.getInstance().getEnabledLoggerType();
        assertEquals(LoggerType.CoreLogger, loggerType);
    }

    @Test
    public void should_have_applicationInsightsLoggerType_when_applicationInsightsEnabledFlagIsTrue() {
        System.setProperty("APPINSIGHTS_LOGGING_ENABLED", "true");
        CoreLoggerFactory.resetFactory();
        LoggerType loggerType = CoreLoggerFactory.getInstance().getEnabledLoggerType();
        assertEquals(LoggerType.ApplicationInsightsLogger, loggerType);
    }

    @Test
    public void should_have_applicationInsightsLoggerType_when_EnvironmentVariableNotSet() {
        CoreLoggerFactory.resetFactory();
        LoggerType loggerType = CoreLoggerFactory.getInstance().getEnabledLoggerType();
        assertEquals(LoggerType.ApplicationInsightsLogger, loggerType);
    }

    @Test
    public void should_return_coreLogger_when_applicationInsightsEnabledFlagIsFalse() {
        System.setProperty("APPINSIGHTS_LOGGING_ENABLED", "false");
        CoreLoggerFactory.resetFactory();
        ICoreLogger iCoreLogger = CoreLoggerFactory.getInstance().getLogger("testLogger");
        assertEquals(CoreLogger.class, iCoreLogger.getClass());
    }

    @Test
    public void should_return_applicationInsightsLogger_when_applicationInsightsEnabledFlagIsTrue() {
        System.setProperty("APPINSIGHTS_LOGGING_ENABLED", "true");
        CoreLoggerFactory.resetFactory();
        ICoreLogger iCoreLogger = CoreLoggerFactory.getInstance().getLogger("testLogger");
        assertEquals(ApplicationInsightsLogger.class, iCoreLogger.getClass());
    }

    @Test
    public void should_return_applicationInsightsLogger_when_EnvironmentVariableNotSet() {
        CoreLoggerFactory.resetFactory();
        ICoreLogger iCoreLogger = CoreLoggerFactory.getInstance().getLogger("testLogger");
        assertEquals(ApplicationInsightsLogger.class, iCoreLogger.getClass());
    }

}
