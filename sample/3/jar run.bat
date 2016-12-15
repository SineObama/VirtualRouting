start "jar" cmd /c jar.bat
pause
start "client0" cmd /c "java -jar Client.jar 0"
start "client1" cmd /c "java -jar Client.jar 1"
start "client2" cmd /c "java -jar Client.jar 2"
pause
TASKKILL /F /FI "WINDOWTITLE eq client0"
TASKKILL /F /FI "WINDOWTITLE eq client1"
TASKKILL /F /FI "WINDOWTITLE eq client2"
TASKKILL /F /FI "WINDOWTITLE eq client0"
TASKKILL /F /FI "WINDOWTITLE eq client1"
TASKKILL /F /FI "WINDOWTITLE eq client2"
