// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.azure.concurrency;

import org.springframework.web.context.request.RequestContextHolder;
import jakarta.validation.constraints.NotNull;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/***
 * A custom thread pool executor that executes code in the new thread after copying the MDC and request context.
 */
public class CustomThreadPoolExecutor extends ThreadPoolExecutor {

    /***
     * Calling constructor of the super class.
     * @param corePoolSize the number of threads to keep in the pool, even
     *                     if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     */
    public CustomThreadPoolExecutor(
            final int corePoolSize,
            final int maximumPoolSize,
            final long keepAliveTime,
            final TimeUnit unit,
            final BlockingQueue<Runnable> workQueue
    ) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    /**
     * Calling execute method of super class after wrapping the executable code with MDC and thread context.
     * @param command
     */
    @Override
    public void execute(final @NotNull Runnable command) {
        super.execute(CustomThreadPoolExecutorUtil.wrapWithContext(command, RequestContextHolder.currentRequestAttributes()));
    }
}
