package org.opengroup.osdu.azure.logging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dependency logging options.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DependencyLoggingOptions {
    /**
     * the dependency type.
     */
    private String type;
    /**
     * the name of the command initiated with this dependency call.
     */
    private String name;
    /**
     * the command initiated by this dependency call.
     */
    private String data;
    /**
     * the target of this dependency call.
     */
    private String target;
    /**
     * the request duration in milliseconds.
     */
    private long timeTakenInMs;
    /**
     * the request charge.
     */
    private double requestCharge;
    /**
     * the result code of the call.
     */
    private int resultCode;
    /**
     * indication of successful or unsuccessful call.
     */
    private boolean success;
}
