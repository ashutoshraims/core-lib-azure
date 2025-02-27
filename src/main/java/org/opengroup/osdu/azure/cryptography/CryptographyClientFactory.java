package org.opengroup.osdu.azure.cryptography;

import com.azure.security.keyvault.keys.cryptography.CryptographyClient;

/**
 * Interface for Cryptography Client Factory to return appropriate
 * cryptographyClient based on the data partition id.
 */
public interface CryptographyClientFactory {

  /**
   *
   * @param partitionId partition id
   * @return CryptographyClient
   */
  CryptographyClient getClient(String partitionId);
}
