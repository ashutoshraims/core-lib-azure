package org.opengroup.osdu.azure.blobstorage.system.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * A configuration class to store blob related config for system resources.
 */
@Configuration
@Getter
@Setter
@ConfigurationProperties("osdu.azure.system")
public class SystemBlobStoreConfig {
    private String storageAccountNameKeyName;
    private String storageKeyKeyName;
}
