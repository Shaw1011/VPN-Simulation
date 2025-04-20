# VPN Simulation Project Upgrade Notes

## Changes Made

### Java Version
- Updated from Java 11 to Java 17 (LTS version)
  - Both source and target compatibility have been updated
  - This provides better security, performance, and access to newer language features

### Dependencies Updated
- **SQLite JDBC**: 3.41.2.1 → 3.49.1.0
  - Latest version with improved performance and bug fixes
  - Adds GraalVM native image support

- **JavaFX**: 17.0.2 → 21.0.6 (LTS version)
  - Modern UI capabilities
  - Performance improvements
  - Better support for modern displays

- **Bouncy Castle**: 1.70 → 1.80
  - Upgraded from jdk15on to jdk18on artifacts
  - Latest cryptographic algorithms and security fixes

- **SLF4J**: 2.0.5 → 2.0.16
  - Latest logging improvements
  - Better performance

- **Maven Plugins**:
  - maven-compiler-plugin: 3.10.1 → 3.11.0
  - Added exec-maven-plugin 3.1.1 for better command-line execution

### Build Script Improvements
- Fixed batch files for running server and client
  - Created separate execution profiles for server and client in Maven
  - This resolves port conflicts when running both applications
  - Scripts now use `exec:java@server` and `exec:java@client` respectively

## Running the Application

The application can be run the same way as before:

1. Server: `run-server.bat`
2. Client: `run-client.bat`

**Important**: Always start the server first before starting the client.

## Requirements

- Java 17 or later must be installed
- Maven should automatically download the updated dependencies

## Potential Future Improvements

1. **Java Module System**: Consider modularizing the application for better encapsulation
2. **Native Image Compilation**: Use GraalVM to create a native executable for faster startup
3. **Security Improvements**: Further review of cryptographic implementations
4. **UI Modernization**: Leverage newer JavaFX components and designs
5. **Testing Framework**: Add comprehensive tests 