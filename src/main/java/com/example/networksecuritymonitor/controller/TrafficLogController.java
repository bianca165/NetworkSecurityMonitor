package com.example.networksecuritymonitor.controller;

import com.example.networksecuritymonitor.model.TrafficLog;
import com.example.networksecuritymonitor.service.TrafficMonitorService;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.stream.Collectors;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class TrafficLogController {

    private final TrafficMonitorService trafficMonitorService;

    @Autowired
    public TrafficLogController(TrafficMonitorService trafficMonitorService) {
        this.trafficMonitorService = trafficMonitorService;
    }

    @GetMapping
    public ResponseEntity<List<TrafficLog>> getAllLogs() {
        List<TrafficLog> logs = trafficMonitorService.getAllLogs();

        return ResponseEntity.ok(logs);
    }

    @PostMapping
    public ResponseEntity<String> addLog(@RequestBody TrafficLog log) {
        trafficMonitorService.saveLog(log);
        return ResponseEntity.ok("Log added successfully");
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/attack/{type}")
    public ResponseEntity<List<TrafficLog>> getLogsByAttackType(@PathVariable String type) {
        List<TrafficLog> logs = trafficMonitorService.getLogsByAttackType(type);
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/comments")
    public ResponseEntity<String> submitComment(@RequestBody Map<String, String> request) {
        String comment = request.get("comment");
        TrafficLog log = new TrafficLog("Unknown", "/api/comments", 200, "XSS Attack", "Logged", "Logged", Instant.now());
        trafficMonitorService.saveLog(log);
        return ResponseEntity.ok("Comentariu primit");
    }

    @GetMapping("/files")
    public ResponseEntity<String> getFile(@RequestParam String file) {
        TrafficLog log = new TrafficLog("Unknown", "/api/files", 403, "Path Traversal", "Blocked", "Logged", Instant.now());
        trafficMonitorService.saveLog(log);
        return ResponseEntity.status(403).body("Access interzis la " + file);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body("Eroare validare: " + errors);
    }
}

