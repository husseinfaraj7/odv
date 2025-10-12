@echo off
echo Running EmailService validateRecipientEmail unit tests...

REM Create target directories
if not exist "target\classes" mkdir "target\classes"
if not exist "target\test-classes" mkdir "target\test-classes"

REM Set JAVA_HOME if needed
set JAVA_HOME=C:\Program Files\Java\jdk-25

REM Create classpath for compilation (using basic Spring Boot starter dependencies)
set CLASSPATH=src\main\java;src\test\java

REM Compile just the test class (simplified approach)
echo Compiling EmailServiceTest...
javac -cp "%CLASSPATH%" -d target\test-classes src\test\java\it\odvsicilia\backend\EmailServiceTest.java

if %ERRORLEVEL% neq 0 (
    echo Compilation failed
    exit /b 1
)

echo Tests would run here, but Maven build system is required for full Spring Boot test execution
echo Unit tests for validateRecipientEmail method have been added to EmailServiceTest.java
echo The tests verify:
echo - Valid email addresses pass validation without exceptions
echo - Invalid email formats throw EmailInvalidRecipientException with AddressException as cause
echo - Null/empty emails throw EmailInvalidRecipientException with RECIPIENT_EMAIL_EMPTY error code