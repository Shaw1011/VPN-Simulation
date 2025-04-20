@echo off
echo Starting VPN Server...
mvn exec:java@server -Dexec.classpathScope=runtime
pause
