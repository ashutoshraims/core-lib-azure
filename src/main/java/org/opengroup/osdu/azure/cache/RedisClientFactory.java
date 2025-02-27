package org.opengroup.osdu.azure.cache;

import com.azure.security.keyvault.secrets.SecretClient;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.azure.di.RedisAzureConfiguration;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.core.common.cache.IRedisCache;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis client factory.
 * @param <K>
 * @param <V>
 */
@Component
@Lazy
public class RedisClientFactory<K, V> implements IRedisClientFactory<K, V> {
    private static final String LOGGER_NAME = RedisClientFactory.class.getName();
    private static RedissonClient redissonClient = null;

    @Autowired
    private SecretClient secretClient;

    private Map<String, IRedisCache> redisClientMap;
    /**
     * Initializes the private variables as required.
     */
    @PostConstruct
    public void initialize() {
        redisClientMap = new ConcurrentHashMap<>();
    }

    /**
     * Get redis client.
     * @param keyClass Class type for key
     * @param valueClass Class type for value
     * @param redisConfiguration configuration for redis client
     * @return Redis client
     */
    @Override
    public IRedisCache<K, V> getClient(final Class<K> keyClass, final Class<V> valueClass, final RedisAzureConfiguration redisConfiguration) {
        String cacheKey = String.format("%s-%s", keyClass.toString(), valueClass.toString());
        if (this.redisClientMap.containsKey(cacheKey)) {
            IRedisCache<K, V> cacheObject = this.redisClientMap.get(cacheKey);
            if (cacheObject instanceof RedisCache) {
                return cacheObject;
            } else {
                // No Op cache it is. Re-try to initialize cache.
                CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("The client is of NoOpRedisCache. Re-checking if Redis is available.");
            }
        }

        IRedisCache<K, V> redisCache = this.redisClientMap.computeIfAbsent(cacheKey, cacheObject -> createRedisClient(keyClass, valueClass, redisConfiguration));
        return redisCache == null ? new NoOpRedisCache<>() : redisCache;
    }

    /**
     * Create redis client object.
     * @param keyClass Class type for key
     * @param valueClass Class type for value
     * @param redisConfiguration configuration for redis client
     * @return redis client
     */
    private IRedisCache<K, V> createRedisClient(final Class<K> keyClass, final Class<V> valueClass, final RedisAzureConfiguration redisConfiguration) {
        final String host = getSecret(redisConfiguration.getHostKey());
        final String password = getSecret(redisConfiguration.getPasswordKey());
        if (host == null || password == null) {
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).warn("Required secrets does not exist. Redis is not available yet.");
            return null;
        } else {
            ClientOptions options = ClientOptions.builder()
                    .socketOptions(SocketOptions.builder()
                    .connectTimeout(Duration.ofSeconds(redisConfiguration.getConnectionTimeout())).build())
                    .build();
            return new RedisCache<K, V>(host, redisConfiguration.getPort(), password, redisConfiguration.getExpiration(), redisConfiguration.getCommandTimeout(), redisConfiguration.getDatabase(), options, keyClass, valueClass);
        }
    }

    /**
     * Create RedissonClient instance assuming redis-host and redis-password already exists.
     * @param applicationName application name.
     * @param redisAzureConfiguration configuration for redis client.
     * @return Redisson client instance
     */
    @Override
    public RedissonClient getRedissonClient(final String applicationName, final RedisAzureConfiguration redisAzureConfiguration) {
        if (redissonClient == null) {
            synchronized (RedissonClient.class) {
                String redisHost = getSecret(redisAzureConfiguration.getHostKey());
                String redisPassword = getSecret(redisAzureConfiguration.getPasswordKey());
                if (redissonClient == null && redisHost != null && redisPassword != null) {
                    Config config = new Config();
                    config.useSingleServer().setAddress(String.format("rediss://%s:%d", redisHost, redisAzureConfiguration.getPort()))
                            .setPassword(redisPassword)
                            .setDatabase(redisAzureConfiguration.getDatabase())
                            .setKeepAlive(true)
                            .setClientName(applicationName);
                    redissonClient = Redisson.create(config);
                }
            }
        }
        return redissonClient;
    }

    /**
     * Get secret from Key vault.
     * @param keyName name of the secret
     * @return Secret value
     */
    private String getSecret(final String keyName) {
        return KeyVaultFacade.getSecretWithDefault(secretClient, keyName, null);
    }
}
