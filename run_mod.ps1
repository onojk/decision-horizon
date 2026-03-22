# =========================
# Decision Horizon Build + Run Script
# =========================

$projectPath = "C:\Users\whyno\Documents\projects\decision-horizon"
$gamePath = "C:\Program Files (x86)\Steam\steamapps\common\SlayTheSpire"
$jarPath = "$projectPath\build\libs\decision-horizon-1.0.jar"
$modsPath = "$gamePath\mods"

$gradlePath = "C:\gradle\gradle-9.4.0\bin\gradle.bat"
$java25Home = "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"

Write-Host "=== Building mod ==="
Set-Location $projectPath

# Force Gradle to run on Java 25
$env:JAVA_HOME = $java25Home
$env:Path = "$java25Home\bin;C:\gradle\gradle-9.4.0\bin;$env:Path"

& $gradlePath clean build

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed"
    exit 1
}

if (!(Test-Path $jarPath)) {
    Write-Host "Build failed: JAR not found"
    exit 1
}

Write-Host "=== Copying JAR to game ==="
Copy-Item $jarPath $modsPath -Force

Write-Host "=== Launching Slay the Spire ==="
Set-Location $gamePath
.\jre\bin\java.exe -jar mts-launcher.jar