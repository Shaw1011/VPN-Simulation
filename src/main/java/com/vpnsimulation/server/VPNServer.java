package com.vpnsimulation.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.vpnsimulation.common.database.DatabaseManager;
import com.vpnsimulation.common.encryption.AESEncryption;
import com.vpnsimulation.common.encryption.DiffieHellmanKeyExchange;
import com.vpnsimulation.common.util.CertificateManager;
import com.vpnsimulation.common.util.Message;

/**
 * The VPN server that handles client connections, encryption, and routing
 */
public class VPNServer {
    
    private final int port;
    private boolean running;
    private ServerSocket serverSocket;
    private final ExecutorService executorService;
    private final Map<String, ClientHandler> connectedClients;
    private final DatabaseManager databaseManager;
    private final CertificateManager certificateManager;
    
    /**
     * Creates a new VPN server instance
     * @param port The port to listen on
     */
    public VPNServer(int port) {
        this.port = port;
        this.connectedClients = new ConcurrentHashMap<>();
        this.executorService = Executors.newCachedThreadPool();
        this.databaseManager = new DatabaseManager();
        this.certificateManager = new CertificateManager();
    }
    
    /**
     * Initializes the server
     */
    public void initialize() throws Exception {
        // Initialize database
        databaseManager.initialize();
        
        // Generate server certificate
        certificateManager.generateSelfSignedCertificate("VPN Server");
        
        System.out.println("VPN Server initialized. Server certificate generated.");
    }
    
    /**
     * Starts the server
     */
    public void start() throws IOException {
        if (running) {
            return;
        }
        
        serverSocket = new ServerSocket(port);
        running = true;
        
        System.out.println("VPN Server started on port " + port);
        
        // Accept client connections
        new Thread(() -> {
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleNewClient(clientSocket);
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        }).start();
    }
    
    /**
     * Handles a new client connection
     */
    private void handleNewClient(Socket clientSocket) {
        String clientId = UUID.randomUUID().toString();
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        
        System.out.println("New client connected: " + clientId + " from " + clientIp);
        
        try {
            // Log the connection
            int connectionId = databaseManager.logConnection(clientId, clientIp);
            
            // Create a client handler
            ClientHandler clientHandler = new ClientHandler(clientId, clientSocket, connectionId);
            connectedClients.put(clientId, clientHandler);
            
            // Start the client handler
            executorService.submit(clientHandler);
            
        } catch (SQLException e) {
            System.err.println("Database error handling new client: " + e.getMessage());
            try {
                clientSocket.close();
            } catch (IOException ex) {
                // Ignore
            }
        } catch (IOException e) {
            System.err.println("I/O error handling new client: " + e.getMessage());
            try {
                clientSocket.close();
            } catch (IOException ex) {
                // Ignore
            }
        }
    }
    
    /**
     * Stops the server
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        
        // Close all client connections
        for (ClientHandler handler : connectedClients.values()) {
            handler.disconnect();
        }
        
        // Shutdown executor service
        executorService.shutdown();
        
        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
        
        // Close database connection
        try {
            databaseManager.close();
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
        
        System.out.println("VPN Server stopped");
    }
    
    /**
     * Get the database manager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    /**
     * Get the certificate manager
     */
    public CertificateManager getCertificateManager() {
        return certificateManager;
    }
    
    /**
     * Returns the number of connected clients
     */
    public int getClientCount() {
        return connectedClients.size();
    }
    
    /**
     * Client handler class for processing client connections
     */
    private class ClientHandler implements Runnable {
        
        private final String clientId;
        private final Socket clientSocket;
        private final int connectionId;
        private final ObjectInputStream inputStream;
        private final ObjectOutputStream outputStream;
        private boolean connected;
        private final AESEncryption aesEncryption;
        private final DiffieHellmanKeyExchange keyExchange;
        
        public ClientHandler(String clientId, Socket clientSocket, int connectionId) throws IOException {
            this.clientId = clientId;
            this.clientSocket = clientSocket;
            this.connectionId = connectionId;
            this.outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            this.inputStream = new ObjectInputStream(clientSocket.getInputStream());
            this.connected = true;
            this.aesEncryption = new AESEncryption();
            this.keyExchange = new DiffieHellmanKeyExchange();
        }
        
        @Override
        public void run() {
            try {
                // Perform handshake and key exchange
                performHandshake();
                
                // Process messages
                while (connected) {
                    try {
                        Message encryptedMessage = (Message) inputStream.readObject();
                        
                        // Decrypt the message content
                        String decryptedContent = aesEncryption.decrypt(encryptedMessage.getContent());
                        encryptedMessage.setContent(decryptedContent);
                        
                        // Process the message
                        processMessage(encryptedMessage);
                        
                    } catch (IOException e) {
                        // Client disconnected
                        disconnect();
                        break;
                    } catch (Exception e) {
                        System.err.println("Error processing message from client " + clientId + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in client handler: " + e.getMessage());
            } finally {
                disconnect();
            }
        }
        
        /**
         * Performs the initial handshake with the client
         */
        private void performHandshake() throws Exception {
            try {
                // Step 1: Receive handshake init from client
                Message handshakeInit = (Message) inputStream.readObject();
                if (handshakeInit.getType() != Message.MessageType.HANDSHAKE_INIT) {
                    throw new Exception("Expected HANDSHAKE_INIT, but received " + handshakeInit.getType());
                }
                System.out.println("Received handshake init from client " + clientId);
                
                // Step 2: Send handshake response with server certificate
                String encodedCertificate = certificateManager.getEncodedCertificate();
                Message handshakeResponse = new Message(
                        Message.MessageType.HANDSHAKE_RESPONSE,
                        "server",
                        encodedCertificate);
                outputStream.writeObject(handshakeResponse);
                outputStream.flush();
                System.out.println("Sent handshake response to client " + clientId);
                
                // Step 3: Receive client certificate
                Message clientCertificate = (Message) inputStream.readObject();
                if (clientCertificate.getType() != Message.MessageType.CERTIFICATE_EXCHANGE) {
                    throw new Exception("Expected CERTIFICATE_EXCHANGE, but received " + clientCertificate.getType());
                }
                System.out.println("Received client certificate from " + clientId);
                
                // Step 4: Initialize Diffie-Hellman key exchange
                keyExchange.init();
                String publicKeyEncoded = keyExchange.getPublicKeyEncoded();
                
                Message keyExchangeMessage = new Message(
                        Message.MessageType.KEY_EXCHANGE,
                        "server",
                        publicKeyEncoded);
                outputStream.writeObject(keyExchangeMessage);
                outputStream.flush();
                System.out.println("Sent server key exchange to client " + clientId);
                
                // Step 5: Receive client's Diffie-Hellman public key
                Message clientKeyExchange = (Message) inputStream.readObject();
                if (clientKeyExchange.getType() != Message.MessageType.KEY_EXCHANGE) {
                    throw new Exception("Expected KEY_EXCHANGE, but received " + clientKeyExchange.getType());
                }
                System.out.println("Received client key exchange from " + clientId);
                
                // Generate shared secret
                byte[] sharedSecret = keyExchange.generateSharedSecret(clientKeyExchange.getContent());
                
                // Initialize AES encryption with shared secret
                aesEncryption.initFromSharedSecret(sharedSecret);
                
                System.out.println("Handshake completed with client " + clientId);
                
            } catch (Exception e) {
                System.err.println("Handshake failed with client " + clientId + ": " + e.getMessage());
                throw e;
            }
        }
        
        /**
         * Processes an incoming message from the client
         */
        private void processMessage(Message message) throws Exception {
            try {
                System.out.println("Processing message from " + message.getSender() + 
                                 " to " + message.getDestinationAddress());
                
                switch (message.getType()) {
                    case DATA -> {
                        // Check if this is a message to be routed to another client
                        if (message.getDestinationAddress() != null && !message.getDestinationAddress().isEmpty()) {
                            // Route the message to another client
                            routeMessage(message);
                        } else {
                            // Message is for the server
                            System.out.println("Received data message for server: " + message.getContent());
                        }
                    }
                    case DISCONNECT -> {
                        System.out.println("Client " + clientId + " requested disconnect");
                        disconnect();
                    }
                    default -> {
                        System.out.println("Received message of type " + message.getType() + 
                                         " from client " + clientId);
                    }
                }
            } catch (IOException | SQLException e) {
                System.err.println("Error processing message from client " + clientId + ": " + e.getMessage());
                throw e;
            }
        }
        
        /**
         * Routes a message to its destination
         */
        private void routeMessage(Message message) throws IOException, SQLException {
            String destinationClientId = message.getDestinationAddress();
            ClientHandler destinationHandler = connectedClients.get(destinationClientId);
            
            System.out.println("Attempting to route message from " + message.getSender() + 
                             " to " + destinationClientId);
            
            if (destinationHandler != null) {
                try {
                    // Forward the message to the destination client without modifying content
                    // This preserves the original message content from the sender
                    Message forwardMessage = new Message(
                            Message.MessageType.DATA,
                            message.getSender(), // Keep original sender
                            message.getContent(), // Keep original content
                            destinationClientId);
                    
                    // Send the message
                    destinationHandler.outputStream.writeObject(forwardMessage);
                    destinationHandler.outputStream.flush();
                    
                    // Log the successful transfer
                    System.out.println("Message successfully routed from " + message.getSender() + 
                                     " to " + destinationClientId);
                    
                    // Log the data transfer
                    databaseManager.logDataTransfer(
                            connectionId, 
                            clientSocket.getInetAddress().getHostAddress(), 
                            destinationHandler.clientSocket.getInetAddress().getHostAddress(), 
                            message.getContent().length(), 
                            true,
                            Message.MessageType.DATA.toString());
                            
                } catch (IOException e) {
                    System.err.println("Error sending message to " + destinationClientId + ": " + e.getMessage());
                    throw e;
                }
            } else {
                try {
                    // Send error message back to sender if destination not found
                    String errorMessage = "Destination client " + destinationClientId + " not found";
                    String encryptedError = aesEncryption.encrypt(errorMessage);
                    Message errorResponse = new Message(
                            Message.MessageType.ERROR,
                            "server",
                            encryptedError);
                    outputStream.writeObject(errorResponse);
                    outputStream.flush();
                    
                    // Log the error
                    System.err.println(errorMessage);
                    databaseManager.logSecurityEvent(connectionId, "ROUTING_ERROR", 
                            errorMessage, "WARNING");
                } catch (Exception e) {
                    System.err.println("Error sending error message: " + e.getMessage());
                }
            }
        }
        
        /**
         * Disconnects the client
         */
        public void disconnect() {
            if (!connected) {
                return;
            }
            
            connected = false;
            
            // Log disconnection
            try {
                databaseManager.logDisconnection(connectionId);
            } catch (SQLException e) {
                System.err.println("Error logging disconnection: " + e.getMessage());
            }
            
            // Close streams and socket
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client connection: " + e.getMessage());
            }
            
            // Remove from connected clients
            connectedClients.remove(clientId);
            
            System.out.println("Client disconnected: " + clientId);
        }
    }
}
