package org.opengroup.osdu.azure.cache;

import org.opengroup.osdu.azure.partition.EventGridTopicPartitionInfoAzure;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.opengroup.osdu.core.common.cache.enums.CachingStrategy.EXPIRE_AFTER_WRITE;

/**
 * Implementation of ICache for PartitionServiceEventGridCache.
 */
@Component
@Lazy
public class PartitionServiceEventGridCache extends VmCache<String, Map<String, EventGridTopicPartitionInfoAzure>> {

    /**
     *  Default cache constructor.
     */
    public PartitionServiceEventGridCache() {
        super(60 * 60, 1000, EXPIRE_AFTER_WRITE);
    }

    /**
     * @param key cache key
     * @return true if found in cache
     */
    public boolean containsKey(final String key) {
        return this.get(key) != null;
    }
}
