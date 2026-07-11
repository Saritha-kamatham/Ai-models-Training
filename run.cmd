@echo off
title FoodDash Delivery Application
echo ====================================================================
echo Starting Food Delivery Application...
echo ====================================================================
echo.
call mvnw spring-boot:run
if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: Application failed to start.
    echo Please make sure the database is running and maven-wrapper.jar is in .mvn/wrapper/
    echo.
)
pause
