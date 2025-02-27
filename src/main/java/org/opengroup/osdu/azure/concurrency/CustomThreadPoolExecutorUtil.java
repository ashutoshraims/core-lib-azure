package org.opengroup.osdu.azure.concurrency;

import org.jetbrains.annotations.NotNull;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;

/***
 * Utils class for CustomThreadPoolExecutor {@link CustomThreadPoolExecutor}.
 */
public final class CustomThreadPoolExecutorUtil {
    private static final String LOGGER_NAME = CustomThreadPoolExecutorUtil.class.getName();

    /***
     * Constructor marked as private so that this class is never instantiated.
     */
    private CustomThreadPoolExecutorUtil() {
    }

    /***
     * Copy the MDC and request context to the Runnable object being returned.
     * @param  task instance of Runnable class.
     * @param  context the RequestAttributes to be copied to new thread.
     * @return Runnable instance wrapped with request context and MDC.
     */
    public static Runnable wrapWithContext(final @NotNull Runnable task, final RequestAttributes context) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            //save the current MDC context
            setMDCContext(contextMap);
            //save the current request attributes
            setRequestContext(context);
            try {
                task.run();
            } finally {
                // once the task is complete, clear MDC
                MDC.clear();
                RequestContextHolder.resetRequestAttributes();
            }
        };
    }

    /***
     * Set the MDC.
     * @param contextMap the MDC map to be copied.
     */
    private static void setMDCContext(final Map<String, String> contextMap) {
        MDC.clear();
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }  else {
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).warn("Cannot set MDC as it is null");
        }
    }

    /***
     * Set the Request Attributes.
     * @param context the Attributes to be copied.
     */
    private static void setRequestContext(final RequestAttributes context) {
        RequestContextHolder.resetRequestAttributes();
        if (context != null) {
            RequestContextHolder.setRequestAttributes(context);
        } else {
            CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).warn("Cannot set RequestAttributes as they are null");
        }
    }
}