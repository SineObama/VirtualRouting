@echo off
set /a max=%1-1
if %max% LSS 0 (
    echo 请输入大于0的参数
    pause
) else (
    :start "jar" cmd /c "cd .. && jar.bat || pause"
    for /l %%i in (0,1,%max%) do (
        :start "client%%i" cmd /c "java -jar ../Client.jar %%i ./%1/%2/ || pause"
        start "client%%i" cmd /c "cd ../bin && java selforganized.Client %%i ../sample/%1/%2/ || pause"
        :echo wscript.sleep 40>sleep.vbs
        :@cscript sleep.vbs >nul
    )
    :del /f /s /q sleep.vbs
    pause
    for /l %%j in (0,1,%max%) do (
        TASKKILL /F /FI "WINDOWTITLE eq client%%j"
    )
    for /l %%j in (0,1,%max%) do (
        TASKKILL /F /FI "WINDOWTITLE eq client%%j"
    )
)