package com.services.services.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
public class CounterController {
    
    private static final Logger logger = LoggerFactory.getLogger(CounterController.class);
    private final Map<String, Integer> COUNTER = new HashMap<>();

    /**
     * Health Status endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        return ResponseEntity.ok(response);
    }

    /**
     * Index page with service information
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> index() {
        logger.info("Request for Base URL");
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.OK.value());
        response.put("message", "Hit Counter Service");
        response.put("version", "1.0.0");
        response.put("url", ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/counters")
                .toUriString());
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all counters
     */
    @GetMapping("/counters")
    public ResponseEntity<List<Map<String, Object>>> listCounters() {
        logger.info("Request to list all counters...");
        List<Map<String, Object>> counters = new ArrayList<>();
        COUNTER.forEach((name, value) -> {
            Map<String, Object> counter = new HashMap<>();
            counter.put("name", name);
            counter.put("counter", value);
            counters.add(counter);
        });
        return ResponseEntity.ok(counters);
    }

    /**
     * Creates a new counter
     */
    @PostMapping("/counters/{name}")
    public ResponseEntity<Map<String, Object>> createCounter(@PathVariable String name) {
        logger.info("Request to Create counter: {}...", name);
        
        if (COUNTER.containsKey(name)) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", String.format("Counter %s already exists", name)));
        }

        COUNTER.put(name, 0);
        
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .buildAndExpand(name)
            .toUri();

        Map<String, Object> response = new HashMap<>();
        response.put("name", name);
        response.put("counter", 0);
        
        return ResponseEntity
            .created(location)
            .body(response);
    }

    /**
     * Reads a single counter
     */
    @GetMapping("/counters/{name}")
    public ResponseEntity<Map<String, Object>> readCounter(@PathVariable String name) {
        logger.info("Request to Read counter: {}...", name);
        
        if (!COUNTER.containsKey(name)) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", String.format("Counter %s does not exist", name)));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("name", name);
        response.put("counter", COUNTER.get(name));
        return ResponseEntity.ok(response);
    }

    /**
     * Updates a counter
     */
    @PutMapping("/counters/{name}")
    public ResponseEntity<Map<String, Object>> updateCounter(@PathVariable String name) {
        logger.info("Request to Update counter: {}...", name);
        
        if (!COUNTER.containsKey(name)) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", String.format("Counter %s does not exist", name)));
        }

        COUNTER.put(name, COUNTER.get(name) + 1);
        
        Map<String, Object> response = new HashMap<>();
        response.put("name", name);
        response.put("counter", COUNTER.get(name));
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a counter
     */
    @DeleteMapping("/counters/{name}")
    public ResponseEntity<Void> deleteCounter(@PathVariable String name) {
        logger.info("Request to Delete counter: {}...", name);
        COUNTER.remove(name);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clear all counters - for testing only
     */
    public void resetCounters() {
        COUNTER.clear();
    }
}