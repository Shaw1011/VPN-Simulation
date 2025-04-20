# VPN Simulation Project in Java

This project implements a secure client-server-based VPN simulation that encrypts data using AES encryption and Diffie-Hellman key exchange, routes traffic through a server, and logs all communication in an SQLite database with a real-time monitoring GUI.

## Project Features

- **Client-Server Communication**: Uses Java Sockets (ServerSocket, Socket) with multithreading to handle multiple clients concurrently.
- **Secure Data Encryption**: Implements AES encryption and Diffie-Hellman key exchange for secure communication.
- **Certificate Authentication**: Uses X.509 certificates for client and server authentication.
- **Traffic Routing**: The server acts as an intermediary, forwarding client requests to destinations.
- **Database Logging**: Stores logs of all communication events in an SQLite database.
- **Real-time Monitoring GUI**: JavaFX-based GUI for monitoring VPN activity with filtering options.

## Project Structure

```
src/main/java/com/vpnsimulation/
├── client/
│   ├── VPNClient.java           # Core VPN client implementation
│   └── VPNClientApp.java        # JavaFX GUI for the client
├── server/
│   ├── VPNServer.java           # Core VPN server implementation
│   └── VPNServerApp.java        # JavaFX GUI for the server with monitoring
└── common/
    ├── encryption/
    │   ├── AESEncryption.java   # AES encryption implementation
    │   └── DiffieHellmanKeyExchange.java  # Key exchange implementation
    ├── database/
    │   ├── DatabaseManager.java # SQLite database operations
    │   ├── ConnectionLog.java   # Connection log model
    │   ├── DataTransferLog.java # Data transfer log model
    │   └── SecurityEventLog.java # Security event log model
    └── util/
        ├── CertificateManager.java # X.509 certificate management
        └── Message.java           # Message model for communication
```

## Technical Requirements

- Java 11 or higher
- Maven for dependency management

## Dependencies

- SQLite JDBC Driver for database operations
- JavaFX for the GUI components
- Bouncy Castle for cryptography operations
- SLF4J for logging

## How to Run

### Building the Project

```bash
mvn clean package
```

### Running the Server

```bash
mvn javafx:run -Djavafx.mainClass=com.vpnsimulation.server.VPNServerApp
```

### Running the Client

```bash
mvn javafx:run -Djavafx.mainClass=com.vpnsimulation.client.VPNClientApp
```

## Usage Instructions

1. **Start the Server**: Launch the VPN Server application first.
2. **Start the Client**: Launch the VPN Client application.
3. **Connect**: In the client application, enter the server address (default: localhost) and port (default: 8443), then click "Connect".
4. **Send Messages**: After connecting, you can send encrypted messages through the VPN tunnel.
5. **Monitor Traffic**: Use the server application to monitor connections, data transfers, and security events.

## Security Features

- **Encryption**: All data is encrypted with AES-GCM for confidentiality and integrity.
- **Authentication**: X.509 certificates verify the identity of clients and server.
- **Secure Key Exchange**: Diffie-Hellman key exchange for secure key sharing.

## Implementation Notes

This simulation focuses on demonstrating the core principles of VPN technology and secure communication. In a production environment, additional security measures and optimizations would be needed.

## Future Enhancements

- Implement multiple destination routing
- Add packet filtering capabilities
- Implement bandwidth monitoring and throttling
- Add user authentication with username/password
- Support for multiple encryption algorithms
