package com.example.networksecuritymonitor.service;

import com.example.networksecuritymonitor.model.TrafficLog;
import com.example.networksecuritymonitor.repo.TrafficLogRepository;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Service
public class LogExportService {
    private final TrafficLogRepository repository;

    public LogExportService(TrafficLogRepository repository) {
        this.repository = repository;
    }

    public void exportLogsToCSV(String filePath) throws IOException {
        List<TrafficLog> logs = repository.findAll();

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("timestamp,ip_address,url,status_code,attack_type,response,status\n");
            for (TrafficLog log : logs) {
                writer.write(String.format("%s,%s,%s,%d,%s,%s,%s\n",
                        log.getTimestamp(),
                        log.getIpAddress(),
                        log.getUrl(),
                        log.getStatusCode(),
                        log.getAttackType(),
                        log.getResponse(),
                        log.getStatus()
                ));
            }
        }
    }
}
