package org.opengroup.osdu.azure.di;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * A configuration bean class Enable/Disable PaaS Pod MSI (Managed Service Identity).
 */
@Configuration
@ConfigurationProperties(prefix = "azure.paas.podidentity")
@Getter
@Setter
public class PodIdentityConfiguration {
    private Boolean isEnabled = false;
}
