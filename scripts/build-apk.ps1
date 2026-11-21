<#
.SYNOPSIS
    Builds the APK (and runs the tests) of "Yak stati milyonerom".
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
    [string] $JdkHome = '',
# The patch version is bumped before every build; pass -BumpVersion:$false to skip it.
    [bool] $BumpVersion = $true
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

# When invoked through powershell -File, "-Tasks a,b" arrives as one string; split it.
$Tasks = @($Tasks | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ })

function Test-Jdk([string] $path) {
    if ([string]::IsNullOrWhiteSpace($path)) { return $false }
    $javaExe = Join-Path $path 'bin\java.exe'
    if (-not (Test-Path $javaExe)) { return $false }
    # java -version writes to stderr; merge it in cmd so PowerShell does not raise
    # a NativeCommandError under $ErrorActionPreference = 'Stop'.
    $version = ((cmd /c "`"$javaExe`" -version 2>&1") | Select-Object -First 1) -join ''
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

# --- Automatic versioning ----------------------------------------------------
# The single source of truth for the version is gradle.properties. It is bumped
# here, before Gradle reads it, so every APK that leaves this script carries a
# unique, increasing version name and version code.

function Get-GradleProperty {
    param([string] $Text, [string] $Key)
    $found = [regex]::Match($Text, ('(?m)^' + [regex]::Escape($Key) + '=(.*)$'))
    if (-not $found.Success) { throw ('Property ' + $Key + ' not found in gradle.properties.') }
    return $found.Groups[1].Value.Trim()
}

function Set-GradleProperty {
    param([string] $Text, [string] $Key, [string] $Value)
    return [regex]::Replace($Text, ('(?m)^' + [regex]::Escape($Key) + '=.*$'), ($Key + '=' + $Value))
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$propsPath = Join-Path $projectRoot 'gradle.properties'
$propsText = [System.IO.File]::ReadAllText($propsPath)
$versionCode = [int] (Get-GradleProperty $propsText 'appVersionCode')
$versionName = Get-GradleProperty $propsText 'appVersionName'

if ($BumpVersion) {
    $parts = $versionName.Split('.')
    while ($parts.Count -lt 3) { $parts += '0' }
    $parts[2] = ([int] $parts[2] + 1).ToString()
    $versionName = ($parts -join '.')
    $versionCode = $versionCode + 1
    $propsText = Set-GradleProperty $propsText 'appVersionCode' $versionCode.ToString()
    $propsText = Set-GradleProperty $propsText 'appVersionName' $versionName
    [System.IO.File]::WriteAllText($propsPath, $propsText, $utf8NoBom)
    Write-Host ('Version bumped to ' + $versionName + ' (code ' + $versionCode + ').') -ForegroundColor Yellow
} else {
    Write-Host ('Building version ' + $versionName + ' (code ' + $versionCode + ') unchanged.') -ForegroundColor Yellow
}

$gradlew = Join-Path $projectRoot 'gradlew.bat'
Write-Host "Gradle tasks: $($Tasks -join ' ')"
# Gradle reports warnings on stderr; do not let them abort the script.
$ErrorActionPreference = 'Continue'
& $gradlew @Tasks --console=plain
$gradleExit = $LASTEXITCODE

# --- Publish the APK into dist -----------------------------------------------
if ($gradleExit -eq 0) {
    $apkRoot = Join-Path $projectRoot 'androidApp\build\outputs\apk'
    $built = @(Get-ChildItem -Path $apkRoot -Recurse -Filter '*.apk' -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike '*-unsigned.apk' })
    if ($built.Count -eq 0) {
        Write-Warning 'The build produced no APK - nothing was published to dist.'
    } else {
        $distDir = Join-Path $projectRoot 'dist'
        if (-not (Test-Path $distDir)) { New-Item -ItemType Directory -Path $distDir | Out-Null }
        $stamp = Get-Date -Format 'yyyyMMdd-HHmm'
        foreach ($apk in $built) {
            $target = 'millionaire-' + $apk.Directory.Name + '-' + $versionName + '-' + $stamp + '.apk'
            Copy-Item -LiteralPath $apk.FullName -Destination (Join-Path $distDir $target) -Force
            Write-Host ('Published ' + $target) -ForegroundColor Green
        }
        $cards = $null
        foreach ($file in (Get-ChildItem -Path $distDir -Filter '*.apk' | Sort-Object LastWriteTime -Descending)) {
            $mb = [math]::Round($file.Length / 1MB, 1)
            $cards += '  <a class=card href=' + $file.Name + '>' + $file.Name + ' &mdash; ' + $mb + ' MB</a>' + [Environment]::NewLine
        }
        $page = @()
        $page += '<!DOCTYPE html>'
        $page += '<html lang=uk>'
        $page += '<head>'
        $page += '<meta charset=utf-8>'
        $page += '<meta name=viewport content=width=device-width,initial-scale=1>'
        $page += '<title>Як стати мільйонером &mdash; APK</title>'
        $page += '<style>'
        $page += '  body { font-family: system-ui, sans-serif; background: #07145c; color: #fff; margin: 0; padding: 24px; }'
        $page += '  h1 { font-size: 22px; margin: 0 0 4px; }'
        $page += '  p.hint { color: #b9c4ff; font-size: 14px; margin-top: 0; }'
        $page += '  a.card { display: block; background: #0d1f7a; border: 1px solid #2f4bd0; border-radius: 14px; padding: 16px 18px; margin: 14px 0; text-decoration: none; color: #ffd76a; font-size: 17px; }'
        $page += '</style>'
        $page += '</head>'
        $page += '<body>'
        $page += '<h1>Як стати мільйонером</h1>'
        $page += '<p class=hint>Оберіть збірку для встановлення. Після завантаження відкрийте файл і дозвольте встановлення з невідомого джерела.</p>'
        $page += ('<p class=hint>Остання збірка: ' + $versionName + ' (код ' + $versionCode + '), ' + (Get-Date -Format 'yyyy-MM-dd HH:mm') + '.</p>')
        $page += $cards
        $page += '</body>'
        $page += '</html>'
        [System.IO.File]::WriteAllText((Join-Path $distDir 'index.html'), ($page -join [Environment]::NewLine), $utf8NoBom)
        Write-Host ('Updated ' + (Join-Path $distDir 'index.html')) -ForegroundColor Green
    }
}

exit $gradleExit