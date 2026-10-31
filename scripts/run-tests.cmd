@echo off
rem Runs Gradle tasks that can install or uninstall the app, on the TEST emulator only.
rem   set ANDROID_SERIAL=emulator-5560
rem   scripts\run-tests.cmd testDebugUnitTest lintDebug connectedDebugAndroidTest
rem Refuses unless ANDROID_SERIAL is the AVD "Pixel_API_34_test" (checked with "adb emu avd name",
rem so a phone or the manual emulator "Pixel_API_34" can never be picked). Always leaves the app
rem installed after instrumented tests: an uninstall would delete the data on the device.
rem See CLAUDE.md section 0b and the Hard rules.
setlocal EnableExtensions
set "TEST_AVD=Pixel_API_34_test"
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not exist "%ADB%" set "ADB=adb"

if "%ANDROID_SERIAL%"=="" (
    echo run-tests: ANDROID_SERIAL is not set. Set it to the serial of %TEST_AVD%, for example emulator-5560.
    exit /b 2
)

set "AVD="
for /f "usebackq delims=" %%L in (`""%ADB%" -s %ANDROID_SERIAL% emu avd name 2^>nul"`) do if not defined AVD set "AVD=%%L"
if /i not "%AVD%"=="%TEST_AVD%" (
    echo run-tests: refusing. %ANDROID_SERIAL% is "%AVD%", not the test emulator %TEST_AVD%.
    exit /b 3
)

rem Lets the Gradle guard in app/build.gradle.kts allow connected and uninstall tasks.
set "PILLPALS_TEST_RUNNER=1"
pushd "%~dp0.."
call "%CD%\gradlew.bat" %* "-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true"
set "RESULT=%ERRORLEVEL%"
popd
exit /b %RESULT%
