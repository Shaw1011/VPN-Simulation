package com.vpnsimulation.client;

import java.util.concurrent.CompletableFuture;

import com.vpnsimulation.common.util.Message;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * JavaFX application for the VPN client
 */
public class VPNClientApp extends Application {
    
    private static final String DEFAULT_SERVER_ADDRESS = "localhost";
    private static final int DEFAULT_SERVER_PORT = 8443;
    
    private VPNClient vpnClient;
    private boolean connected = false;
    
    // UI components
    private TextField serverAddressField;
    private TextField portField;
    private Button connectButton;
    private Label statusLabel;
    private Label clientIdLabel;
    private TextArea messageTextArea;
    private TextField destinationField;
    private Button sendButton;
    private TextArea logTextArea;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Create UI
            BorderPane root = new BorderPane();
            root.setPadding(new Insets(10));
            
            // Top panel with connection controls
            GridPane connectionPanel = createConnectionPanel();
            root.setTop(connectionPanel);
            
            // Center panel with message controls
            VBox messagePanel = createMessagePanel();
            root.setCenter(messagePanel);
            
            // Bottom panel with log
            VBox logPanel = createLogPanel();
            root.setBottom(logPanel);
            
            // Create scene
            Scene scene = new Scene(root, 600, 500);
            primaryStage.setScene(scene);
            primaryStage.setTitle("VPN Client");
            
            // Set close handler
            primaryStage.setOnCloseRequest(e -> {
                stop();
            });
            
            primaryStage.show();
            
        } catch (Exception e) {
            System.err.println("Error starting VPN client: " + e.getMessage());
            showErrorAlert("Error", e.getMessage());
        }
    }
    
    /**
     * Creates the connection panel
     */
    private GridPane createConnectionPanel() {
        GridPane panel = new GridPane();
        panel.setPadding(new Insets(10));
        panel.setHgap(10);
        panel.setVgap(10);
        
        Label serverAddressLabel = new Label("Server Address:");
        serverAddressField = new TextField(DEFAULT_SERVER_ADDRESS);
        
        Label portLabel = new Label("Port:");
        portField = new TextField(String.valueOf(DEFAULT_SERVER_PORT));
        
        connectButton = new Button("Connect");
        connectButton.setOnAction(e -> handleConnectDisconnect());
        
        statusLabel = new Label("Status: Disconnected");
        clientIdLabel = new Label("Client ID: -");
        
        panel.add(serverAddressLabel, 0, 0);
        panel.add(serverAddressField, 1, 0);
        panel.add(portLabel, 2, 0);
        panel.add(portField, 3, 0);
        panel.add(connectButton, 4, 0);
        panel.add(statusLabel, 0, 1, 2, 1);
        panel.add(clientIdLabel, 2, 1, 3, 1);
        
        return panel;
    }
    
    /**
     * Creates the message panel
     */
    private VBox createMessagePanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        
        Label messageLabel = new Label("Message:");
        messageTextArea = new TextArea();
        messageTextArea.setPrefRowCount(5);
        messageTextArea.setEditable(true);
        
        Label destinationLabel = new Label("Destination Address (for routing):");
        destinationField = new TextField("example.com");
        
        sendButton = new Button("Send");
        sendButton.setOnAction(e -> handleSendMessage());
        sendButton.setDisable(true);
        
        panel.getChildren().addAll(messageLabel, messageTextArea, destinationLabel, destinationField, sendButton);
        
        return panel;
    }
    
    /**
     * Creates the log panel
     */
    private VBox createLogPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        
        Label logLabel = new Label("Connection Log:");
        logTextArea = new TextArea();
        logTextArea.setPrefRowCount(10);
        logTextArea.setEditable(false);
        
        panel.getChildren().addAll(logLabel, logTextArea);
        
        return panel;
    }
    
    /**
     * Handles connect/disconnect button click
     */
    private void handleConnectDisconnect() {
        if (!connected) {
            // Connect to server
            try {
                String serverAddress = serverAddressField.getText();
                int port = Integer.parseInt(portField.getText());
                
                // Create and initialize client
                vpnClient = new VPNClient(serverAddress, port);
                vpnClient.initialize();
                
                // Set message handler
                vpnClient.setMessageHandler(this::handleIncomingMessage);
                
                // Log information
                logMessage("Initializing VPN client...");
                logMessage("Client ID: " + vpnClient.getClientId());
                
                // Connect to server
                logMessage("Connecting to server at " + serverAddress + ":" + port + "...");
                boolean success = vpnClient.connect();
                
                if (success) {
                    connected = true;
                    updateUIForConnected();
                    logMessage("Connected to server successfully.");
                } else {
                    logMessage("Failed to connect to server.");
                }
                
            } catch (Exception e) {
                System.err.println("Connection error: " + e.getMessage());
                showErrorAlert("Connection Error", "Failed to connect: " + e.getMessage());
                logMessage("Error: " + e.getMessage());
            }
            
        } else {
            // Disconnect from server
            try {
                vpnClient.disconnect();
                connected = false;
                updateUIForDisconnected();
                logMessage("Disconnected from server.");
                
            } catch (Exception e) {
                System.err.println("Disconnection error: " + e.getMessage());
                showErrorAlert("Disconnection Error", "Failed to disconnect: " + e.getMessage());
                logMessage("Error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handles send message button click
     */
    private void handleSendMessage() {
        if (!connected || vpnClient == null) {
            showErrorAlert("Not Connected", "You must connect to the server first.");
            return;
        }
        
        String message = messageTextArea.getText();
        String destination = destinationField.getText();
        
        if (message.isEmpty()) {
            showErrorAlert("Empty Message", "Please enter a message to send.");
            return;
        }
        
        // Send message through VPN
        logMessage("Sending message to " + destination + "...");
        CompletableFuture<Void> future = vpnClient.sendMessage(message, destination);
        
        future.thenRun(() -> {
            Platform.runLater(() -> {
                logMessage("Message sent successfully.");
                messageTextArea.clear();
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> {
                logMessage("Failed to send message: " + e.getMessage());
                showErrorAlert("Send Error", "Failed to send message: " + e.getMessage());
            });
            return null;
        });
    }
    
    /**
     * Handles incoming messages from the server
     */
    private void handleIncomingMessage(Message message) {
        Platform.runLater(() -> {
            try {
                switch (message.getType()) {
                    case DATA -> {
                        try {
                            // Decrypt the message content
                            String decryptedContent = vpnClient.getAesEncryption().decrypt(message.getContent());
                            
                            // Format the message with sender and timestamp
                            String formattedMessage = String.format("[%s] From: %s\nMessage: %s\n",
                                java.time.LocalDateTime.now().toString(),
                                message.getSender(),
                                decryptedContent);
                            
                            // Add to log
                            logTextArea.appendText(formattedMessage);
                            
                            // Scroll to bottom
                            logTextArea.setScrollTop(Double.MAX_VALUE);
                        } catch (Exception ex) {
                            logMessage("Error decrypting message: " + ex.getMessage());
                        }
                    }
                    case ERROR -> {
                        try {
                            // Handle error messages
                            String errorMessage = vpnClient.getAesEncryption().decrypt(message.getContent());
                            logMessage("Error from server: " + errorMessage);
                        } catch (Exception ex) {
                            logMessage("Error decrypting error message: " + ex.getMessage());
                        }
                    }
                    default -> {
                        logMessage("Received message of type: " + message.getType());
                    }
                }
            } catch (RuntimeException e) {
                logMessage("Error processing incoming message: " + e.getMessage());
                System.err.println("Error processing message: " + e.getMessage());
            }
        });
    }
    
    /**
     * Updates the UI for connected state
     */
    private void updateUIForConnected() {
        connectButton.setText("Disconnect");
        statusLabel.setText("Status: Connected");
        clientIdLabel.setText("Client ID: " + vpnClient.getClientId());
        serverAddressField.setEditable(false);
        portField.setEditable(false);
        sendButton.setDisable(false);
    }
    
    /**
     * Updates the UI for disconnected state
     */
    private void updateUIForDisconnected() {
        connectButton.setText("Connect");
        statusLabel.setText("Status: Disconnected");
        clientIdLabel.setText("Client ID: -");
        serverAddressField.setEditable(true);
        portField.setEditable(true);
        sendButton.setDisable(true);
    }
    
    /**
     * Logs a message to the log text area
     */
    private void logMessage(String message) {
        String timestamp = java.time.LocalDateTime.now().toString();
        logTextArea.appendText("[" + timestamp + "] " + message + "\n");
    }
    
    /**
     * Shows an error alert
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @Override
    public void stop() {
        if (vpnClient != null) {
            vpnClient.shutdown();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
