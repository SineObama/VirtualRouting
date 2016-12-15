start "jar" cmd /c jar.bat
pause
start "client0" cmd /c "java -jar Client.jar 0"
start "client1" cmd /c "java -jar Client.jar 1"
start "client2" cmd /c "java -jar Client.jar 2"
start "client1" cmd /c "java -jar Client.jar 3"
start "client2" cmd /c "java -jar Client.jar 4"
pause
TASKKILL /F /FI "WINDOWTITLE eq client0"
TASKKILL /F /FI "WINDOWTITLE eq client1"
TASKKILL /F /FI "WINDOWTITLE eq client2"
TASKKILL /F /FI "WINDOWTITLE eq client3"
TASKKILL /F /FI "WINDOWTITLE eq client4"
TASKKILL /F /FI "WINDOWTITLE eq client0"
TASKKILL /F /FI "WINDOWTITLE eq client1"
TASKKILL /F /FI "WINDOWTITLE eq client2"
TASKKILL /F /FI "WINDOWTITLE eq client3"
TASKKILL /F /FI "WINDOWTITLE eq client4"
