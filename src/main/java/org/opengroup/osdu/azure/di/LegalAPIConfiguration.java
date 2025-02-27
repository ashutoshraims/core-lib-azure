package org.opengroup.osdu.azure.di;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 *  configuration bean class to set up Legal service related variables.
 */
@Data
@ConfigurationProperties
@Component
public class LegalAPIConfiguration {

    private String legalTagApi;

    private String legalApiKey = "OBSOLETE";
}
