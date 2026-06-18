# Quick test runner for cpally:// deep link scenarios.
# Usage:
#   .\dev-test-protocol.ps1             - interactive menu
#   .\dev-test-protocol.ps1 cold 1A     - cold-launch problem 1A
#   .\dev-test-protocol.ps1 hot  2208A  - hot-handoff problem 2208A
#   .\dev-test-protocol.ps1 log         - tail diagnostics.log

param(
    [string]$Mode = "",
    [string]$Code = "1A"
)

$logPath = "$env:APPDATA\CompetitiveProgrammingAlly\diagnostics.log"

function Show-Log {
    if (Test-Path $logPath) {
        Write-Host "`n--- Last 30 lines of diagnostics.log ---" -ForegroundColor Cyan
        Get-Content $logPath -Tail 30
    } else {
        Write-Host "Log not found: $logPath" -ForegroundColor Yellow
        Write-Host "(App hasn't written it yet — check if the app started at all.)"
    }
}

function Launch-Url([string]$url) {
    Write-Host "Launching: $url" -ForegroundColor Yellow
    Start-Process $url
    Start-Sleep -Seconds 3
    Show-Log
}

switch ($Mode.ToLower()) {
    "cold" {
        Write-Host "Cold-launch test. Close the app now if it is running, then press Enter." -ForegroundColor Green
        Read-Host
        Launch-Url "cpally://problem/$Code"
    }
    "hot" {
        Write-Host "Hot-handoff test. The app must already be running." -ForegroundColor Green
        Write-Host "Press Enter to send the URL."
        Read-Host
        Launch-Url "cpally://problem/$Code"
    }
    "log" {
        Show-Log
    }
    default {
        Write-Host "cpally:// dev test tool" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "1) Cold launch  - app is NOT running; link should open it and load problem"
        Write-Host "2) Hot handoff  - app IS running; link should load problem in running window"
        Write-Host "3) Show log"
        Write-Host "4) Exit"
        Write-Host ""
        $choice = Read-Host "Choice"
        switch ($choice) {
            "1" {
                $code = Read-Host "Problem code (e.g. 1A)"
                Write-Host "Close the app if it is open, then press Enter."
                Read-Host
                Launch-Url "cpally://problem/$code"
            }
            "2" {
                $code = Read-Host "Problem code (e.g. 2208A)"
                Write-Host "Make sure the app is already running, then press Enter."
                Read-Host
                Launch-Url "cpally://problem/$code"
            }
            "3" { Show-Log }
        }
    }
}
