package org.opengroup.osdu.azure.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.microsoft.azure.management.eventgrid.v2020_04_01_preview.implementation.EventGridManager;

/**
 * Implementation of ICache for CryptographyClient.
 */
@Lazy
@Component
public class EventGridManagerCache extends VmCache<String, EventGridManager> {

  /**
   * Default cache constructor.
   */
  public EventGridManagerCache() {
    super(60 * 60, 1000);
  }

  /**
   * @param key cache key
   * @return true if found in cache
   */
  public boolean containsKey(final String key) {
    return this.get(key) != null;
  }
}
