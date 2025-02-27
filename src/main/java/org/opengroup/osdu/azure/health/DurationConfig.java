package org.opengroup.osdu.azure.health;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
/**
 * Configuration class for Azure HealthWebExtension Duration.
 */

@Configuration
public class DurationConfig {
    /**
     * Bean definition for slow indicator logging threshold.
     *
     * @return the Duration value representing the threshold for slow indicator logging
     */
    @Bean
    public Duration slowIndicatorLoggingThreshold() {
        return Duration.ofSeconds(5);
    }
}
