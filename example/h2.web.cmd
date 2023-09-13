@echo off
setlocal
set JAVA_BINARY="java"
if exist "java" set JAVA_BINARY=".\java\bin\java.exe"
if exist "java-home" set JAVA_BINARY=".\java-home\bin\java.exe"
if exist "jdk-17.0.7+7" set JAVA_BINARY=".\jdk-17.0.7+7\bin\java.exe"
if exist "jdk-17.0.7+7-jre" set JAVA_BINARY=".\jdk-17.0.7+7-jre\bin\java.exe"
if exist "jdk-17.0.8+7" set JAVA_BINARY=".\jdk-17.0.8+7\bin\java.exe"
if exist "jdk-17.0.8+7-jre" set JAVA_BINARY=".\jdk-17.0.8+7-jre\bin\java.exe"

set H2_JAR="h2-2.2.222.jar"
if exist ".\plugin-libraries\com\h2database\h2\2.1.222\h2-2.2.222.jar" set JAVA_BINARY=".\plugin-libraries\com\h2database\h2\2.1.222\h2-2.2.222.jar"
if exist ".\plugin-libraries\com\h2database\h2\2.1.214\h2-2.1.214.jar" set JAVA_BINARY=".\plugin-libraries\com\h2database\h2\2.1.214\h2-2.1.214.jar"

%JAVA_BINARY% -version
%JAVA_BINARY% -jar %H2_JAR% -url jdbc:h2:./data/xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin/hibernate.h2;AUTO_SERVER=TRUE %*

set EL=%ERRORLEVEL%
if %EL% NEQ 0 (
    echo Process exited with %EL%
    pause
)