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

package org.opengroup.osdu.azure.logging;

import org.opengroup.osdu.core.common.logging.audit.AuditPayload;

/**
 * Interface for common logger to support traces, exceptions, dependencies among services, and audit events.
 */
public interface ICoreLogger {
    /**
     * Log a message at the INFO level.
     *
     * @param msg the message string to be logged
     */
    void info(String msg);

    /**
     * Log a message at the INFO level according to the specified format and arguments.
     *
     * @param format    the format string
     * @param arguments a list of arguments
     */
    void info(String format, Object... arguments);

    /**
     * Log a message at the INFO level.
     *
     * @param msg the message string to be logged
     */
    void debug(String msg);

    /**
     * Log a message at the INFO level according to the specified format and arguments.
     *
     * @param format    the format string
     * @param arguments a list of arguments
     */
    void debug(String format, Object... arguments);

    /**
     * Log a message at the WARN level.
     *
     * @param msg the message string to be logged
     */
    void warn(String msg);

    /**
     * Log a message at the WARN level according to the specified format and arguments.
     *
     * @param format    the format string
     * @param arguments a list of arguments
     */
    void warn(String format, Object... arguments);

    /**
     * Log an exception (throwable) at the WARN level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    void warn(String msg, Throwable t);

    /**
     * Log a message at the ERROR level.
     *
     * @param msg the message string to be logged
     */
    void error(String msg);

    /**
     * Log a message at the ERROR level according to the specified format and arguments.
     *
     * @param format    the format string
     * @param arguments a list of arguments
     */
    void error(String format, Object... arguments);

    /**
     * Log an exception (throwable) at the ERROR level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    void error(String msg, Throwable t);

    /**
     * Log an audit.
     *
     * @param payload the audit payload
     */
    void logAudit(AuditPayload payload);

    /**
     * Log a dependency.
     *
     * @param payload the dependency payload
     */
    void logDependency(DependencyPayload payload);

    /***
     * Log a worker task.
     *
     * @param payload the worker payload
     */
    void logWorkerTask(WorkerPayload payload);
}
