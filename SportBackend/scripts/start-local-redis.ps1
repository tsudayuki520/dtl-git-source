param(
    [string]$RedisDirectory = 'E:\JavaWeb\cqwmDevelop\redis',
    [int]$Port = 6379
)
$ErrorActionPreference = 'Stop'
$cli = Join-Path $RedisDirectory 'redis-cli.exe'
$server = Join-Path $RedisDirectory 'redis-server.exe'
if (!(Test-Path -LiteralPath $server)) { throw "Redis executable not found: $server" }
$ping = & $cli -h 127.0.0.1 -p $Port ping 2>$null
if ($ping -eq 'PONG') { Write-Output "Redis already running on 127.0.0.1:$Port"; exit 0 }
# Dedicated runtime directory; never load/overwrite the user's existing dump.rdb.
$runtime = Join-Path $PSScriptRoot "../target/redis-local-$Port"
New-Item -ItemType Directory -Path $runtime -Force | Out-Null
$runtime = (Resolve-Path -LiteralPath $runtime).Path.Replace('\', '/')
$config = Join-Path $runtime 'redis.conf'
@"
bind 127.0.0.1
port $Port
save ""
appendonly no
dir "$runtime"
logfile "$runtime/redis.log"
"@ | Set-Content -LiteralPath $config -Encoding ascii
$proc = Start-Process -FilePath $server -ArgumentList @('"' + $config + '"') -WindowStyle Hidden -PassThru
for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Milliseconds 250
    $ping = & $cli -h 127.0.0.1 -p $Port ping 2>$null
    if ($ping -eq 'PONG') {
        Write-Output "Redis started on 127.0.0.1:$Port (PID $($proc.Id)); runtime: $runtime"
        exit 0
    }
    if ($proc.HasExited) { break }
}
throw "Redis did not start. Inspect $runtime/redis.log"
