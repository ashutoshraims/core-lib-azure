package org.opengroup.osdu.azure.logging;

import com.microsoft.applicationinsights.TelemetryClient;

/**
 * Static Class to return Telemetry Client.
 */
public final class ApplicationInsightsClient {

    /**
     * Private Constructor for Utility Class.
     */
    private ApplicationInsightsClient() {
    }

    private static TelemetryClient telemetryClient = new TelemetryClient();

    /**
     * Utility function to return TelemetryClient.
     * @return  static TelemetryClient for ApplicationInsights Logger.
     */
    public static TelemetryClient getTelemetryClient() {
        return telemetryClient;
    }

}
