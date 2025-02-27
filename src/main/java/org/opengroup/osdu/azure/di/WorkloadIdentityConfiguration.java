package org.opengroup.osdu.azure.di;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * A configuration bean class Enable/Disable Workload Identity.
 */
@Configuration
@ConfigurationProperties(prefix = "azure.paas.workloadidentity")
@Getter
@Setter
public class WorkloadIdentityConfiguration {
    private Boolean isEnabled = false;
}
