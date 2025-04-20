package com.vpnsimulation.common.util;

import java.io.Serializable;

/**
 * Represents a message exchanged between VPN client and server
 */
public class Message implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        HANDSHAKE_INIT,
        HANDSHAKE_RESPONSE,
        CERTIFICATE_EXCHANGE,
        KEY_EXCHANGE,
        DATA,
        DISCONNECT,
        ERROR
    }
    
    private MessageType type;
    private String sender;
    private String content;
    private String destinationAddress; // For routing purposes
    private long timestamp;
    
    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }
    
    public Message(MessageType type, String sender, String content, String destinationAddress) {
        this(type, sender, content);
        this.destinationAddress = destinationAddress;
    }
    
    // Getters and setters
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getDestinationAddress() {
        return destinationAddress;
    }
    
    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", sender='" + sender + '\'' +
                ", destinationAddress='" + destinationAddress + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
