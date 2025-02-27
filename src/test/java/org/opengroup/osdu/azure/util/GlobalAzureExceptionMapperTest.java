package org.opengroup.osdu.azure.util;

import com.azure.cosmos.implementation.RequestEntityTooLargeException;
import com.azure.cosmos.implementation.RequestRateTooLargeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class GlobalAzureExceptionMapperTest {

    @InjectMocks
    private GlobalAzureExceptionMapper sut;

    @Test
    public void should_returnPayloadTooLarge_with_correct_reason_when_RequestEntityTooLargeException_Is_Captured() {
        RequestEntityTooLargeException exception = mock(RequestEntityTooLargeException.class);

        ResponseEntity response = this.sut.handleRequestEntityTooLargeException(exception);

        assertEquals(413, response.getStatusCodeValue());
        assertEquals("Record size is too large", ((AppError) response.getBody()).getReason());
    }

    @Test
    public void should_returnServiceUnavailable_with_correct_reason_when_RequestRateTooLargeException_Is_Captured() {
        RequestRateTooLargeException exception = mock(RequestRateTooLargeException.class);

        ResponseEntity response = this.sut.handleCosmosdbException(exception);

        assertEquals(429, response.getStatusCodeValue());
        assertEquals("Cosmos DB request rate is too large", ((AppError) response.getBody()).getReason());
    }
}
