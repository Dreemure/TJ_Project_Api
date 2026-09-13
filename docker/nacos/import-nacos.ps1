param(
    [string]$Server = "http://127.0.0.1:8848",
    [string]$Username = "nacos",
    [string]$Password = "nacos",
    [string]$Namespace = "public",
    [string]$Group = "DEFAULT_GROUP"
)

$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------------------
# 本脚本针对 Nacos 3.x 编写。
#   Nacos 2.x: POST /nacos/v1/cs/configs   (tenant / group / content 作为表单参数)
#   Nacos 3.x: POST /nacos/v3/admin/cs/config (namespaceId / groupName / content)
#   Spring Cloud Alibaba 2025.1.0.0 自带 nacos-client 3.1.1，服务端必须为 Nacos 3.1.1。
# ---------------------------------------------------------------------------

$loginBody = @{ username = $Username; password = $Password }
$login = Invoke-RestMethod -Method Post -Uri "$Server/nacos/v1/auth/login" -ContentType "application/x-www-form-urlencoded" -Body $loginBody
$accessToken = $login.accessToken
if ([string]::IsNullOrWhiteSpace($accessToken)) {
    throw "Nacos login did not return an access token."
}

$tokenParam = [uri]::EscapeDataString($accessToken)
$groupParam = [uri]::EscapeDataString($Group)
$nsParam = [uri]::EscapeDataString($Namespace)
$configUrl = "$Server/nacos/v3/admin/cs/config"

# Windows PowerShell 5.1 用哈希表当 -Body 时按系统 ANSI 编码非 ASCII 字符，
# 会把配置里的中文写成乱码（例如 "网关" -> "ç½å³"）。
# 因此这里手工做百分号编码：EscapeDataString 会把非 ASCII 按 UTF-8 编码成
# %XX%XX%XX，请求体变成纯 ASCII，杜绝编码歧义。
function New-FormBody([hashtable]$Fields) {
    $pairs = foreach ($k in $Fields.Keys) {
        "{0}={1}" -f [uri]::EscapeDataString([string]$k), [uri]::EscapeDataString([string]$Fields[$k])
    }
    # 前面的逗号不能省：PowerShell 会把函数返回的 byte[] 展开成 Object[]，
    # 那样 -Body 收到的是 "97 61 49..." 这种十进制字节串，服务端直接 403。
    return ,([System.Text.Encoding]::UTF8.GetBytes(($pairs -join '&')))
}

$configFiles = Get-ChildItem -Path $PSScriptRoot -Filter "*.yaml" -File |
    Where-Object { $_.Name -notlike "application*" } |
    Sort-Object Name

$failed = @()

foreach ($file in $configFiles) {
    $dataId = $file.Name
    $content = Get-Content -Raw -Encoding utf8 $file.FullName

    $publishBody = New-FormBody @{
        dataId      = $dataId
        groupName   = $Group
        namespaceId = $Namespace
        type        = "yaml"
        content     = $content
        accessToken = $accessToken
    }

    try {
        $result = Invoke-RestMethod -Method Post -Uri $configUrl -ContentType "application/x-www-form-urlencoded; charset=utf-8" -Body $publishBody
        if ($result.code -ne 0) {
            $failed += $dataId
            Write-Warning "Failed  $dataId -> $($result.message)"
            continue
        }
    } catch {
        $failed += $dataId
        Write-Warning "Failed  $dataId -> publish error: $($_.Exception.Message)"
        continue
    }

    # 回读校验，确保 Nacos 中的内容与本地文件完全一致。
    # 这里必须显式按 UTF-8 解码响应体：PS 5.1 的 Invoke-RestMethod 会按系统 ANSI
    # 解码 JSON 响应，含中文的配置会被误判成"回读不一致"（字节数≠字符数）。
    $readUrl = "$configUrl" + "?dataId=" + [uri]::EscapeDataString($dataId) + "&groupName=$groupParam&namespaceId=$nsParam&accessToken=$tokenParam"
    try {
        $wc = New-Object System.Net.WebClient
        $wc.Encoding = [System.Text.Encoding]::UTF8
        try {
            $readBack = ($wc.DownloadString($readUrl) | ConvertFrom-Json)
        } finally {
            $wc.Dispose()
        }
    } catch {
        $failed += $dataId
        Write-Warning "Failed  $dataId -> read-back error: $($_.Exception.Message)"
        continue
    }

    $remote = $readBack.data.content
    if ($remote -ne $content) {
        $failed += $dataId
        Write-Warning "Failed  $dataId -> verify mismatch (local $($content.Length) vs remote $($remote.Length) chars)"
        continue
    }

    Write-Host "Published $dataId ($($content.Length) chars, verified)"
}

if ($failed.Count -gt 0) {
    throw "Nacos publish failed for: $($failed -join ', ')"
}

Write-Host "All $($configFiles.Count) configs published to $Server (namespace=$Namespace, group=$Group)."
