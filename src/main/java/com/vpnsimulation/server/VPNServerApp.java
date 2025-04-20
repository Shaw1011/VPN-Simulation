package com.vpnsimulation.server;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.vpnsimulation.common.database.ConnectionLog;
import com.vpnsimulation.common.database.DataTransferLog;
import com.vpnsimulation.common.database.SecurityEventLog;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * JavaFX application for the VPN server with real-time monitoring
 */
public class VPNServerApp extends Application {
    
    private static final int SERVER_PORT = 8443;
    
    private VPNServer vpnServer;
    private Timer refreshTimer;
    
    // UI components
    private Label statusLabel;
    private Label clientCountLabel;
    private TableView<ConnectionLog> connectionsTable;
    private TableView<DataTransferLog> dataTransfersTable;
    private TableView<SecurityEventLog> securityEventsTable;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private LineChart<Number, Number> trafficChart;
    private XYChart.Series<Number, Number> dataSeries;
    
    // Data
    private final ObservableList<ConnectionLog> connectionLogs = FXCollections.observableArrayList();
    private final ObservableList<DataTransferLog> dataTransferLogs = FXCollections.observableArrayList();
    private final ObservableList<SecurityEventLog> securityEventLogs = FXCollections.observableArrayList();
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize server
            vpnServer = new VPNServer(SERVER_PORT);
            vpnServer.initialize();
            
            // Create UI
            BorderPane root = new BorderPane();
            root.setPadding(new Insets(10));
            
            // Top panel with controls
            HBox topPanel = createTopPanel();
            root.setTop(topPanel);
            
            // Center panel with tabs
            TabPane tabPane = createTabPane();
            root.setCenter(tabPane);
            
            // Create scene
            Scene scene = new Scene(root, 900, 600);
            primaryStage.setScene(scene);
            primaryStage.setTitle("VPN Server Monitor");
            
            // Start server
            vpnServer.start();
            updateStatusLabel("Server started on port " + SERVER_PORT);
            
            // Start refresh timer
            startRefreshTimer();
            
            // Set close handler
            primaryStage.setOnCloseRequest(e -> {
                stop();
            });
            
            primaryStage.show();
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            showErrorAlert("Failed to start server", e.getMessage());
        }
    }
    
    /**
     * Creates the top panel with server controls
     */
    private HBox createTopPanel() {
        HBox topPanel = new HBox(10);
        topPanel.setPadding(new Insets(0, 0, 10, 0));
        
        statusLabel = new Label("Server status: Initializing...");
        clientCountLabel = new Label("Connected clients: 0");
        
        Button refreshButton = new Button("Refresh Data");
        refreshButton.setOnAction(e -> refreshData());
        
        topPanel.getChildren().addAll(statusLabel, clientCountLabel, refreshButton);
        
        return topPanel;
    }
    
    /**
     * Creates the tab pane with data tables
     */
    private TabPane createTabPane() {
        TabPane tabPane = new TabPane();
        
        // Connections tab
        Tab connectionsTab = new Tab("Connections");
        connectionsTab.setClosable(false);
        connectionsTab.setContent(createConnectionsPanel());
        
        // Data Transfers tab
        Tab dataTransfersTab = new Tab("Data Transfers");
        dataTransfersTab.setClosable(false);
        dataTransfersTab.setContent(createDataTransfersPanel());
        
        // Security Events tab
        Tab securityEventsTab = new Tab("Security Events");
        securityEventsTab.setClosable(false);
        securityEventsTab.setContent(createSecurityEventsPanel());
        
        // Traffic Graph tab
        Tab trafficGraphTab = new Tab("Traffic Graph");
        trafficGraphTab.setClosable(false);
        trafficGraphTab.setContent(createTrafficGraphPanel());
        
        tabPane.getTabs().addAll(connectionsTab, dataTransfersTab, securityEventsTab, trafficGraphTab);
        
        return tabPane;
    }
    
    /**
     * Creates the connections panel
     */
    private VBox createConnectionsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        
        // Date filter
        HBox dateFilterBox = new HBox(10);
        Label startDateLabel = new Label("Start Date:");
        startDatePicker = new DatePicker(LocalDate.now().minusDays(7));
        
        Label endDateLabel = new Label("End Date:");
        endDatePicker = new DatePicker(LocalDate.now());
        
        Button filterButton = new Button("Filter");
        filterButton.setOnAction(e -> refreshData());
        
        dateFilterBox.getChildren().addAll(startDateLabel, startDatePicker, endDateLabel, endDatePicker, filterButton);
        
        // Connections table
        connectionsTable = new TableView<>();
        connectionsTable.setPlaceholder(new Label("No connections"));
        
        TableColumn<ConnectionLog, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        
        TableColumn<ConnectionLog, String> clientIdColumn = new TableColumn<>("Client ID");
        clientIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getClientId()));
        
        TableColumn<ConnectionLog, String> clientIpColumn = new TableColumn<>("Client IP");
        clientIpColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getClientIp()));
        
        TableColumn<ConnectionLog, String> connectionTimeColumn = new TableColumn<>("Connection Time");
        connectionTimeColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getConnectionTime().toString()));
        
        TableColumn<ConnectionLog, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getConnectionStatus()));
        
        connectionsTable.getColumns().addAll(idColumn, clientIdColumn, clientIpColumn, connectionTimeColumn, statusColumn);
        connectionsTable.setItems(connectionLogs);
        
        // Add selection listener to show related data transfers
        connectionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                try {
                    loadDataTransferLogs(newVal.getId());
                    loadSecurityEventLogs(newVal.getId());
                } catch (Exception e) {
                    System.err.println("Error loading logs: " + e.getMessage());
                    showErrorAlert("Error loading logs", e.getMessage());
                }
            }
        });
        
        panel.getChildren().addAll(dateFilterBox, connectionsTable);
        
        return panel;
    }
    
    /**
     * Creates the data transfers panel
     */
    private VBox createDataTransfersPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        
        dataTransfersTable = new TableView<>();
        dataTransfersTable.setPlaceholder(new Label("No data transfers"));
        
        TableColumn<DataTransferLog, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        
        TableColumn<DataTransferLog, Integer> connectionIdColumn = new TableColumn<>("Connection ID");
        connectionIdColumn.setCellValueFactory(cellData -> 
                new SimpleIntegerProperty(cellData.getValue().getConnectionId()).asObject());
        
        TableColumn<DataTransferLog, String> timestampColumn = new TableColumn<>("Timestamp");
        timestampColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getTimestamp().toString()));
        
        TableColumn<DataTransferLog, String> sourceIpColumn = new TableColumn<>("Source IP");
        sourceIpColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getSourceIp()));
        
        TableColumn<DataTransferLog, String> destIpColumn = new TableColumn<>("Destination IP");
        destIpColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getDestinationIp()));
        
        TableColumn<DataTransferLog, Integer> sizeColumn = new TableColumn<>("Size (bytes)");
        sizeColumn.setCellValueFactory(cellData -> 
                new SimpleIntegerProperty(cellData.getValue().getDataSize()).asObject());
        
        TableColumn<DataTransferLog, String> encryptedColumn = new TableColumn<>("Encrypted");
        encryptedColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(String.valueOf(cellData.getValue().isEncrypted())));
        
        dataTransfersTable.getColumns().addAll(idColumn, connectionIdColumn, timestampColumn, 
                sourceIpColumn, destIpColumn, sizeColumn, encryptedColumn);
        dataTransfersTable.setItems(dataTransferLogs);
        
        panel.getChildren().add(dataTransfersTable);
        
        return panel;
    }
    
    /**
     * Creates the security events panel
     */
    private VBox createSecurityEventsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        
        securityEventsTable = new TableView<>();
        securityEventsTable.setPlaceholder(new Label("No security events"));
        
        TableColumn<SecurityEventLog, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        
        TableColumn<SecurityEventLog, Integer> connectionIdColumn = new TableColumn<>("Connection ID");
        connectionIdColumn.setCellValueFactory(cellData -> 
                new SimpleIntegerProperty(cellData.getValue().getConnectionId()).asObject());
        
        TableColumn<SecurityEventLog, String> timestampColumn = new TableColumn<>("Timestamp");
        timestampColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getTimestamp().toString()));
        
        TableColumn<SecurityEventLog, String> eventTypeColumn = new TableColumn<>("Event Type");
        eventTypeColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getEventType()));
        
        TableColumn<SecurityEventLog, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getDescription()));
        
        TableColumn<SecurityEventLog, String> severityColumn = new TableColumn<>("Severity");
        severityColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getSeverity()));
        
        securityEventsTable.getColumns().addAll(idColumn, connectionIdColumn, timestampColumn, 
                eventTypeColumn, descriptionColumn, severityColumn);
        securityEventsTable.setItems(securityEventLogs);
        
        panel.getChildren().add(securityEventsTable);
        
        return panel;
    }
    
    /**
     * Creates the traffic graph panel
     */
    private VBox createTrafficGraphPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        
        // Create chart
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Time (s)");
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Data Size (bytes)");
        
        trafficChart = new LineChart<>(xAxis, yAxis);
        trafficChart.setTitle("Data Transfer Traffic");
        trafficChart.setAnimated(false);
        
        dataSeries = new XYChart.Series<>();
        dataSeries.setName("Data Size");
        
        trafficChart.getData().add(dataSeries);
        
        panel.getChildren().add(trafficChart);
        
        return panel;
    }
    
    /**
     * Starts the refresh timer to update the data periodically
     */
    private void startRefreshTimer() {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    try {
                        // Update client count
                        clientCountLabel.setText("Connected clients: " + vpnServer.getClientCount());
                        
                        // Refresh data transfers if a connection is selected
                        ConnectionLog selectedConnection = connectionsTable.getSelectionModel().getSelectedItem();
                        if (selectedConnection != null) {
                            loadDataTransferLogs(selectedConnection.getId());
                        }
                    } catch (Exception e) {
                        System.err.println("Error updating UI: " + e.getMessage());
                    }
                });
            }
        }, 0, 1000); // Refresh every second
    }
    
    /**
     * Refreshes all data in the tables
     */
    private void refreshData() {
        try {
            loadConnectionLogs();
            loadAllDataTransferLogs(); // Load all data transfers
            
            // Clear security events if no connection is selected
            if (connectionsTable.getSelectionModel().getSelectedItem() == null) {
                securityEventLogs.clear();
            }
        } catch (Exception e) {
            System.err.println("Error refreshing data: " + e.getMessage());
            showErrorAlert("Error refreshing data", e.getMessage());
        }
    }
    
    /**
     * Loads connection logs from the database
     */
    private void loadConnectionLogs() throws Exception {
        connectionLogs.clear();
        
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        if (startDate != null && endDate != null) {
            Date sqlStartDate = Date.valueOf(startDate);
            Date sqlEndDate = Date.valueOf(endDate);
            
            List<ConnectionLog> logs = vpnServer.getDatabaseManager().getConnectionLogs(sqlStartDate, sqlEndDate);
            connectionLogs.addAll(logs);
        }
    }
    
    /**
     * Loads all data transfer logs
     */
    private void loadAllDataTransferLogs() throws Exception {
        dataTransferLogs.clear();
        
        List<DataTransferLog> logs = vpnServer.getDatabaseManager().getAllDataTransferLogs();
        dataTransferLogs.addAll(logs);
        
        // Update traffic chart
        updateTrafficChart(logs);
    }
    
    /**
     * Updates the traffic chart with the given logs
     */
    private void updateTrafficChart(List<DataTransferLog> logs) {
        dataSeries.getData().clear();
        int timeOffset = 0;
        for (DataTransferLog log : logs) {
            dataSeries.getData().add(new XYChart.Data<>(timeOffset++, log.getDataSize()));
        }
    }
    
    /**
     * Loads data transfer logs for a specific connection
     */
    private void loadDataTransferLogs(int connectionId) throws Exception {
        dataTransferLogs.clear();
        
        List<DataTransferLog> logs = vpnServer.getDatabaseManager().getDataTransferLogs(connectionId);
        dataTransferLogs.addAll(logs);
        
        // Update traffic chart
        dataSeries.getData().clear();
        int timeOffset = 0;
        for (DataTransferLog log : logs) {
            dataSeries.getData().add(new XYChart.Data<>(timeOffset++, log.getDataSize()));
        }
    }
    
    /**
     * Loads security event logs for a specific connection
     */
    private void loadSecurityEventLogs(int connectionId) throws Exception {
        securityEventLogs.clear();
        
        List<SecurityEventLog> logs = vpnServer.getDatabaseManager().getSecurityEventLogs(connectionId);
        securityEventLogs.addAll(logs);
    }
    
    /**
     * Updates the status label
     */
    private void updateStatusLabel(String status) {
        statusLabel.setText("Server status: " + status);
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
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
        
        if (vpnServer != null) {
            vpnServer.stop();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
