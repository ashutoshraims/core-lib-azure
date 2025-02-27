package org.opengroup.osdu.azure.cache;

import com.azure.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.opengroup.osdu.azure.logging.CoreLogger;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.core.common.cache.RedisCache;
import com.azure.security.keyvault.secrets.SecretClient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.di.RedisAzureConfiguration;
import org.opengroup.osdu.core.common.cache.IRedisCache;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

@ExtendWith(MockitoExtension.class)
public class RedisClientFactoryTest {

    @Mock
    private SecretClient secretClient;
    @Mock
    private Map<String, IRedisCache> redisClientMap;
    @Mock
    private RedisAzureConfiguration redisConfiguration;
    @Mock
    private CoreLoggerFactory coreLoggerFactory;
    @Mock
    private CoreLogger coreLogger;
    @InjectMocks
    private RedisClientFactory redisClientFactory;

    /**
     * Workaround for inability to mock static methods like getInstance().
     *
     * @param mock CoreLoggerFactory mock instance
     */
    private void mockSingleton(CoreLoggerFactory mock) {
        try {
            Field instance = CoreLoggerFactory.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reset workaround for inability to mock static methods like getInstance().
     */
    private void resetSingleton() {
        try {
            Field instance = CoreLoggerFactory.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
            instance.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Test
    public void should_return_cachedClient_when_cachedEarlier() {
        RedisCache redisCache = mock(RedisCache.class);
        final String cacheKey = String.format("%s-%s", String.class, List.class);
        when(this.redisClientMap.containsKey(cacheKey)).thenReturn(true);
        when(this.redisClientMap.get(cacheKey)).thenReturn(redisCache);

        IRedisCache<String, List> redisClient = this.redisClientFactory.getClient(String.class, List.class, redisConfiguration);

        assertSame(redisCache, redisClient);
    }

    @Test
    public void should_return_NoOpRedisCache_when_computeIfAbsent_returns_null() {
        final String cacheKey = String.format("%s-%s", String.class, List.class);
        when(this.redisClientMap.containsKey(cacheKey)).thenReturn(false);
        when(this.redisClientMap.computeIfAbsent(any(), any())).thenReturn(null);

        IRedisCache<String, List> redisClient = this.redisClientFactory.getClient(String.class, List.class, redisConfiguration);

        assertTrue(redisClient instanceof NoOpRedisCache);
    }

    @Test
    public void should_again_create_redisClient_if_cachedclient_instanceof_noOpRedisCache() {
        NoOpRedisCache noOpRedisCache = mock(NoOpRedisCache.class);
        RedisCache redisCache = mock(RedisCache.class);
        final String cacheKey = String.format("%s-%s", String.class, List.class);

        mockSingleton(coreLoggerFactory);
        when(coreLoggerFactory.getLogger(anyString())).thenReturn(coreLogger);

        when(this.redisClientMap.containsKey(cacheKey)).thenReturn(true);
        when(this.redisClientMap.get(cacheKey)).thenReturn(noOpRedisCache);
        when(this.redisClientMap.computeIfAbsent(any(), any())).thenReturn(redisCache);

        IRedisCache<String, List> redisClient = this.redisClientFactory.getClient(String.class, List.class, redisConfiguration);
        resetSingleton();

        assert(redisClient instanceof RedisCache);
    }

    @Test
    public void getRedissonClient_should_return_null_if_redis_secret_not_present() {
        when(redisConfiguration.getHostKey()).thenReturn("dummyHost");
        when(redisConfiguration.getPasswordKey()).thenReturn("dummyPassword");
        when(secretClient.getSecret(any())).thenThrow(new ResourceNotFoundException("SecretDoesNotExists", null));
        mockSingleton(coreLoggerFactory);
        when(coreLoggerFactory.getLogger(anyString())).thenReturn(coreLogger);

        RedissonClient redissonClient = this.redisClientFactory.getRedissonClient("dummy", redisConfiguration);

        assertNull(redissonClient);
    }
}
