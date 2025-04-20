package com.vpnsimulation.common.database;

import java.sql.Timestamp;

/**
 * Represents a connection log entry in the database
 */
public class ConnectionLog {
    private int id;
    private String clientId;
    private String clientIp;
    private Timestamp connectionTime;
    private Timestamp disconnectionTime;
    private String connectionStatus;
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getClientIp() {
        return clientIp;
    }
    
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
    
    public Timestamp getConnectionTime() {
        return connectionTime;
    }
    
    public void setConnectionTime(Timestamp connectionTime) {
        this.connectionTime = connectionTime;
    }
    
    public Timestamp getDisconnectionTime() {
        return disconnectionTime;
    }
    
    public void setDisconnectionTime(Timestamp disconnectionTime) {
        this.disconnectionTime = disconnectionTime;
    }
    
    public String getConnectionStatus() {
        return connectionStatus;
    }
    
    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }
    
    @Override
    public String toString() {
        return "Connection ID: " + id + 
               ", Client ID: " + clientId + 
               ", Client IP: " + clientIp + 
               ", Connection Time: " + connectionTime + 
               ", Status: " + connectionStatus;
    }
}
