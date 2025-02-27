package org.opengroup.osdu.azure.logging;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DependencyPayloadTest {

    @Test
    public void testToString() {

        String name = "TestName";
        String data = "TestData";
        Duration duration = Duration.ofSeconds(5);
        String resultCode = "200";
        boolean success = true;

        DependencyPayload payload = new DependencyPayload(name, data, duration, resultCode, success);

        // Payload String Output
        String result = payload.toString();

        // Assert
        String expected = String.format("{\"name\": \"%s\", \"data\": \"%s\", \"duration\": %d, \"resultCode\": \"%s\", \"success\": %s}",
                name, data, duration.toNanos(), resultCode, success);
        assertEquals(expected, result);
    }
}