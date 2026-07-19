package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpServiceTest {
    @Test
    void invalidUrlFailsWithoutReturningData() {
        HttpService service = new HttpService();
        assertThrows(Exception.class, () -> service.get("http://127.0.0.1:1/", 200, 1024, java.util.Map.of()));
    }
}
