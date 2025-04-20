package com.vpnsimulation.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.vpnsimulation.common.encryption.AESEncryption;
import com.vpnsimulation.common.encryption.DiffieHellmanKeyExchange;
import com.vpnsimulation.common.util.CertificateManager;
import com.vpnsimulation.common.util.Message;

/**
 * VPN client that connects to the VPN server, encrypts traffic, and handles communication
 */
public class VPNClient {
    
    private final String serverAddress;
    private final int serverPort;
    private final String clientId;
    private Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private boolean connected;
    private final AESEncryption aesEncryption;
    private final DiffieHellmanKeyExchange keyExchange;
    private final CertificateManager certificateManager;
    private final ExecutorService executorService;
    
    private Consumer<Message> messageHandler;
    
    /**
     * Creates a new VPN client instance
     * @param serverAddress The server address to connect to
     * @param serverPort The server port to connect to
     */
    public VPNClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.clientId = UUID.randomUUID().toString();
        this.aesEncryption = new AESEncryption();
        this.keyExchange = new DiffieHellmanKeyExchange();
        this.certificateManager = new CertificateManager();
        this.executorService = Executors.newFixedThreadPool(2);
    }
    
    /**
     * Initializes the client
     */
    public void initialize() throws Exception {
        // Generate client certificate
        certificateManager.generateSelfSignedCertificate("VPN Client " + clientId);
        
        System.out.println("VPN Client initialized. Client ID: " + clientId);
    }
    
    /**
     * Connects to the VPN server
     */
    public boolean connect() throws Exception {
        if (connected) {
            return true;
        }
        
        try {
            // Connect to server
            socket = new Socket(serverAddress, serverPort);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
            
            // Perform handshake
            boolean handshakeSuccess = performHandshake();
            
            if (handshakeSuccess) {
                connected = true;
                
                // Start message listener
                startMessageListener();
                
                System.out.println("Connected to VPN server at " + serverAddress + ":" + serverPort);
                return true;
            } else {
                System.err.println("Handshake failed");
                close();
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            close();
            throw e;
        }
    }
    
    /**
     * Performs the initial handshake with the server
     */
    private boolean performHandshake() throws Exception {
        try {
            // Step 1: Send handshake init
            Message handshakeInit = new Message(
                    Message.MessageType.HANDSHAKE_INIT,
                    clientId,
                    "Hello Server");
            outputStream.writeObject(handshakeInit);
            outputStream.flush();
            System.out.println("Sent handshake init");
            
            // Step 2: Receive handshake response with server certificate
            Message handshakeResponse = (Message) inputStream.readObject();
            if (handshakeResponse.getType() != Message.MessageType.HANDSHAKE_RESPONSE) {
                throw new Exception("Expected HANDSHAKE_RESPONSE, but received " + handshakeResponse.getType());
            }
            System.out.println("Received handshake response");
            
            // Step 3: Send client certificate
            String encodedCertificate = certificateManager.getEncodedCertificate();
            Message certificateExchange = new Message(
                    Message.MessageType.CERTIFICATE_EXCHANGE,
                    clientId,
                    encodedCertificate);
            outputStream.writeObject(certificateExchange);
            outputStream.flush();
            System.out.println("Sent client certificate");
            
            // Step 4: Receive server's Diffie-Hellman public key
            Message serverKeyExchange = (Message) inputStream.readObject();
            if (serverKeyExchange.getType() != Message.MessageType.KEY_EXCHANGE) {
                throw new Exception("Expected KEY_EXCHANGE, but received " + serverKeyExchange.getType());
            }
            System.out.println("Received server key exchange");
            
            // Step 5: Initialize Diffie-Hellman key exchange
            keyExchange.init();
            String publicKeyEncoded = keyExchange.getPublicKeyEncoded();
            
            // Send client's Diffie-Hellman public key
            Message keyExchangeMessage = new Message(
                    Message.MessageType.KEY_EXCHANGE,
                    clientId,
                    publicKeyEncoded);
            outputStream.writeObject(keyExchangeMessage);
            outputStream.flush();
            System.out.println("Sent client key exchange");
            
            // Generate shared secret
            byte[] sharedSecret = keyExchange.generateSharedSecret(serverKeyExchange.getContent());
            
            // Initialize AES encryption with shared secret
            aesEncryption.initFromSharedSecret(sharedSecret);
            
            System.out.println("Handshake completed successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("Handshake failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Starts the message listener thread
     */
    private void startMessageListener() {
        executorService.submit(() -> {
            while (connected) {
                try {
                    Message encryptedMessage = (Message) inputStream.readObject();
                    System.out.println("Received message of type: " + encryptedMessage.getType() + 
                                     " from: " + encryptedMessage.getSender());
                    
                    // Handle different message types
                    switch (encryptedMessage.getType()) {
                        case DATA -> {
                            // Don't decrypt here - pass the encrypted message to the handler
                            if (messageHandler != null) {
                                messageHandler.accept(encryptedMessage);
                            }
                        }
                        case ERROR -> {
                            // Handle error messages
                            try {
                                String errorMessage = aesEncryption.decrypt(encryptedMessage.getContent());
                                System.err.println("Received error from server: " + errorMessage);
                                if (messageHandler != null) {
                                    messageHandler.accept(encryptedMessage);
                                }
                            } catch (Exception ex) {
                                System.err.println("Error decrypting error message: " + ex.getMessage());
                            }
                        }
                        default -> {
                            System.out.println("Received message of type: " + encryptedMessage.getType());
                        }
                    }
                    
                } catch (IOException e) {
                    // Server disconnected
                    if (connected) {
                        System.err.println("Server disconnected: " + e.getMessage());
                        disconnect();
                    }
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.println("Error reading message (invalid class): " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Unexpected error processing message: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Sends a message to the server
     */
    public CompletableFuture<Void> sendMessage(String message, String destinationAddress) {
        if (!connected) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new Exception("Not connected to server"));
            return future;
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                // Encrypt the message
                String encryptedContent = aesEncryption.encrypt(message);
                
                // Create and send the message
                Message dataMessage = new Message(
                        Message.MessageType.DATA,
                        clientId,
                        encryptedContent,
                        destinationAddress);
                        
                System.out.println("Sending message to " + destinationAddress);
                outputStream.writeObject(dataMessage);
                outputStream.flush();
                
            } catch (Exception e) {
                System.err.println("Error sending message: " + e.getMessage());
                throw new RuntimeException("Error sending message", e);
            }
        }, executorService);
    }
    
    /**
     * Sets a handler for incoming messages
     */
    public void setMessageHandler(Consumer<Message> messageHandler) {
        this.messageHandler = messageHandler;
    }
    
    /**
     * Disconnects from the server
     */
    public void disconnect() {
        if (!connected) {
            return;
        }
        
        connected = false;
        
        try {
            // Send disconnect message
            if (outputStream != null) {
                Message disconnectMessage = new Message(
                        Message.MessageType.DISCONNECT,
                        clientId,
                        "Disconnecting");
                String encryptedContent = aesEncryption.encrypt(disconnectMessage.getContent());
                disconnectMessage.setContent(encryptedContent);
                outputStream.writeObject(disconnectMessage);
                outputStream.flush();
            }
        } catch (Exception e) {
            // Ignore, we're disconnecting anyway
        }
        
        close();
    }
    
    /**
     * Closes all resources
     */
    private void close() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
        
        connected = false;
    }
    
    /**
     * Returns whether the client is connected
     */
    public boolean isConnected() {
        return connected;
    }
    
    /**
     * Returns the client ID
     */
    public String getClientId() {
        return clientId;
    }
    
    /**
     * Shuts down the client
     */
    public void shutdown() {
        disconnect();
        executorService.shutdown();
    }
    
    /**
     * Returns the AES encryption instance
     */
    public AESEncryption getAesEncryption() {
        return aesEncryption;
    }
}
