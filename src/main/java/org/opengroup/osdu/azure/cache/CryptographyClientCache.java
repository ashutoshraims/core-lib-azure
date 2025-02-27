package org.opengroup.osdu.azure.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.azure.security.keyvault.keys.cryptography.CryptographyClient;

/**
 * Implementation of ICache for CryptographyClient.
 */
@Lazy
@Component
public class CryptographyClientCache extends VmCache<String, CryptographyClient> {

  /**
   * Default cache constructor.
   */
  public CryptographyClientCache() {
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
