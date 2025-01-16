package com.services.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import com.services.services.controller.CounterController;

import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.profiles.active=test"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CounterControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CounterController counterController;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        counterController.resetCounters();
        
        // Verify counters are cleared
        ResponseEntity<List> response = restTemplate.getForEntity(
            baseUrl + "/counters",
            List.class
        );
        assertTrue(response.getBody().isEmpty(), "Counters were not properly cleared before test");
    }

    @Test
    void testHealthEndpoint() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            baseUrl + "/health",
            Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK", response.getBody().get("status"));
    }

    @Test
    void testCreateCounterSuccess() {
        String counterName = "test-counter";
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/counters/" + counterName,
            null,
            Map.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getHeaders().getLocation());
        assertEquals(counterName, response.getBody().get("name"));
        assertEquals(0, response.getBody().get("counter"));
    }

    @Test
    void testCreateCounterDuplicate() {
        String counterName = "duplicate-counter";
        
        // Create first counter
        restTemplate.postForEntity(
            baseUrl + "/counters/" + counterName,
            null,
            Map.class
        );

        // Try to create duplicate
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/counters/" + counterName,
            null,
            Map.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().get("error").toString()
            .contains("Counter duplicate-counter already exists"));
    }

    @Test
    void testListCounters() {
        // Verify we start with no counters
        ResponseEntity<List> initialResponse = restTemplate.getForEntity(
            baseUrl + "/counters",
            List.class
        );
        assertEquals(0, initialResponse.getBody().size(), "Should start with no counters");

        // Create some test counters
        restTemplate.postForEntity(baseUrl + "/counters/counter1", null, Map.class);
        restTemplate.postForEntity(baseUrl + "/counters/counter2", null, Map.class);

        ResponseEntity<List> response = restTemplate.getForEntity(
            baseUrl + "/counters",
            List.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size(), "Should have exactly 2 counters");
    }

    @Test
    void testCompleteCounterLifecycle() {
        String counterName = "lifecycle-counter";

        // Create counter
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
            baseUrl + "/counters/" + counterName,
            null,
            Map.class
        );
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertEquals(0, createResponse.getBody().get("counter"));

        // Update counter multiple times
        for (int i = 0; i < 3; i++) {
            ResponseEntity<Map> updateResponse = restTemplate.exchange(
                baseUrl + "/counters/" + counterName,
                HttpMethod.PUT,
                null,
                Map.class
            );
            assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
            assertEquals(i + 1, updateResponse.getBody().get("counter"));
        }

        // Read counter
        ResponseEntity<Map> readResponse = restTemplate.getForEntity(
            baseUrl + "/counters/" + counterName,
            Map.class
        );
        assertEquals(HttpStatus.OK, readResponse.getStatusCode());
        assertEquals(3, readResponse.getBody().get("counter"));

        // Delete counter
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
            baseUrl + "/counters/" + counterName,
            HttpMethod.DELETE,
            null,
            Void.class
        );
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

        // Verify deletion
        ResponseEntity<Map> verifyResponse = restTemplate.getForEntity(
            baseUrl + "/counters/" + counterName,
            Map.class
        );
        assertEquals(HttpStatus.NOT_FOUND, verifyResponse.getStatusCode());
    }
}