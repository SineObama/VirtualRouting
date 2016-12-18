@echo off
set /a max=%1-1
set title="client"
if %max% LSS 0 (
    echo 请输入大于0的参数
    pause
) else (
    :start "jar" cmd /c "cd .. && jar.bat || pause"
    for /l %%i in (0,1,%max%) do (
        :start %title%%%i cmd /c "java -jar ../Client.jar %%i ./%1/%2/ || pause"
        start "client%%i" cmd /c "cd ../bin && java selforganized.Client ../sample/%1/%2/client%%i.txt || pause"
    )
    pause
    for /l %%j in (0,1,%max%) do (
        TASKKILL /F /FI "WINDOWTITLE eq %title%%%j"
    )
    for /l %%j in (0,1,%max%) do (
        TASKKILL /F /FI "WINDOWTITLE eq %title%%%j"
    )
)
