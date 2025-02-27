package org.opengroup.osdu.azure.concurrency;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CustomExecutorsTest {

    private long keepAliveTime = 0L;
    private int nThreads = 1;
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    @Test
    public void newFixedThreadPool_ShouldReturnExecutorServiceInstanceWithCorrectFields() {
        ExecutorService executorService = CustomExecutors.newFixedThreadPool(nThreads);
        Assertions.assertEquals(nThreads,((ThreadPoolExecutor)executorService).getCorePoolSize());
        Assertions.assertEquals(nThreads,((ThreadPoolExecutor)executorService).getMaximumPoolSize());
        Assertions.assertEquals(keepAliveTime,((ThreadPoolExecutor)executorService).getKeepAliveTime(timeUnit));
    }
}
