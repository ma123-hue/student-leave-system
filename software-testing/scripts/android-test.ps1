param(
    [string]$Adb = "adb",
    [string]$Apk = "..\..\android-webview\app\build\outputs\apk\debug\app-debug.apk",
    [string]$OutputDir = "..\artifacts\android"
)

$ErrorActionPreference = "Stop"
$package = "com.example.studentleave.webview.debug"
$activity = "$package/com.example.studentleave.webview.MainActivity"
$output = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot $OutputDir))
$apkPath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot $Apk))
New-Item -ItemType Directory -Force -Path $output | Out-Null

& $Adb wait-for-device
& $Adb install -r $apkPath | Tee-Object (Join-Path $output "install.txt")
& $Adb logcat -c
& $Adb shell am force-stop $package
& $Adb shell am start -W -n $activity | Tee-Object (Join-Path $output "launch.txt")
Start-Sleep -Seconds 5

& $Adb shell getprop ro.product.model | Set-Content (Join-Path $output "device-model.txt")
& $Adb shell getprop ro.build.version.sdk | Set-Content (Join-Path $output "android-api.txt")
& $Adb shell dumpsys package com.google.android.webview | Select-String "versionName" | Set-Content (Join-Path $output "webview-version.txt")
& $Adb shell dumpsys activity activities | Set-Content (Join-Path $output "dumpsys-activity.txt")
& $Adb shell dumpsys connectivity | Set-Content (Join-Path $output "dumpsys-connectivity-online.txt")
& $Adb shell screencap -p /sdcard/student-leave-online.png
& $Adb pull /sdcard/student-leave-online.png (Join-Path $output "online.png") | Out-Null

& $Adb shell svc wifi disable
& $Adb shell svc data disable
Start-Sleep -Seconds 3
& $Adb shell am force-stop $package
& $Adb shell am start -W -n $activity | Tee-Object (Join-Path $output "launch-offline.txt")
Start-Sleep -Seconds 5
& $Adb shell dumpsys connectivity | Set-Content (Join-Path $output "dumpsys-connectivity-offline.txt")
& $Adb shell screencap -p /sdcard/student-leave-offline.png
& $Adb pull /sdcard/student-leave-offline.png (Join-Path $output "offline.png") | Out-Null

& $Adb shell svc wifi enable
& $Adb shell svc data enable
Start-Sleep -Seconds 5
& $Adb shell input keyevent 82
& $Adb shell am force-stop $package
& $Adb shell am start -W -n $activity | Tee-Object (Join-Path $output "launch-recovered.txt")
Start-Sleep -Seconds 5
& $Adb logcat -d -v threadtime StudentLeaveQA:V chromium:W AndroidRuntime:E '*:S' | Set-Content (Join-Path $output "logcat.txt")
& $Adb bugreport (Join-Path $output "student-leave-bugreport.zip")

Write-Output "Android test artifacts: $output"
