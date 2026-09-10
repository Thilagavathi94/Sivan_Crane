@echo off
cd /d "%~dp0"
start "" java -jar crane-management.jar
timeout /t 10
start "" http://localhost:8080