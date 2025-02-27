package org.opengroup.osdu.azure.logging;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.util.MapUtil;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import org.slf4j.Logger;
import org.slf4j.MDC;


/**
 * ApplicationInsightsLogger impl for CoreLogger to send dependency data into ApplicationInsights.
 */
public class ApplicationInsightsLogger extends CoreLogger {

    /**
     * Singleton Telemetry client for all ApplicationInsightsLogger.
     */
    private final TelemetryClient telemetryClient = ApplicationInsightsClient.getTelemetryClient();

    /**
     * @param traceLogger the logger instance.
     */
    public ApplicationInsightsLogger(final Logger traceLogger) {
        super(traceLogger);
    }

    /**
     * Log dependency payload.
     * @param payload the dependency payload
     */
    @Override
    public void logDependency(final DependencyPayload payload) {
        this.telemetryClient.trackDependency(getRemoteDependencyTelemetry(payload));
    }

    /**
     * Returns a RemoteDependencyTelemetry object from DependencyPayload object.
     * @param payload a DependencyPayload object
     * @return a RemoteDependencyTelemetry object
     */
    private RemoteDependencyTelemetry getRemoteDependencyTelemetry(final DependencyPayload payload) {
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry(payload.getName(), payload.getData(), new Duration(payload.getDuration().toMillis()), payload.isSuccess());
        telemetry.setResultCode(payload.getResultCode());
        telemetry.setType(payload.getType());
        telemetry.setTarget(payload.getTarget());
        if (payload.getRequestCharge() != 0.0) {
            telemetry.getProperties().put("requestCharge", Double.toString(payload.getRequestCharge()));
        }
        MapUtil.copy(MDC.getCopyOfContextMap(), telemetry.getContext().getProperties());
        return telemetry;
    }
}
