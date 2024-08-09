@echo off
setlocal

if "%~3"=="" (
    echo Usage: %~nx0 file1 file2 output_file
    exit /b 1
)

set "file1=%~1"
set "file2=%~2"
set "output_file=%~3"

for %%I in ("%output_file%") do set "output_dir=%%~dpI"

if not exist "%output_dir%" (
    echo Output directory does not exist: %output_dir%
    exit /b 1
)

java -jar "C:\Users\Admin\Documents\Java_Files\xml-comparer\xml-comparer\target\xml-comparer-0.0.1-SNAPSHOT.jar" "%file1%" "%file2%" "%output_file%"

endlocal
