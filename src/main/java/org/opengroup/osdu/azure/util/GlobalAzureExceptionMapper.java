package org.opengroup.osdu.azure.util;

import com.azure.cosmos.implementation.RequestEntityTooLargeException;
import com.azure.cosmos.implementation.RequestRateTooLargeException;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Azure as service specific resource exception handler.
 */

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public final class GlobalAzureExceptionMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalAzureExceptionMapper.class);

    /**
     * RequestEntityTooLargeException handler.
     *
     * @param e RequestEntityTooLargeException exception
     * @return SC_REQUEST_TOO_LONG response
     */
    @ExceptionHandler(RequestEntityTooLargeException.class)
    protected ResponseEntity<Object> handleRequestEntityTooLargeException(final RequestEntityTooLargeException e) {
        return this.getErrorResponse(
                new AppException(HttpStatus.PAYLOAD_TOO_LARGE.value(), "Record size is too large", "Request size is too large. The non-data properties on a record cannot be larger than 2MB", e));
    }

    /**
     * RequestRateTooLargeException handler.
     *
     * @param e RequestRateTooLargeException exception
     * @return SERVICE_UNAVAILABLE response
     */
    @ExceptionHandler(RequestRateTooLargeException.class)
    protected ResponseEntity<Object> handleCosmosdbException(final RequestRateTooLargeException e) {
        return this.getErrorResponse(
            new AppException(HttpStatus.TOO_MANY_REQUESTS.value(), "Cosmos DB request rate is too large",
                        "Request rate is large. Please retry this request later", e));
    }

    /**
     * @param e AppException exception
     * @return Erroneous response
     */
    private ResponseEntity<Object> getErrorResponse(final AppException e) {

        String exceptionMsg = e.getOriginalException() != null
                ? e.getOriginalException().getMessage()
                : e.getError().getMessage();

        if (e.getError().getCode() > 499) {
            this.LOGGER.error(exceptionMsg, e);
        } else {
            this.LOGGER.warn(exceptionMsg, e);
        }

        return new ResponseEntity<Object>(e.getError(), HttpStatus.resolve(e.getError().getCode()));
    }
}
