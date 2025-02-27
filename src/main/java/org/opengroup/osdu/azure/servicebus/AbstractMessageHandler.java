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

package org.opengroup.osdu.azure.servicebus;

import com.microsoft.azure.servicebus.ExceptionPhase;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageHandler;
import com.microsoft.azure.servicebus.SubscriptionClient;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.azure.logging.ICoreLogger;
import org.opengroup.osdu.azure.logging.WorkerPayload;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/***
 * Abstract class that implements IMessageHandler. This class cannot be instantiated and is created to enforce standard logging practises for workers.
 */
public abstract class AbstractMessageHandler implements IMessageHandler {

    private static final ICoreLogger LOGGER = CoreLoggerFactory.getInstance().getLogger(AbstractMessageHandler.class.getName());
    private static final String END_LOG_TEMPLATE = "End worker task duration(ms)=%d success=%s";
    private String workerName;
    private SubscriptionClient receiveClient;

    /***
     * Constructor.
     * @param workerServiceName name of the worker service.
     * @param client subscription client.
     */
    public AbstractMessageHandler(final String workerServiceName, final SubscriptionClient client) {
        this.workerName = workerServiceName;
        this.receiveClient = client;
    }

    /***
     * Receives a message from service bus and processes it.
     * @param message service bus message.
     * @return a CompletableFuture representing the pending complete.
     */
    @Override
    public CompletableFuture<Void> onMessageAsync(final IMessage message) {
        long startTime = System.currentTimeMillis();
        String messageId = message.getMessageId();

        try {
            processMessage(message);
            return this.receiveClient.completeAsync(message.getLockToken());
        } catch (Exception e) {
            long stopTime = System.currentTimeMillis();
            logWorkerEnd(messageId, this.workerName, String.format("Exception occurred : %s", e), stopTime - startTime, false);

            if (Instant.now().compareTo(message.getExpiresAtUtc()) < 0) {
                // Current instant is less than message expiry time => message lock has not expired yet.
                // We need to explicitly abandon the message.
                return this.receiveClient.abandonAsync(message.getLockToken());
            } else {
                // Message lock already expired.
                return null;
            }
        }
    }

    /***
     * Receiving the exceptions that passed by pump during message processing.
     * @param throwable Exception thrown.
     * @param exceptionPhase Enumeration to represent the phase in a message pump or session pump at which an exception occurred.
     */
    @Override
    public void notifyException(final Throwable throwable, final ExceptionPhase exceptionPhase) {
        LOGGER.error("Exception {} occurred in service bus message in exception phase {}.", exceptionPhase, throwable.getMessage());
    }

    /***
     * Handle the message received from Service Bus.
     * @param message service bus message.
     * @throws Exception Throws exception in case of failure.
     */
    public abstract void processMessage(IMessage message) throws Exception;

    /***
     * Log end of worker task.
     * @param messageId unique id of message.
     * @param appName name of worker service.
     * @param data information to be logged.
     * @param timeTaken time taken by worker task to complete in milliseconds.
     * @param success boolean, true if successful, false otherwise.
     */
    public void logWorkerEnd(final String messageId, final String appName, final String data, final long timeTaken, final boolean success) {
        WorkerPayload payload = new WorkerPayload(messageId, appName, data, String.format(END_LOG_TEMPLATE, timeTaken, success));
        LOGGER.logWorkerTask(payload);
    }
}
