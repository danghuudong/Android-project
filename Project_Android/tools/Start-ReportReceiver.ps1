param(
    [int]$Port = 8765,
    [string]$OutputDirectory = (Join-Path $PSScriptRoot '..\reports\dashboard')
)

$targetDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
[System.IO.Directory]::CreateDirectory($targetDirectory) | Out-Null

$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Any, $Port)
$listener.Start()

Write-Host "Dang nhan bao cao Excel tai cong $Port."
Write-Host "Thu muc luu file: $targetDirectory"
Write-Host "Nhan Ctrl+C de dung."

try {
    while ($true) {
        $client = $listener.AcceptTcpClient()
        try {
            $stream = $client.GetStream()
            $headerBytesList = [System.Collections.Generic.List[byte]]::new()
            while ($true) {
                $value = $stream.ReadByte()
                if ($value -lt 0) { throw 'Ket noi bi ngat truoc khi nhan header.' }
                $headerBytesList.Add([byte]$value)
                $count = $headerBytesList.Count
                if ($count -ge 4 `
                    -and $headerBytesList[$count - 4] -eq 13 `
                    -and $headerBytesList[$count - 3] -eq 10 `
                    -and $headerBytesList[$count - 2] -eq 13 `
                    -and $headerBytesList[$count - 1] -eq 10) {
                    break
                }
                if ($count -gt 16384) { throw 'Header request qua lon.' }
            }

            $headerText = [System.Text.Encoding]::ASCII.GetString($headerBytesList.ToArray())
            $headerLines = $headerText -split "`r`n"
            $requestLine = $headerLines[0]
            $headers = @{}
            for ($index = 1; $index -lt $headerLines.Length; $index++) {
                $line = $headerLines[$index]
                if ([string]::IsNullOrEmpty($line)) { continue }
                $separator = $line.IndexOf(':')
                if ($separator -gt 0) {
                    $headers[$line.Substring(0, $separator).Trim().ToLowerInvariant()] =
                        $line.Substring($separator + 1).Trim()
                }
            }

            if ($requestLine -notlike 'POST /reports/dashboard*') {
                $responseBody = [System.Text.Encoding]::UTF8.GetBytes('Endpoint khong hop le')
                $response = "HTTP/1.1 404 Not Found`r`nContent-Length: $($responseBody.Length)`r`nConnection: close`r`n`r`n"
                $headerBytes = [System.Text.Encoding]::ASCII.GetBytes($response)
                $stream.Write($headerBytes, 0, $headerBytes.Length)
                $stream.Write($responseBody, 0, $responseBody.Length)
                continue
            }

            $length = 0
            if (-not [int]::TryParse($headers['content-length'], [ref]$length) -or $length -le 0) {
                throw 'Request khong co du lieu file.'
            }

            $requestedName = $headers['x-file-name']
            $safeName = [System.IO.Path]::GetFileName($requestedName)
            if ([string]::IsNullOrWhiteSpace($safeName) -or -not $safeName.EndsWith('.xlsx')) {
                $safeName = "Bao_Cao_Tong_Quan_$([DateTime]::Now.ToString('yyyyMMdd_HHmmss')).xlsx"
            }

            $targetFile = Join-Path $targetDirectory $safeName
            $fileStream = [System.IO.File]::Create($targetFile)
            try {
                $buffer = New-Object byte[] 8192
                $remaining = $length
                while ($remaining -gt 0) {
                    $read = $stream.Read($buffer, 0, [Math]::Min($buffer.Length, $remaining))
                    if ($read -le 0) { throw 'Ket noi bi ngat khi dang nhan file.' }
                    $fileStream.Write($buffer, 0, $read)
                    $remaining -= $read
                }
            } finally {
                $fileStream.Dispose()
            }

            $responseText = "Da luu: $targetFile"
            $responseBody = [System.Text.Encoding]::UTF8.GetBytes($responseText)
            $response = "HTTP/1.1 200 OK`r`nContent-Type: text/plain; charset=utf-8`r`nContent-Length: $($responseBody.Length)`r`nConnection: close`r`n`r`n"
            $headerBytes = [System.Text.Encoding]::ASCII.GetBytes($response)
            $stream.Write($headerBytes, 0, $headerBytes.Length)
            $stream.Write($responseBody, 0, $responseBody.Length)
            Write-Host $responseText
        } catch {
            Write-Host "Loi nhan file: $($_.Exception.Message)"
        } finally {
            $client.Dispose()
        }
    }
} finally {
    $listener.Stop()
}
