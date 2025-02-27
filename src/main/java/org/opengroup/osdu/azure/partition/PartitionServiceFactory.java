package org.opengroup.osdu.azure.partition;

import org.opengroup.osdu.azure.di.PartitionServiceConfiguration;
import org.opengroup.osdu.core.common.partition.IPartitionFactory;
import org.opengroup.osdu.core.common.partition.PartitionAPIConfig;
import org.opengroup.osdu.core.common.partition.PartitionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Bootstraps Partition service dependencies for OSDU.
 */
@Component
@Lazy
public class PartitionServiceFactory {

    @Autowired
    private PartitionServiceConfiguration partitionServiceConfiguration;

    /**
     * Partition service factory bean.
     *
     * @return IPartitionFactory instance
     */
    @Bean
    public IPartitionFactory partitionFactory() {
        PartitionAPIConfig apiConfig = PartitionAPIConfig.builder()
                .rootUrl(partitionServiceConfiguration.getPartitionAPIEndpoint())
                .build();
        return new PartitionFactory(apiConfig);
    }
}
