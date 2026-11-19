<#
.SYNOPSIS
    Builds the APK (and runs the tests) of "Khto khochhe stati tysiachnykom?".
.DESCRIPTION
    Picks a JDK 17..23 automatically (Gradle 8.x does not run on JDK 24+), then delegates to the
    Gradle wrapper. Extra tasks can be passed with -Tasks.
.EXAMPLE
    .\scripts\build-apk.ps1                       # testDebugUnitTest + assembleDebug
    .\scripts\build-apk.ps1 -Tasks assembleRelease
#>
[CmdletBinding()]
param(
    [string[]] $Tasks = @('testDebugUnitTest', 'assembleDebug'),
    [string] $JdkHome = ''
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

function Test-Jdk([string] $path) {
    if ([string]::IsNullOrWhiteSpace($path)) { return $false }
    $javaExe = Join-Path $path 'bin\java.exe'
    if (-not (Test-Path $javaExe)) { return $false }
    $version = (& $javaExe -version 2>&1 | Select-Object -First 1) -join ''
    if ($version -match '"(\d+)') {
        $major = [int] $Matches[1]
        return $major -ge 17 -and $major -le 23
    }
    return $false
}

if (-not (Test-Jdk $JdkHome)) {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $candidates += 'C:\Program Files\Android\Android Studio\jbr'
    $candidates += Get-ChildItem 'C:\Program Files\Android\openjdk' -Directory -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
    $candidates += Get-ChildItem 'C:\Program Files\Java' -Directory -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
    $candidates += Get-ChildItem (Join-Path $env:USERPROFILE '.gradle\jdks') -Directory -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
    $found = $candidates | Where-Object { Test-Jdk $_ } | Select-Object -First 1
    if (-not $found) {
        throw 'No JDK 17-23 found. Install one (JDK 21 recommended) or pass -JdkHome C:\path\to\jdk.'
    }
    $JdkHome = $found
}

$env:JAVA_HOME = $JdkHome
$env:Path = (Join-Path $JdkHome 'bin') + ';' + $env:Path
Write-Host "Using JDK: $JdkHome"

if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT) {
    $defaultSdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path $defaultSdk) { $env:ANDROID_HOME = $defaultSdk }
}

$gradlew = Join-Path $projectRoot 'gradlew.bat'
Write-Host "Gradle tasks: $($Tasks -join ' ')"
& $gradlew @Tasks --console=plain
exit $LASTEXITCODE