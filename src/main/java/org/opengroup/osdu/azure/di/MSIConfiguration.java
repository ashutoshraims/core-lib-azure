package org.opengroup.osdu.azure.di;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * A configuration bean class Enable/Disable MSI (Managed Service Identity).
 */
@Configuration
@ConfigurationProperties(prefix = "azure.msi")
@Getter
@Setter
public class MSIConfiguration {
    private Boolean isEnabled = false;
}
