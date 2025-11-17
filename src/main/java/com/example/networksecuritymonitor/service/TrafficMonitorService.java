package com.example.networksecuritymonitor.service;

import com.example.networksecuritymonitor.model.TrafficLog;
import com.example.networksecuritymonitor.repo.TrafficLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrafficMonitorService {

    private final TrafficLogRepository trafficLogRepository;

    @Autowired
    public TrafficMonitorService(TrafficLogRepository trafficLogRepository) {
        this.trafficLogRepository = trafficLogRepository;
    }

    public List<TrafficLog> getAllLogs() {
        return trafficLogRepository.findAll();
    }

    public void saveLog(TrafficLog log) {
        trafficLogRepository.save(log);
    }

    public List<TrafficLog> getLogsByAttackType(String attackType) {
        return trafficLogRepository.findByAttackType(attackType);
    }
}


