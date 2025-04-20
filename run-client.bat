@echo off
echo Starting VPN Client...
mvn exec:java@client -Dexec.classpathScope=runtime
pause
