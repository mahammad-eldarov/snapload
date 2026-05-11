param(
    [Parameter(Mandatory=$true)]
    [string]$url,
    [string]$format = "mp3",
    [int]$start = -1,
    [int]$end   = -1
)

# ── PATH check ──────────────────────────────────────────────────
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$currentPath = [Environment]::GetEnvironmentVariable("PATH", "User")
if ($currentPath -notlike "*$scriptDir*") {
    Write-Host "Adding to PATH..."
    [Environment]::SetEnvironmentVariable("PATH", $currentPath + ";$scriptDir", "User")
    Write-Host "Done. Reopen terminal, then use: yt 'url' [mp3|mp4] [start] [end]"
    exit
}

# ── Format validation ───────────────────────────────────────────
if ($format -notin @("mp3", "mp4")) {
    Write-Host "Format must be mp3 or mp4"
    exit 1
}

if (($start -ge 0 -or $end -ge 0) -and $format -ne "mp4") {
    Write-Host "Trim is only supported for mp4"
    exit 1
}

if ($start -ge 0 -and $end -le $start) {
    Write-Host "End second must be greater than start"
    exit 1
}

# ── Server check ────────────────────────────────────────────────
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method GET | Out-Null
} catch {
    Write-Host "Starting Spring Boot..."
    Start-Process -FilePath "$scriptDir\gradlew.bat" -ArgumentList "bootRun" -WindowStyle Hidden
    Write-Host "Please wait..."
    Start-Sleep -Seconds 15
}

# ── Request body ────────────────────────────────────────────────
$body = @{ url = $url; format = $format }
if ($start -ge 0 -and $end -gt $start) {
    $body.startSec = $start
    $body.endSec   = $end
}

$bodyJson  = $body | ConvertTo-Json
$bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($bodyJson)

# ── Streaming POST ──────────────────────────────────────────────
$request               = [System.Net.WebRequest]::Create("http://localhost:8080/api/stream")
$request.Method        = "POST"
$request.ContentType   = "application/json"
$request.ContentLength = $bodyBytes.Length

$reqStream = $request.GetRequestStream()
$reqStream.Write($bodyBytes, 0, $bodyBytes.Length)
$reqStream.Close()

$response = $request.GetResponse()
$stream   = $response.GetResponseStream()
$reader   = New-Object System.IO.StreamReader($stream)

Write-Host ""
$isMp4 = $format -eq "mp4"
$phase = 1

while (-not $reader.EndOfStream) {
    $line = $reader.ReadLine()

    if ($line -match "\[download\]\s+([\d,.]+)%") {
        $percent = [int]([double]($matches[1] -replace ',', '.'))
        $bar     = "#" * [int]($percent / 10)
        $empty   = "-" * (10 - [int]($percent / 10))

        if ($isMp4) {
            $label = if ($phase -eq 1) { "Video" } else { "Audio" }
            Write-Host -NoNewline "`r Downloading $label [$bar$empty] $percent%   "
            if ($percent -eq 100 -and $phase -eq 1) { $phase = 2 }
        } else {
            Write-Host -NoNewline "`r Downloading [$bar$empty] $percent%"
        }
    }

    elseif ($line -match "\[ExtractAudio\]|\[ffmpeg\]|\[Metadata\]|\[EmbedThumbnail\]") {
        Write-Host -NoNewline "`r Converting to MP3...                      "
    }

    elseif ($line -match "\[Merger\]|\[VideoConvertor\]") {
        Write-Host -NoNewline "`r Merging video + audio...                  "
    }

    elseif ($line -match "STATUS:DONE") {
        $ext = $format.ToUpper()
        Write-Host "`r $ext ready!                                "
    }
}

Write-Host ""
Write-Host "Done!"

# ── Cleanup ─────────────────────────────────────────────────────
try {
    Start-Job -ScriptBlock {
        try {
            Invoke-RestMethod -Uri "http://localhost:8080/api/cleanup" -Method POST -ErrorAction SilentlyContinue
        } catch {}
    } | Out-Null
} catch {}