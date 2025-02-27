package org.opengroup.osdu.azure.cosmosdb.system.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * A configuration class to store cosmos db related config for system resources.
 */
@Configuration
@Getter
@Setter
@ConfigurationProperties("osdu.azure.system")
public class SystemCosmosConfig {
    private String cosmosDBAccountKeyName;
    private String cosmosPrimaryKeyName;
    private String cosmosConnectionStringKeyName;
}
