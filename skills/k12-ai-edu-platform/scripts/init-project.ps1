<#
.SYNOPSIS
    生成 K12 AI 启蒙教育平台的可运行骨架（SpringBoot 后端 + Vue 3 前端 + 建表脚本）。

.DESCRIPTION
    从 templates/ 复制经过验证的骨架代码，替换包名/类名/构建坐标占位符，
    并写入 .gitignore。生成后按提示建库、起服务即可跑通登录流程。

.PARAMETER TargetDir
    生成目标目录（不存在会自动创建；非空时需加 -Force）。

.PARAMETER Package
    Java 包名，全小写点分格式。默认 com.example.eduplatform。
    其前两段会作为 Maven groupId。

.PARAMETER AppName
    SpringBoot 启动类名。默认 EduPlatformApplication。

.PARAMETER Artifact
    Maven artifactId 与 spring.application.name。默认 edu-platform。

.PARAMETER Force
    目标目录非空时仍然生成（会覆盖同名文件）。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File init-project.ps1 `
        -TargetDir D:\my-platform -Package com.example.edu -AppName EduApplication
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$TargetDir,
    [string]$Package  = 'com.example.eduplatform',
    [string]$AppName  = 'EduPlatformApplication',
    [string]$Artifact = 'edu-platform',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

function Write-Step($msg) { Write-Host "==> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)   { Write-Host "    $msg" -ForegroundColor Green }
function Write-Warn2($msg){ Write-Host "    $msg" -ForegroundColor Yellow }

# ---------- 0. 参数校验（早失败，别生成到一半才报错） ----------
if ($Package -notmatch '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$') {
    throw "包名不合法：$Package（应形如 com.example.eduplatform，全小写，至少两段）"
}
if ($AppName -notmatch '^[A-Z][A-Za-z0-9]*$') {
    throw "启动类名不合法：$AppName（大驼峰，如 EduPlatformApplication）"
}
if ($Artifact -notmatch '^[a-z][a-z0-9\-]*$') {
    throw "artifactId 不合法：$Artifact（小写字母数字与连字符）"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$tplDir    = Join-Path $scriptDir 'templates'
if (-not (Test-Path $tplDir)) { throw "找不到模板目录：$tplDir" }

$TargetDir = [System.IO.Path]::GetFullPath($TargetDir)

# ---------- 1. 目标目录 ----------
if (Test-Path $TargetDir) {
    $existing = @(Get-ChildItem $TargetDir -Force)
    if ($existing.Count -gt 0 -and -not $Force) {
        throw "目标目录非空：$TargetDir`n如确认要覆盖请加 -Force"
    }
    Write-Step "目标目录已存在：$TargetDir"
} else {
    New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null
    Write-Step "已创建目标目录：$TargetDir"
}

# ---------- 2. 复制模板 ----------
Write-Step "复制模板"
Get-ChildItem $tplDir -Force | ForEach-Object {
    Copy-Item $_.FullName -Destination $TargetDir -Recurse -Force
}
Write-Ok "后端 / 前端 / sql 模板已复制"

# ---------- 3. 重命名包目录与启动类 ----------
$pkgRel    = $Package.Replace('.', '\')
$oldPkgDir = Join-Path $TargetDir 'backend\src\main\java\__PKG__'
$newPkgDir = Join-Path $TargetDir "backend\src\main\java\$pkgRel"

if (Test-Path $oldPkgDir) {
    New-Item -ItemType Directory -Force -Path (Split-Path $newPkgDir -Parent) | Out-Null
    Move-Item $oldPkgDir $newPkgDir -Force
    Write-Ok "包目录 → $pkgRel"
} else {
    Write-Warn2 "未找到 __PKG__ 目录，跳过重命名"
}

$oldApp = Join-Path $newPkgDir '__APP__.java'
if (Test-Path $oldApp) {
    Rename-Item $oldApp "$AppName.java" -Force
    Write-Ok "启动类 → $AppName.java"
}

# ---------- 4. 占位符替换 ----------
$segs    = $Package.Split('.')
$groupId = if ($segs.Count -ge 2) { "$($segs[0]).$($segs[1])" } else { $Package }
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

Write-Step "替换占位符（包名 / 类名 / 构建坐标）"
$changed = 0
foreach ($f in (Get-ChildItem $TargetDir -Recurse -File -Force)) {
    # 跳过二进制与依赖目录
    if ($f.FullName -match '\\(node_modules|target|dist|\.git)\\' ) { continue }
    if ($f.Length -gt 2MB) { continue }

    $text = [System.IO.File]::ReadAllText($f.FullName)
    if ($text -notmatch '__PKG__|__APP__|__ARTIFACT__|__GROUPID__') { continue }

    $text = $text.Replace('__PKG__', $Package)
    $text = $text.Replace('__APP__', $AppName)
    $text = $text.Replace('__ARTIFACT__', $Artifact)
    $text = $text.Replace('__GROUPID__', $groupId)
    [System.IO.File]::WriteAllText($f.FullName, $text, $utf8NoBom)
    $changed++
}
Write-Ok "$changed 个文件已替换"

# ---------- 5. 写 .gitignore ----------
$gitignore = @'
# 依赖与构建产物
node_modules/
dist/
target/
*.class
*.jar
*.log
.vite/

# 本地敏感配置（务必不要提交）
backend/src/main/resources/application-local.yml
backend/src/main/resources/application-prod.yml
.env
.env.*
.baoyu-skills/
*.pem
*.key

# 学校材料与个人隐私
*.docx
*.doc
*.pdf

# 版权受限素材
frontend/public/videos/

# 备份与一次性脚本
*.bak
backend/sql/fix*.sql

# 编辑器与系统
.idea/
.vscode/
*.iml
.DS_Store
Thumbs.db
'@
[System.IO.File]::WriteAllText((Join-Path $TargetDir '.gitignore'), $gitignore, $utf8NoBom)
Write-Ok "已写入 .gitignore"

# ---------- 6. 完成提示 ----------
$db = 'ai_enlighten'
Write-Host ""
Write-Host "生成完成。" -ForegroundColor Green
Write-Host @"

接下来四步：

  1) 建库
     mysql -u root -p < "$TargetDir\backend\sql\init.sql"
     mysql -u root -p $db < "$TargetDir\backend\sql\demo-data.sql"

  2) 配置后端（复制模板并填自己的数据库账号）
     copy "$TargetDir\backend\src\main\resources\application-local.yml.example" ``
          "$TargetDir\backend\src\main\resources\application-local.yml"

  3) 起服务（各自用后台任务，勿前台阻塞）
     cd "$TargetDir\backend"  ; mvn spring-boot:run            # → 8080
     cd "$TargetDir\frontend" ; npm install ; npm run dev       # → 5173

  4) 自检
     powershell -File "$scriptDir\verify.ps1" -ProjectDir "$TargetDir"

演示账号（密码均为 123456）：admin / teacher01 / student01
包名：$Package    启动类：$AppName    artifactId：$Artifact
"@ -ForegroundColor Gray
