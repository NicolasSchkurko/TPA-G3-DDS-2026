param(
  [string]$Config = "incentivos-service\config\checkstyle\checkstyle.xml",
  [string]$Objetivo = "incentivos-service\src"
)

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
$lib = "$env:APPDATA\JetBrains\IntelliJIdea2026.1\plugins\checkstyle-idea\checkstyle\lib"
$jars = @(Get-ChildItem $lib -Filter *.jar | ForEach-Object { $_.FullName })
$jars += "$env:USERPROFILE\.m2\repository\commons-logging\commons-logging\1.2\commons-logging-1.2.jar"
$cp = $jars -join ";"

$salida = "$env:TEMP\cs-proyecto.txt"
& "$env:JAVA_HOME\bin\java.exe" -cp $cp com.puppycrawl.tools.checkstyle.Main -c $Config $Objetivo 2>&1 | Out-File -FilePath $salida -Encoding utf8

# OJO: si Checkstyle no logra parsear la config, sale con 0 findings y SIN decir nada. Un
# "0 errores, 0 warnings" asi no vale nada. Por eso se chequea el texto crudo primero.
$crudo = Get-Content $salida -Encoding utf8
$excepcion = $crudo | Where-Object { $_ -match "CheckstyleException|unable to parse|cannot initialize" }
if ($excepcion) {
  Write-Output "LA CONFIG NO CORRE. Checkstyle fallo al procesarla:"
  $excepcion | Select-Object -First 4 | ForEach-Object { Write-Output ("  " + $_) }
  exit 1
}

$errores = @($crudo | Where-Object { $_ -match "^\[ERROR\]" })
$warnings = @($crudo | Where-Object { $_ -match "^\[WARN\]" })
$infos = @($crudo | Where-Object { $_ -match "^\[INFO\]" })

Write-Output ("errores:   " + $errores.Count)
Write-Output ("warnings:  " + $warnings.Count + "   <-- lo que el IDE muestra en el panel")
Write-Output ("info:      " + $infos.Count + "   <-- la config las baja a proposito")

if ($warnings.Count -gt 0) {
  Write-Output ""
  Write-Output "warnings por regla:"
  $warnings | ForEach-Object { if ($_ -match '\[([A-Za-z]+)\]\s*$') { $matches[1] } } |
    Group-Object | Sort-Object Count -Descending | ForEach-Object { "{0,5}  {1}" -f $_.Count, $_.Name }
}

if ($infos.Count -gt 0) {
  Write-Output ""
  Write-Output "info por regla (informativo, no molesta):"
  $infos | ForEach-Object { if ($_ -match '\[([A-Za-z]+)\]\s*$') { $matches[1] } } |
    Group-Object | Sort-Object Count -Descending | Select-Object -First 12 |
    ForEach-Object { "{0,5}  {1}" -f $_.Count, $_.Name }
}
