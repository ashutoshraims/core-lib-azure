package org.opengroup.osdu.azure.di;

import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Defines configurations for Cosmos bulk executor.
 */
@Configuration
@Getter
@Lazy
public class CosmosBulkExecutorConfiguration {
    /**
     * @return the connection pool size for the http client used by document client. Use SDK default if not set.
     */
    @Bean
    public int documentClientMaxPoolSize() {
        return Integer.parseInt(System.getenv().getOrDefault("DOCUMENT_CLIENT_MAX_POOL_SIZE", "100"));
    }

    /**
     * @return the amount of RUs allocated to bulk executor.
     */
    @Bean
    public Integer bulkExecutorMaxRUs() {
        return Integer.parseInt(System.getenv().getOrDefault("BULK_EXECUTOR_MAX_RUS", "4000"));
    }
}
