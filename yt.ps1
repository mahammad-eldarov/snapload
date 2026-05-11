param(
    [Parameter(Mandatory=$true)]
    [string]$url
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

$currentPath = [Environment]::GetEnvironmentVariable("PATH", "User")
if ($currentPath -notlike "*$scriptDir*") {
    Write-Host "Adding to PATH..."
    [Environment]::SetEnvironmentVariable("PATH", $currentPath + ";$scriptDir", "User")
    Write-Host "Done. Reopen terminal, then use: yt 'url'"
    exit
}

try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method GET | Out-Null
} catch {
    Write-Host "Starting Spring Boot..."
    Start-Process -FilePath "$scriptDir\gradlew.bat" -ArgumentList "bootRun" -WindowStyle Hidden
    Write-Host "Please wait..."
    Start-Sleep -Seconds 15
}

$encodedUrl = [Uri]::EscapeDataString($url)
$uri = "http://localhost:8080/api/stream?url=$encodedUrl"

$request = [System.Net.WebRequest]::Create($uri)
$request.Method = "GET"

$response = $request.GetResponse()
$stream = $response.GetResponseStream()
$reader = New-Object System.IO.StreamReader($stream)

Write-Host ""

while (-not $reader.EndOfStream) {
    $line = $reader.ReadLine()

    if ($line -match "\[download\]\s+([\d,.]+)%") {
        $percent = [int]([double]($matches[1] -replace ',', '.'))

        $barLength = [int]($percent / 10)
        $bar = "#" * $barLength
        $empty = "-" * (10 - $barLength)

        Write-Host -NoNewline "`r Downloading [$bar$empty] $percent%"
    }

    elseif ($line -match "\[ExtractAudio\]|\[ffmpeg\]|\[Metadata\]|\[EmbedThumbnail\]") {
        Write-Host -NoNewline "`r Converting to MP3...                      "
    }

    elseif ($line -match "STATUS:DONE") {
        Write-Host "`r Converting finished..."
    }
}

Write-Host ""
Write-Host "Done!"

try {
    Start-Job -ScriptBlock {
        try {
            Invoke-RestMethod -Uri "http://localhost:8080/api/cleanup" -Method POST -ErrorAction SilentlyContinue
        } catch {}
    } | Out-Null
} catch {

}