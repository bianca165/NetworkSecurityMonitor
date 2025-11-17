package com.example.networksecuritymonitor.controller;

import com.example.networksecuritymonitor.service.IPBlockerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class IPBlockRestController {

    // 1. Declarați serviciul ca final.
    private final IPBlockerService ipBlockerService;

    // 2. Injecție prin Constructor: Spring va furniza IPBlockerService (bean-ul)
    // Când creează acest controller.
    public IPBlockRestController(IPBlockerService ipBlockerService) {
        this.ipBlockerService = ipBlockerService;
    }

    @PostMapping("/block-ip")
    public ResponseEntity<String> blockIp(@RequestBody Map<String, String> payload) {
        String ip = payload.get("ip");
        System.out.println("Cerere de blocare pentru IP: " + ip);

        if (ip != null && !ip.isBlank()) {
            try {
                // Utilizați instanța injectată
                this.ipBlockerService.blockIP(ip);
                System.out.println("IP blocat cu succes: " + ip);
                return ResponseEntity.ok("IP blocat cu succes: " + ip);
            } catch (Exception e) {
                System.err.println("Eroare la blocarea IP-ului " + ip + ": " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.internalServerError().body("Eroare la blocarea IP-ului: " + e.getMessage());
            }
        } else {
            System.err.println("IP lipsă sau invalid în cerere");
            return ResponseEntity.badRequest().body("IP lipsă sau invalid");
        }
    }

    @PostMapping("/unblock-ip")
    public ResponseEntity<String> unblockIp(@RequestBody Map<String, String> payload) {
        String ip = payload.get("ip");
        System.out.println("Cerere de deblocare pentru IP: " + ip);

        if (ip != null && !ip.isBlank()) {
            try {
                // Utilizați instanța injectată
                this.ipBlockerService.unblockIP(ip);
                System.out.println("IP deblocat cu succes: " + ip);
                return ResponseEntity.ok("IP deblocat cu succes: " + ip);
            } catch (Exception e) {
                System.err.println("Eroare la deblocarea IP-ului " + ip + ": " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.internalServerError().body("Eroare la deblocarea IP-ului: " + e.getMessage());
            }
        } else {
            System.err.println("IP lipsă sau invalid în cerere");
            return ResponseEntity.badRequest().body("IP lipsă sau invalid");
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("IP Blocker Service este activ pe portul 8081");
    }
}