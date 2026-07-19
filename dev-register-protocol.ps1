# Registers cpally:// in HKCU (no admin required) pointing at the dev jar.
# Run once after each 'mvn package'. Override is user-scoped and takes
# precedence over any installed handler from InnoSetup.

param(
    [string]$ProblemCode = ""   # optional: immediately test after registering
)

$ErrorActionPreference = "Stop"

# --- Locate java.exe ---
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    Write-Error "java not found in PATH. Is JAVA_HOME set?"
    exit 1
}
$javaExe = $javaCmd.Source
Write-Host "Java:  $javaExe"

# --- Locate the jar ---
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$jar = Join-Path $scriptDir "target\cp-ally-ide-0.3.0.jar"
if (-not (Test-Path $jar)) {
    Write-Error "Jar not found at $jar`nRun 'mvn package' first."
    exit 1
}
Write-Host "Jar:   $jar"

# --- Build the open command ---
# %1 is the full cpally:// URL passed by Windows.
$openCmd = "`"$javaExe`" -jar `"$jar`" `"%1`""
Write-Host "Cmd:   $openCmd"

# --- Write to HKCU\Software\Classes (user-level, no admin) ---
$base = "HKCU:\Software\Classes\cpally"
New-Item -Path $base -Force | Out-Null
Set-ItemProperty -Path $base -Name "(Default)"    -Value "URL:CP Ally Protocol"
New-Item -Path "$base\DefaultIcon" -Force | Out-Null
Set-ItemProperty -Path "$base\DefaultIcon" -Name "(Default)" -Value "$javaExe,0"
New-Item -Path "$base\shell\open\command" -Force | Out-Null
Set-ItemProperty -Path "$base\shell\open\command" -Name "(Default)" -Value $openCmd

# The "URL Protocol" value must exist (even empty) for Windows to treat this as a URL scheme.
$key = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey("Software\Classes\cpally", $true)
$key.SetValue("URL Protocol", "", [Microsoft.Win32.RegistryValueKind]::String)
$key.Close()

Write-Host ""
Write-Host "cpally:// registered. Test scenarios:" -ForegroundColor Green
Write-Host "  Cold launch:   start cpally://problem/1A     (close the app first)"
Write-Host "  Hot handoff:   start cpally://problem/2208A  (leave the app running)"
Write-Host ""
Write-Host "Log file: $env:APPDATA\CompetitiveProgrammingAlly\diagnostics.log" -ForegroundColor Cyan

# Optional immediate test
if ($ProblemCode -ne "") {
    Write-Host ""
    Write-Host "Launching: cpally://problem/$ProblemCode" -ForegroundColor Yellow
    Start-Process "cpally://problem/$ProblemCode"
}
