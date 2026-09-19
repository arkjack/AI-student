<#
.SYNOPSIS
    K12 教育平台骨架验收检查：密钥泄露 / .gitignore 覆盖 / mock 残留 / 构建 / 越权。

.DESCRIPTION
    静态检查（密钥、gitignore、mock）默认全部执行，速度很快。
    构建与接口测试需显式开启，因为它们依赖外部环境。

.PARAMETER ProjectDir
    项目根目录（含 backend/ 与 frontend/）。默认当前目录。

.PARAMETER Build
    额外执行 mvn compile 与 npm run build（较慢）。

.PARAMETER BaseUrl
    给出后端地址（如 http://127.0.0.1:8080）时，额外做跨角色越权测试。

.PARAMETER DemoPassword
    越权测试用的演示账号密码，默认 123456。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File verify.ps1 -ProjectDir D:\my-platform -Build
#>
[CmdletBinding()]
param(
    [string]$ProjectDir = '.',
    [switch]$Build,
    [string]$BaseUrl = '',
    [string]$DemoPassword = '123456'
)

$ErrorActionPreference = 'Continue'

$script:fail = 0
$script:warn = 0

function Section($t) { Write-Host "`n=== $t ===" -ForegroundColor Cyan }
function Pass($m)    { Write-Host "  [PASS] $m" -ForegroundColor Green }
function Fail($m)    { Write-Host "  [FAIL] $m" -ForegroundColor Red;   $script:fail++ }
function Warn($m)    { Write-Host "  [WARN] $m" -ForegroundColor Yellow; $script:warn++ }
function Info($m)    { Write-Host "  $m" -ForegroundColor Gray }

$root = [System.IO.Path]::GetFullPath($ProjectDir)
if (-not (Test-Path $root)) { Write-Host "目录不存在：$root" -ForegroundColor Red; exit 1 }
Write-Host "验收目录：$root" -ForegroundColor White

# 需要跳过的目录（依赖 / 构建产物 / 版本控制）
$skipRegex = '\\(node_modules|target|dist|\.git|vendor|\.tmp-)\b'

function Get-SourceFiles($subDirs, $exts) {
    foreach ($sd in $subDirs) {
        $p = Join-Path $root $sd
        if (-not (Test-Path $p)) { continue }
        Get-ChildItem $p -Recurse -File -Force -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -notmatch $skipRegex -and $exts -contains $_.Extension -and $_.Length -lt 2MB }
    }
}

# ============================================================
# 1. 明文凭据扫描
# ============================================================
Section '1. 明文凭据扫描'

$credPatternsAny = @(
    @{ name = '火山方舟 ARK Key'; re = 'ark-[0-9a-f]{8}-[0-9a-f]{4}' },
    @{ name = 'OpenAI 风格密钥';  re = 'sk-[A-Za-z0-9]{20,}' },
    @{ name = 'GitHub Token';     re = 'gh[pousr]_[A-Za-z0-9]{20,}' },
    @{ name = 'AWS Access Key';   re = 'AKIA[0-9A-Z]{16}' }
)

# 仅配置文件扫 password:/secret: 赋值。
# ⚠️ 不能在源码里扫这个模式：`password: form.password`（Vue/JS 普通对象属性）
#    会造成大量误报；误报多了这个脚本就没人信了。
$credPatternsConfig = @(
    @{ name = 'JWT 明文密钥';   re = 'secret:\s*(?!\$\{|change|your_|CHANGE)\S{6,}' },
    @{ name = '明文数据库密码'; re = 'password:\s*(?!\$\{|change|your_|CHANGE)\S{6,}' }
)
$configExts = @('.yml', '.yaml', '.properties', '.ini', '.conf')

$scanTargets = Get-SourceFiles @('backend\src', 'backend\sql', 'frontend\src', '.') `
                               @('.java', '.yml', '.yaml', '.properties', '.sql', '.js', '.vue', '.json', '.md')

$credHits = @()
foreach ($f in ($scanTargets | Sort-Object FullName -Unique)) {
    $text = [System.IO.File]::ReadAllText($f.FullName)

    $pats = @($credPatternsAny)
    if (($configExts -contains $f.Extension.ToLower()) -or ($f.Name -like '*.env*')) {
        $pats += $credPatternsConfig
    }

    foreach ($p in $pats) {
        foreach ($m in [regex]::Matches($text, $p.re)) {
            $credHits += [PSCustomObject]@{
                File = $f.FullName.Replace($root, '.')
                Line = ($text.Substring(0, $m.Index) -split "`n").Count
                Kind = $p.name
                Text = $m.Value.Trim()
            }
        }
    }
}

if ($credHits.Count -eq 0) {
    Pass "源码与配置中未发现明文凭据"
} else {
    foreach ($h in $credHits) { Fail "$($h.Kind) ← $($h.File):$($h.Line)  $($h.Text)" }
    Warn "若为误报（如文档中的示例串），请确认后忽略"
}

# ============================================================
# 2. .gitignore 覆盖
# ============================================================
Section '2. .gitignore 覆盖检查'

$giPath = Join-Path $root '.gitignore'
if (-not (Test-Path $giPath)) {
    Fail "缺少 .gitignore —— 依赖目录与本地配置会被提交"
} else {
    Pass ".gitignore 存在"
    $gi = [System.IO.File]::ReadAllText($giPath)
    $mustHave = @(
        @{ pat = 'node_modules';              desc = '前端依赖' },
        @{ pat = 'target';                    desc = '后端构建产物' },
        @{ pat = 'application-local';         desc = '本地数据库配置' },
        @{ pat = '\.env';                     desc = '环境变量密钥' },
        @{ pat = 'dist';                      desc = '前端构建产物' }
    )
    foreach ($m in $mustHave) {
        if ($gi -match $m.pat) { Pass ".gitignore 已覆盖 $($m.desc)" }
        else { Fail ".gitignore 缺少 $($m.desc)（应含 '$($m.pat)'）" }
    }
}

# ============================================================
# 3. 已跟踪文件（仅在 git 仓库内）
# ============================================================
Section '3. Git 已跟踪文件检查'

$git = Get-Command git -ErrorAction SilentlyContinue
if (-not $git) {
    Warn "未找到 git，跳过"
} elseif (-not (Test-Path (Join-Path $root '.git'))) {
    Info "不是 git 仓库，跳过"
} else {
    Push-Location $root
    $tracked = & git ls-files 2>$null
    Pop-Location
    $bad = $tracked | Where-Object {
        $_ -match 'node_modules/|^vendor/|/target/|^dist/|\.env$|\.docx$|application-local\.yml$|\.bak$|public/videos/'
    }
    if ($bad) {
        Fail "以下不该入库的文件已被跟踪："
        $bad | Select-Object -First 15 | ForEach-Object { Info "    $_" }
    } else {
        Pass "已跟踪文件中无依赖目录 / 本地配置 / 隐私材料（共 $($tracked.Count) 个文件）"
    }
}

# ============================================================
# 4. mock 数据残留
# ============================================================
Section '4. 前端 mock 残留扫描'

$frontViews = Join-Path $root 'frontend\src'
if (-not (Test-Path $frontViews)) {
    Warn "未找到 frontend\src，跳过"
} else {
    $mocks = Get-ChildItem $frontViews -Recurse -File -Include *.vue, *.js -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -notmatch $skipRegex } |
        Select-String -Pattern 'FALLBACK_|from\s+[''"]@/mock|mock/data' -ErrorAction SilentlyContinue

    if ($mocks) {
        Fail "仍有 $($mocks.Count) 处 mock / FALLBACK 残留 —— 交付前必须替换为真实接口："
        $mocks | Select-Object -First 15 | ForEach-Object {
            Info ("    " + $_.Path.Replace($root, '.') + ":" + $_.LineNumber)
        }
    } else {
        Pass "无 mock / FALLBACK 残留"
    }
}

# ============================================================
# 5. 构建（可选）
# ============================================================
if ($Build) {
    Section '5. 构建'

    $mvn = Get-Command mvn -ErrorAction SilentlyContinue
    $backend = Join-Path $root 'backend'
    if (-not $mvn) { Warn "未找到 mvn，跳过后端编译" }
    elseif (-not (Test-Path $backend)) { Warn "未找到 backend 目录" }
    else {
        Push-Location $backend
        & mvn -q -o compile 2>&1 | Out-Null
        $code = $LASTEXITCODE
        Pop-Location
        if ($code -eq 0) { Pass "mvn compile 通过" }
        elseif ($code -eq 1) { Fail "mvn compile 失败（离线模式下依赖缺失也会返回 1，可去掉 -o 重试）" }
        else { Warn "mvn compile 退出码 $code（可能是离线依赖缺失）" }
    }

    $npm = Get-Command npm -ErrorAction SilentlyContinue
    $frontend = Join-Path $root 'frontend'
    if (-not $npm) { Warn "未找到 npm，跳过前端构建" }
    elseif (-not (Test-Path (Join-Path $frontend 'node_modules'))) { Warn "frontend/node_modules 不存在，先 npm install" }
    else {
        Push-Location $frontend
        & npm run build 2>&1 | Out-Null
        $code = $LASTEXITCODE
        Pop-Location
        if ($code -eq 0) { Pass "npm run build 通过" }
        else { Fail "npm run build 失败" }
    }
}

# ============================================================
# 6. 跨角色越权测试（可选）
# ============================================================
if ($BaseUrl) {
    Section '6. 跨角色越权测试'

    $base = $BaseUrl.TrimEnd('/')

    function Invoke-Api($method, $path, $token, $body) {
        $headers = @{}
        if ($token) { $headers['Authorization'] = "Bearer $token" }
        $params = @{ Uri = "$base$path"; Method = $method; Headers = $headers; TimeoutSec = 15; UseBasicParsing = $true }
        if ($body) {
            $params['Body'] = ([System.Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json -Compress)))
            $params['ContentType'] = 'application/json; charset=utf-8'
        }
        try {
            $r = Invoke-WebRequest @params
            return @{ Status = [int]$r.StatusCode; Body = $r.Content }
        } catch {
            $resp = $_.Exception.Response
            if ($resp) {
                $sr = New-Object System.IO.StreamReader($resp.GetResponseStream())
                return @{ Status = [int]$resp.StatusCode; Body = $sr.ReadToEnd() }
            }
            return @{ Status = 0; Body = $_.Exception.Message }
        }
    }

    # 6.1 未登录应被拒
    $anon = Invoke-Api 'GET' '/api/admin/users' $null $null
    if ($anon.Status -eq 401 -or $anon.Body -match '"code":401') { Pass "未登录访问受保护接口 → 401" }
    else { Fail "未登录访问受保护接口未被拒绝（HTTP $($anon.Status)）" }

    # 6.2 学生越权访问管理端应 403
    $login = Invoke-Api 'POST' '/api/auth/login' $null @{ username = 'student01'; password = $DemoPassword }
    if ($login.Body -notmatch '"code":0') {
        Warn "学生账号登录失败，跳过越权测试（请确认演示数据已导入）"
    } else {
        Pass "学生账号登录成功"
        $token = ([regex]::Match($login.Body, '"token":"([^"]+)"')).Groups[1].Value
        $cross = Invoke-Api 'GET' '/api/admin/users' $token $null
        if ($cross.Status -eq 403 -or $cross.Body -match '"code":403') { Pass "学生 token 访问管理端接口 → 403（越权被正确拦截）" }
        else { Fail "学生 token 竟能访问管理端接口！HTTP $($cross.Status) —— @RequireRole 未生效" }

        # 6.3 学生访问自己的接口应正常
        $own = Invoke-Api 'GET' '/api/course/list' $token $null
        if ($own.Body -match '"code":0' -or $own.Status -eq 200) { Pass "学生访问本角色接口正常" }
        else { Warn "学生访问 /api/course/list 未返回 code:0（接口路径可能不同，按实际调整）" }
    }
}

# ============================================================
# 汇总
# ============================================================
Write-Host "`n===============================" -ForegroundColor White
if ($script:fail -eq 0) {
    Write-Host " 验收通过" -ForegroundColor Green -NoNewline
    Write-Host "（警告 $($script:warn) 项）" -ForegroundColor Gray
    exit 0
} else {
    Write-Host " 验收未通过：$($script:fail) 项失败" -ForegroundColor Red -NoNewline
    Write-Host "（警告 $($script:warn) 项）" -ForegroundColor Gray
    exit 1
}
