# Maven wrapper script using Docker
param(
    [Parameter(ValueFromRemainingArguments=$true)]
    [string[]]$MavenArgs
)

$workDir = Get-Location
docker run --rm -v "${workDir}:/app" -w /app/backend maven:3.9.6-openjdk-17 mvn @MavenArgs