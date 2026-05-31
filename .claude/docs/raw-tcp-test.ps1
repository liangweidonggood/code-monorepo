# 异常场景测试 — 原始 TCP 发包
param($TargetHost = "localhost", $TargetPort = 8888)

function Bcc([byte[]]$data) {
    $r = 0
    foreach ($b in $data) { $r = $r -bxor $b }
    return [byte]$r
}

function Send-Frame([byte]$msgType, [byte[]]$body, [string]$desc) {
    Write-Host "`n>>> $desc" -ForegroundColor Cyan
    $bl = [byte]$body.Count
    $frameData = @([byte]0xFE, $bl, $msgType, [byte]0x00) + $body
    $bccVal = Bcc $frameData[1..($frameData.Count-1)]
    $frameData += @($bccVal)
    $hex = -join ($frameData | ForEach-Object { "{0:X2}" -f $_ })
    Write-Host "    发送: $hex"
    try {
        $c = New-Object System.Net.Sockets.TcpClient($TargetHost, $TargetPort)
        $s = $c.GetStream()
        $s.Write($frameData, 0, $frameData.Count)
        $s.Flush()
        Start-Sleep -Milliseconds 300
        if ($s.DataAvailable) {
            $buf = New-Object byte[] 1024
            $n = $s.Read($buf, 0, $buf.Length)
            $resp = [byte[]]$buf[0..($n-1)]
            $rhex = -join ($resp | ForEach-Object { "{0:X2}" -f $_ })
            Write-Host "    响应: $rhex"
            $c.Close(); $c.Dispose()
            return $resp
        }
        Write-Host "    (无响应)" -ForegroundColor DarkGray
        $c.Close(); $c.Dispose()
        return $null
    } catch {
        Write-Host "    ���: $_" -ForegroundColor Yellow
        return $null
    }
}

# ===== 测试 1: 非法魔数 =====
Write-Host "`n===== 测试 1: 非法魔数 (0xAA) =====" -ForegroundColor Yellow
Send-Frame 0x03 @(
    0x12, 0x34, 0x56, 0x78, 0x90, 0x12, 0x34, 0x00, 0x01, 0x01
) "心跳帧 魔数=0xFE (正常BCC)" | Out-Null
# 手工构造 0xAA 帧 (函数强制 0xFE，这里手工发)
$badFrame = @(0xAA, 0x0A, 0x03, 0x00, 0x12,0x34,0x56,0x78,0x90,0x12,0x34, 0x00,0x01,0x01, 0xFF)
Write-Host ">>> 发送非法魔数 0xAA"
try {
    $c = New-Object System.Net.Sockets.TcpClient($TargetHost, $TargetPort)
    $s = $c.GetStream()
    $s.Write($badFrame, 0, $badFrame.Count)
    $s.Flush()
    Start-Sleep -Milliseconds 300
    try { $s.Read((New-Object byte[] 1), 0, 1) | Out-Null } catch { }
    Write-Host "    连接应已被服务端掐断" -ForegroundColor Green
    $c.Close(); $c.Dispose()
} catch {
    Write-Host "    ���: $_"
}

# ===== 测试 2: BCC 校验失败 =====
Write-Host "`n===== 测试 2: BCC 校验失败 =====" -ForegroundColor Yellow
$regBody = @(
    0x12, 0x34, 0x56, 0x78, 0x90, 0x12, 0x34
    0x00, 0x01
    0x00, 0x01
    0x10, 0x20
    0x00, 0x0B
    0x00, 0x01
    0x06
    0x41, 0x31, 0x32, 0x33, 0x34, 0x35
    0x02
)
$bl = [byte]$regBody.Count
# 故意算错 BCC
$correct = Bcc (@([byte]$bl, [byte]0x01, [byte]0x00) + $regBody)
$wrong = $correct -bxor 0x42
$badBccFrame = @(0xFE, $bl, 0x01, 0x00) + $regBody + @($wrong)
$hex2 = -join ($badBccFrame | ForEach-Object { "{0:X2}" -f $_ })
Write-Host ">>> 发送 BCC 错误帧 (正确:0x$('{0:X2}' -f $correct), 发送:0x$('{0:X2}' -f $wrong))"
Write-Host "    帧: $hex2"
try {
    $c2 = New-Object System.Net.Sockets.TcpClient($TargetHost, $TargetPort)
    $s2 = $c2.GetStream()
    $s2.Write($badBccFrame, 0, $badBccFrame.Count)
    $s2.Flush()
    Start-Sleep -Milliseconds 300
    if ($s2.DataAvailable) {
        $buf = New-Object byte[] 1024
        $n = $s2.Read($buf, 0, $buf.Length)
        Write-Host "    意外响应: $([BitConverter]::ToString($buf[0..($n-1)]))"
    } else {
        Write-Host "    (无响应，帧应被丢弃)" -ForegroundColor Green
    }
    $c2.Close(); $c2.Dispose()
} catch { Write-Host "    ���: $_" }

# ===== 测试 3: 未知消息类型 =====
Write-Host "`n===== 测试 3: 未知消息类型 (0xFF vs 0xEE) =====" -ForegroundColor Yellow
Send-Frame 0xFF @(0xDE, 0xAD, 0xBE, 0xEF) "msgType=0xFF (未注册)" | Out-Null
Send-Frame 0xEE @(0xCA, 0xFE) "msgType=0xEE (未注册)" | Out-Null

# ===== 测试 4: 鉴权码错误 =====
Write-Host "`n===== 测试 4: 鉴权码错误 =====" -ForegroundColor Yellow
$regBody2 = @(
    0x12, 0x34, 0x56, 0x78, 0x90, 0x12, 0x34
    0x00, 0x01
    0x00, 0x01
    0x10, 0x20
    0x00, 0x0B
    0x00, 0x01
    0x06
    0x41, 0x31, 0x32, 0x33, 0x34, 0x35
    0x02
)
Write-Host ">>> 步骤 1: 正常注册"
try {
    $c4 = New-Object System.Net.Sockets.TcpClient($TargetHost, $TargetPort)
    $s4 = $c4.GetStream()
    $bl2 = [byte]$regBody2.Count
    $bccReg = Bcc (@([byte]$bl2, [byte]0x01, [byte]0x00) + $regBody2)
    $fReg = @(0xFE, $bl2, 0x01, 0x00) + $regBody2 + @($bccReg)
    $s4.Write($fReg, 0, $fReg.Count)
    $s4.Flush()
    Start-Sleep -Milliseconds 500
    $buf4 = New-Object byte[] 1024
    $n4 = $s4.Read($buf4, 0, $buf4.Length)
    $resp = [byte[]]$buf4[0..($n4-1)]
    # 解析 authCode (位置 6-9)
    $ac = -join ($resp[6..9] | ForEach-Object { "{0:X2}" -f $_ })
    Write-Host "    注册成功，authCode=$ac"
    # 步骤 2: 错误鉴权码
    Write-Host ">>> 步骤 2: 发送错误鉴权码 00000000"
    $badAuthBody = @(
        0x12, 0x34, 0x56, 0x78, 0x90, 0x12, 0x34
        0x00, 0x00, 0x00, 0x00
        0x00, 0x02
    )
    $blA = [byte]$badAuthBody.Count
    $bccA = Bcc (@([byte]$blA, [byte]0x02, [byte]0x00) + $badAuthBody)
    $fAuth = @(0xFE, $blA, 0x02, 0x00) + $badAuthBody + @($bccA)
    $s4.Write($fAuth, 0, $fAuth.Count)
    $s4.Flush()
    Start-Sleep -Milliseconds 500
    $bufA = New-Object byte[] 1024
    $nA = $s4.Read($bufA, 0, $bufA.Length)
    $respA = [byte[]]$bufA[0..($nA-1)]
    $resultA = $respA[7]
    Write-Host "    鉴权结果: result=$resultA"
    if ($resultA -ne 0) {
        Write-Host "    ✅ 鉴权被拒 (result=$resultA)，符合预期" -ForegroundColor Green
    } else {
        Write-Host "    ❌ 鉴权通过，不符合预期!" -ForegroundColor Red
    }
    $c4.Close(); $c4.Dispose()
} catch { Write-Host "    ���: $_" -ForegroundColor Yellow }

Write-Host "`n===== 全部协议层测试完成 =====" -ForegroundColor Green
