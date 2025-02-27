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
import org.slf4j.Logger;

/**
 * Logger wrapper around SLF4J APIs.
 */
public class CoreLogger implements ICoreLogger {

    private final Logger logger;

    /**
     * @param traceLogger       the logger instance
     */
    public CoreLogger(final Logger traceLogger) {
        this.logger = traceLogger;
    }

    /**
     * Log a message at the INFO level.
     *
     * @param msg the message string to be logged
     */
    @Override
    public void info(final String msg) {
        this.logger.info(msg);
    }

    /**
     * Log a message at the INFO level according to the specified format and arguments.
     *
     * @param format    the format string
     * @param arguments a list of arguments
     */
    @Override
    public void info(final String format, final Object... arguments) {
        this.logger.info(format, arguments);
    }

    /**
     * Log a message at the DEBUG level.
     *
     * @param msg the message string to be logged
     */
    @Override
    public void debug(final String msg) {
        this.logger.debug(msg);
    }

    /**
     * Log a message at the DEBUG level according to the specified format and arguments.
     *
     * @param format    the format string
     * @param arguments a list of arguments
     */
    @Override
    public void debug(final String format, final Object... arguments) {
        this.logger.debug(format, arguments);
    }

    /**
     * Log a message at the WARN level.
     *
     * @param msg the message string to be logged
     */
    @Override
    public void warn(final String msg) {
        this.logger.warn(msg);
    }

    /**
     * Log a message at the WARN level according to the specified format and arguments.
     *
     * @param format    the format string
     * @param arguments a list of arguments
     */
    @Override
    public void warn(final String format, final Object... arguments) {
        this.logger.warn(format, arguments);
    }

    /**
     * Log an exception (throwable) at the WARN level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    @Override
    public void warn(final String msg, final Throwable t) {
        this.logger.warn(msg, t);
    }

    /**
     * Log a message at the ERROR level.
     *
     * @param msg the message string to be logged
     */
    @Override
    public void error(final String msg) {
        this.logger.error(msg);
    }

    /**
     * Log a message at the ERROR level according to the specified format and arguments.
     *
     * @param format    the format string
     * @param arguments a list of arguments
     */
    @Override
    public void error(final String format, final Object... arguments) {
        this.logger.error(format, arguments);
    }

    /**
     * Log an exception (throwable) at the ERROR level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    @Override
    public void error(final String msg, final Throwable t) {
        this.logger.error(msg, t);
    }

    /**
     * Log an audit.
     *
     * @param payload the audit payload
     */
    @Override
    public void logAudit(final AuditPayload payload) {
        this.logger.info("{}", payload);
    }

    /**
     * Log a dependency.
     *
     * @param payload the dependency payload
     */
    @Override
    public void logDependency(final DependencyPayload payload) {
        this.logger.info("{}", payload);
    }

    /***
     * Log a worker task.
     * @param payload the worker payload
     */
    @Override
    public void logWorkerTask(final WorkerPayload payload) {
        this.logger.info("{}", payload);
    }

}
