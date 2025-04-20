package com.vpnsimulation.common.database;

import java.sql.Timestamp;

/**
 * Represents a security event log entry in the database
 */
public class SecurityEventLog {
    private int id;
    private int connectionId;
    private Timestamp timestamp;
    private String eventType;
    private String description;
    private String severity;
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getConnectionId() {
        return connectionId;
    }
    
    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }
    
    public Timestamp getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    @Override
    public String toString() {
        return "Event ID: " + id + 
               ", Connection ID: " + connectionId + 
               ", Time: " + timestamp + 
               ", Type: " + eventType + 
               ", Description: " + description + 
               ", Severity: " + severity;
    }
}
