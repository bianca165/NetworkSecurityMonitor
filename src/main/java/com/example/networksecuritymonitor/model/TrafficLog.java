package com.example.networksecuritymonitor.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Document(collection = "logs")
public class TrafficLog {

    @Id
    private String id;

    @Field("ip_address")
    @NotBlank(message = "IP-ul nu poate fi gol")
    private String ipAddress;

    @NotBlank(message = "URL-ul nu poate fi gol")
    private String url;

    @Field("status_code")
    @NotNull(message = "Status code-ul nu poate fi null")
    private int statusCode;

    @Field("attack_type")
    @NotBlank(message = "Tipul de atac nu poate fi gol")
    private String attackType;

    private String response;
    private String status;

    @NotNull(message = "Timestamp-ul nu poate fi null")
    private Instant timestamp;

    // Constructor
    public TrafficLog(String ipAddress, String url, int statusCode, String attackType, String response, String status, Instant timestamp) {
        this.ipAddress = ipAddress;
        this.url = url;
        this.statusCode = statusCode;
        this.attackType = attackType;
        this.response = response;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getAttackType() {
        return attackType;
    }

    public void setAttackType(String attackType) {
        this.attackType = attackType;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}

