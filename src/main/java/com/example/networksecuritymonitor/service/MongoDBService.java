package com.example.networksecuritymonitor.service;

import com.example.networksecuritymonitor.model.TrafficLog;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import org.bson.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MongoDBService {

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;

    public MongoDBService(@Value("${spring.data.mongodb.uri}") String connectionUri) {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionUri))
                .build();

        this.mongoClient = MongoClients.create(settings);
        this.database = mongoClient.getDatabase("licenta");
        this.collection = database.getCollection("logs");
    }

    public List<TrafficLog> getAllLogs() {
        List<TrafficLog> logs = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

        for (Document doc : collection.find()) {
            try {
                Instant timestamp = Instant.from(formatter.parse(doc.getString("timestamp")));

                TrafficLog log = new TrafficLog(
                        doc.getString("ip_address"),
                        doc.getString("url"),
                        doc.getInteger("status_code"),
                        doc.getString("attack_type"),
                        doc.getString("response"),
                        doc.getString("status"),
                        timestamp
                );

                logs.add(log);
            } catch (DateTimeParseException e) {
                System.err.println("Eroare la parsarea timestamp-ului: " + e.getMessage());
                logs.add(new TrafficLog(
                        doc.getString("ip_address"),
                        doc.getString("url"),
                        doc.getInteger("status_code"),
                        doc.getString("attack_type"),
                        doc.getString("response"),
                        doc.getString("status"),
                        Instant.now()
                ));
            }
        }

        return logs;
    }
}
