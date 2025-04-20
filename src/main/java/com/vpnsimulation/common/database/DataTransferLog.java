package com.vpnsimulation.common.database;

import java.sql.Timestamp;

/**
 * Represents a data transfer log entry in the database
 */
public class DataTransferLog {
    private int id;
    private int connectionId;
    private Timestamp timestamp;
    private String sourceIp;
    private String destinationIp;
    private int dataSize;
    private boolean isEncrypted;
    private String packetType;
    
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
    
    public String getSourceIp() {
        return sourceIp;
    }
    
    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }
    
    public String getDestinationIp() {
        return destinationIp;
    }
    
    public void setDestinationIp(String destinationIp) {
        this.destinationIp = destinationIp;
    }
    
    public int getDataSize() {
        return dataSize;
    }
    
    public void setDataSize(int dataSize) {
        this.dataSize = dataSize;
    }
    
    public boolean isEncrypted() {
        return isEncrypted;
    }
    
    public void setEncrypted(boolean encrypted) {
        isEncrypted = encrypted;
    }
    
    public String getPacketType() {
        return packetType;
    }
    
    public void setPacketType(String packetType) {
        this.packetType = packetType;
    }
    
    @Override
    public String toString() {
        return "Transfer ID: " + id + 
               ", Connection ID: " + connectionId + 
               ", Time: " + timestamp + 
               ", Source IP: " + sourceIp + 
               ", Destination IP: " + destinationIp + 
               ", Size: " + dataSize + " bytes" +
               ", Encrypted: " + isEncrypted +
               ", Type: " + packetType;
    }
}
