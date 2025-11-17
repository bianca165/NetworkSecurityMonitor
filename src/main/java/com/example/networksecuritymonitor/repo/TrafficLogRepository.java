package com.example.networksecuritymonitor.repo;

import com.example.networksecuritymonitor.model.TrafficLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TrafficLogRepository extends MongoRepository<TrafficLog, String> {
    List<TrafficLog> findByAttackType(String attackType);
}


