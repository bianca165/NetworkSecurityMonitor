package com.example.networksecuritymonitor.service;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IPBlockerService {
    private final String CONNECTION_STRING;
    private static final String DATABASE_NAME = "licenta";
    private static final String COLLECTION_NAME = "logs";
    private static final String BLOCKED_IPS_COLLECTION = "blocked_ips";

    private static final int ATTACK_THRESHOLD = 5;
    private static final int BLOCK_DURATION = 200;

    private static final Set<String> WHITELIST_IPS = Set.of(
            "35.241.129.42"
    );

    private final Set<String> blockedIPs = new HashSet<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, ScheduledFuture<?>> scheduledUnblockTasks = new ConcurrentHashMap<>();

    public IPBlockerService(@Value("${spring.data.mongodb.uri}") String connectionUri) {
        this.CONNECTION_STRING = connectionUri;
        loadBlockedIPsFromDB();
        scheduler.scheduleAtFixedRate(this::checkAndBlockIPs, 0, 2, TimeUnit.MINUTES);
    }
    public void monitorAndBlockIPs() {
        loadBlockedIPsFromDB();
        scheduler.scheduleAtFixedRate(this::checkAndBlockIPs, 0, 2, TimeUnit.MINUTES);
    }

    private void checkAndBlockIPs() {
        System.out.println("[INFO] Verificare IP-uri ...");
        try (var mongoClient = MongoClients.create(this.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

            var pipeline = Arrays.asList(
                    new Document("$match", new Document("attack_type", new Document("$ne", "Normal"))),
                    new Document("$group", new Document("_id", "$ip_address")
                            .append("count", new Document("$sum", 1))),
                    new Document("$match", new Document("count", new Document("$gt", ATTACK_THRESHOLD)))
            );

            List<Document> results = collection.aggregate(pipeline).into(new ArrayList<>());

            System.out.println("[DEBUG] IP-uri detectate pentru blocare: " + results);

            for (Document doc : results) {
                String ip = doc.get("_id").toString();
                System.out.println("[DEBUG] IP găsit pentru blocare: " + ip);
                if (!blockedIPs.contains(ip)) {
                    System.out.println("[DEBUG] Apel blockIP pentru: " + ip);
                    blockIP(ip);
                }
            }
        }
    }

    public void blockIP(String ip) {
        if (WHITELIST_IPS.contains(ip)) {
            return;
        }

        try {
            System.out.println("[SECURITY] Blocare IP: " + ip);
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", "sudo iptables -A INPUT -s " + ip + " -j DROP");
            pb.inheritIO();
            pb.start();
            blockedIPs.add(ip);

            Date blockingTime = new Date();
            try (var mongoClient = MongoClients.create(this.CONNECTION_STRING)) {
                MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
                MongoCollection<Document> blockedCollection = database.getCollection(BLOCKED_IPS_COLLECTION);

                blockedCollection.deleteOne(new Document("ip", ip));

                blockedCollection.insertOne(
                        new Document("ip", ip)
                                .append("blocked_at", blockingTime)
                                .append("duration", BLOCK_DURATION)
                );
            }

            scheduleUnblock(ip, BLOCK_DURATION);

        } catch (IOException e) {
            System.err.println("[ERROR] Nu s-a putut bloca IP-ul: " + e.getMessage());
        }
    }

    private void scheduleUnblock(String ip, int durationSeconds) {
        ScheduledFuture<?> existingTask = scheduledUnblockTasks.get(ip);
        if (existingTask != null && !existingTask.isDone()) {
            boolean cancelled = existingTask.cancel(false);
            System.out.println("[DEBUG] Task de deblocare existent pentru " + ip + " anulat: " + cancelled);
        }

        ScheduledFuture<?> unblockTask = scheduler.schedule(() -> {
            unblockIP(ip);
            scheduledUnblockTasks.remove(ip); // Curăță după executare
        }, durationSeconds, TimeUnit.SECONDS);

        scheduledUnblockTasks.put(ip, unblockTask);

        System.out.println("[SCHEDULE] IP " + ip + " va fi deblocat în " + durationSeconds + " secunde");
    }

    public static void shutdownScheduler() {
        scheduler.shutdown();
    }

    public void unblockIP(String ip) {
        try {
            System.out.println("[SECURITY] Deblocare IP: " + ip);
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", "sudo iptables -D INPUT -s " + ip + " -j DROP");
            pb.inheritIO();
            pb.start();
            blockedIPs.remove(ip);

            try (var mongoClient = MongoClients.create(this.CONNECTION_STRING)) {
                MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
                MongoCollection<Document> blockedCollection = database.getCollection(BLOCKED_IPS_COLLECTION);
                blockedCollection.deleteOne(new Document("ip", ip));
            }

            scheduledUnblockTasks.remove(ip);

        } catch (IOException e) {
            System.err.println("[ERROR] Nu s-a putut debloca IP-ul: " + e.getMessage());
        }
    }

    public void updateBlockDuration(String ip, int newDurationSeconds) {
        Date currentTime = new Date();

        try (var mongoClient = MongoClients.create(this.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
            MongoCollection<Document> blockedCollection = database.getCollection(BLOCKED_IPS_COLLECTION);

            blockedCollection.updateOne(
                    new Document("ip", ip),
                    new Document("$set", new Document("duration", newDurationSeconds)
                            .append("blocked_at", currentTime))
            );

            scheduleUnblock(ip, newDurationSeconds);

            System.out.println("[UPDATE] Durata blocării pentru " + ip + " a fost actualizată la " +
                    newDurationSeconds + " secunde de la " + currentTime);
        }
    }

    public void saveBlockedIPToDB(String ip) {
        try (var mongoClient = MongoClients.create(this.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
            MongoCollection<Document> blockedCollection = database.getCollection(BLOCKED_IPS_COLLECTION);
            blockedCollection.insertOne(new Document("ip", ip).append("blocked_at", new Date()));
        }
    }
    public void removeBlockedIPFromDB(String ip) {
        try (var mongoClient = MongoClients.create(this.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
            MongoCollection<Document> blockedCollection = database.getCollection(BLOCKED_IPS_COLLECTION);
            blockedCollection.deleteOne(new Document("ip", ip));
        }
    }

    private void loadBlockedIPsFromDB() {
        try (var mongoClient = MongoClients.create(this.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
            MongoCollection<Document> blockedCollection = database.getCollection(BLOCKED_IPS_COLLECTION);

            for (Document doc : blockedCollection.find()) {
                String ip = doc.getString("ip");
                blockedIPs.add(ip);

                Date blockedAt = doc.getDate("blocked_at");
                int duration = doc.getInteger("duration", BLOCK_DURATION);
                long unblockTime = blockedAt.getTime() + duration * 1000L;
                long timeLeft = (unblockTime - System.currentTimeMillis()) / 1000;

                if (timeLeft > 0) {
                    scheduler.schedule(() -> unblockIP(ip), timeLeft, TimeUnit.SECONDS);
                    System.out.println("[RESTORE] IP " + ip + " va fi deblocat în " + timeLeft + " secunde");
                } else {
                    unblockIP(ip); // deja a expirat
                }
            }
        }
    }

    public List<String> getBlockedIPsFromDB() {
        try (var mongoClient = MongoClients.create(this.CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
            MongoCollection<Document> blockedCollection = database.getCollection(BLOCKED_IPS_COLLECTION);
            List<String> blocked = new ArrayList<>();
            for (Document doc : blockedCollection.find()) {
                blocked.add(doc.getString("ip"));
            }
            return blocked;
        }
    }

}


