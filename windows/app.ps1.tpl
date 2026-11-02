# 옛글 키보드 PC — 한글을 15세기 중세국어 표기로 바꿔 줍니다.
# 실행: 이 파일을 오른쪽 클릭 → "PowerShell에서 실행"  (설치·관리자 권한 필요 없음)
# 사용: 아무 곳에서나 치고 Ctrl+Alt+Y → 그 줄(또는 고른 글자)이 옛 표기로 바뀜. 다시 누르면 원래대로.
#       Ctrl+Alt+W 변환 창, Ctrl+Alt+O 켜기/끄기. 오른쪽 아래 알림 영역 아이콘에서 설정.
# 이 파일은 windows/gen.py가 만듭니다. 고칠 때는 windows/app.ps1.tpl과 엔진(Java)을 고치세요.
param([switch]$SelfTest)

$ErrorActionPreference = 'Stop'

$engineSource = @'
@@ENGINE@@
'@

$helperSource = @'
using System;
using System.Runtime.InteropServices;
using System.Windows.Forms;

namespace Yetgeul
{
    public delegate void HotkeyHandler(int id);

    public class HotkeyWindow : NativeWindow, IDisposable
    {
        [DllImport("user32.dll")] static extern bool RegisterHotKey(IntPtr hWnd, int id, uint mod, uint vk);
        [DllImport("user32.dll")] static extern bool UnregisterHotKey(IntPtr hWnd, int id);
        public event HotkeyHandler Pressed;
        public HotkeyWindow() { CreateHandle(new CreateParams()); }
        public bool Register(int id, uint mod, Keys key) { return RegisterHotKey(Handle, id, mod, (uint)key); }
        protected override void WndProc(ref Message m)
        {
            if (m.Msg == 0x0312 && Pressed != null) Pressed(m.WParam.ToInt32());
            base.WndProc(ref m);
        }
        public void Dispose() { for (int i = 1; i <= 3; i++) UnregisterHotKey(Handle, i); DestroyHandle(); }
    }

    public static class Native
    {
        [DllImport("user32.dll")] public static extern short GetAsyncKeyState(int vk);
        [DllImport("kernel32.dll")] public static extern IntPtr GetConsoleWindow();
        [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
        public static bool ModifiersDown()
        {
            int[] vks = { 0x10, 0x11, 0x12, 0x5B, 0x5C }; // Shift Ctrl Alt Win
            foreach (int v in vks) if ((GetAsyncKeyState(v) & 0x8000) != 0) return true;
            return false;
        }
    }
}
'@

Add-Type -TypeDefinition $engineSource -Language CSharp
Add-Type -AssemblyName System.Windows.Forms, System.Drawing
Add-Type -TypeDefinition $helperSource -Language CSharp -ReferencedAssemblies System.Windows.Forms

if ($SelfTest) {
    $c = New-Object Yetgeul.Converter
    $samples = @('나라의 말씀이 중국과 달라 문자와 서로 통하지 아니하므로', '사랑합니다', '안녕하세요', '하늘과 땅이 처음 생길 때')
    foreach ($s in $samples) { "$s`n  -> $($c.Convert($s))" }
    return
}

# 한 번만 실행되게
$mutex = New-Object System.Threading.Mutex($false, 'Local\YetgeulKeyboardPC')
if (-not $mutex.WaitOne(0)) {
    [System.Windows.Forms.MessageBox]::Show('옛글 키보드가 이미 실행 중입니다. 오른쪽 아래 알림 영역을 확인하세요.', '옛글 키보드') | Out-Null
    return
}

# 콘솔 창 숨기기
[Yetgeul.Native]::ShowWindow([Yetgeul.Native]::GetConsoleWindow(), 0) | Out-Null

# ───────────── 설정 (%APPDATA%\Yetgeul\settings.json)
$settingsDir = Join-Path $env:APPDATA 'Yetgeul'
$settingsFile = Join-Path $settingsDir 'settings.json'
$ruleNames = [ordered]@{
    Araea = '아래아 ㆍ  (하늘 → ᄒᆞᄂᆞᆯ)'
    Pansios = '반치음 ㅿ  (마음 → ᄆᆞᅀᆞᆷ)'
    Yesieung = '옛이응 ㆁ  (백성 → ᄇᆡᆨ셔ᇰ)'
    Beop = '순경음 비읍 ㅸ  (더워 → 더ᄫᅥ)'
    Hapyong = '합용병서  (땅이 → ᄯᅡ히)'
    Yeoneol = '이어적기  (사람이 → 사ᄅᆞ미)'
    PalJong = '8종성  (꽃 → 곳)'
    Yeorin = '여린히읗 ㆆ  (할 바 → ᄒᆞᇙ 바)'
    Grammar = '옛 문법·말투  (합니다 → ᄒᆞᄂᆞ이다)'
}
$state = @{ Enabled = $true }
foreach ($k in $ruleNames.Keys) { $state[$k] = $true }
if (Test-Path $settingsFile) {
    try {
        $saved = Get-Content $settingsFile -Raw -Encoding UTF8 | ConvertFrom-Json
        foreach ($p in $saved.PSObject.Properties) { $state[$p.Name] = [bool]$p.Value }
    } catch { }
}
function Save-Settings {
    New-Item -ItemType Directory -Force $settingsDir | Out-Null
    ($state | ConvertTo-Json) | Set-Content $settingsFile -Encoding UTF8
}
function New-Converter {
    $o = New-Object Yetgeul.Options
    foreach ($k in $ruleNames.Keys) { $o.$k = $state[$k] }
    New-Object Yetgeul.Converter -ArgumentList $o
}
$script:conv = New-Converter

# ───────────── 단축키로 바꾸기
$script:lastOut = $null
$script:lastIn = $null

function Get-ClipText {
    try { if ([System.Windows.Forms.Clipboard]::ContainsText()) { return [System.Windows.Forms.Clipboard]::GetText() } } catch { }
    return $null
}

function Convert-InPlace {
    if (-not $state.Enabled) { return }
    # Ctrl·Alt를 뗄 때까지 기다림 (누른 채로 복사하면 엉뚱한 키가 됨)
    $sw = [Diagnostics.Stopwatch]::StartNew()
    while ([Yetgeul.Native]::ModifiersDown() -and $sw.ElapsedMilliseconds -lt 1500) { Start-Sleep -Milliseconds 20 }
    $saved = Get-ClipText
    try {
        [System.Windows.Forms.Clipboard]::Clear()
        [System.Windows.Forms.SendKeys]::SendWait('^c')
        Start-Sleep -Milliseconds 120
        $text = Get-ClipText
        if ([string]::IsNullOrEmpty($text)) {
            # 고른 글자가 없으면 커서가 있는 줄 전체
            [System.Windows.Forms.SendKeys]::SendWait('{END}+{HOME}')
            Start-Sleep -Milliseconds 60
            [System.Windows.Forms.SendKeys]::SendWait('^c')
            Start-Sleep -Milliseconds 120
            $text = Get-ClipText
        }
        if ([string]::IsNullOrEmpty($text)) { return }
        if ($text -eq $script:lastOut -and $script:lastIn) { $out = $script:lastIn }   # 다시 누르면 원래대로
        else { $out = $script:conv.Convert($text); $script:lastIn = $text; $script:lastOut = $out }
        [System.Windows.Forms.Clipboard]::SetText($out)
        [System.Windows.Forms.SendKeys]::SendWait('^v')
        Start-Sleep -Milliseconds 150
    } finally {
        try {
            if ($saved) { [System.Windows.Forms.Clipboard]::SetText($saved) } else { [System.Windows.Forms.Clipboard]::Clear() }
        } catch { }
    }
}

# ───────────── 변환 창
$script:form = $null
function Show-ConvertWindow {
    if ($script:form -and -not $script:form.IsDisposed) { $script:form.Activate(); return }
    $f = New-Object System.Windows.Forms.Form
    $f.Text = '옛글 변환 창'
    $f.TopMost = $true
    $f.Size = New-Object System.Drawing.Size(520, 360)
    $f.StartPosition = 'CenterScreen'
    $f.Font = New-Object System.Drawing.Font('Malgun Gothic', 10)
    $inBox = New-Object System.Windows.Forms.TextBox
    $inBox.Multiline = $true; $inBox.ScrollBars = 'Vertical'
    $inBox.Dock = 'Top'; $inBox.Height = 110
    $inBox.Font = New-Object System.Drawing.Font('Malgun Gothic', 12)
    $outBox = New-Object System.Windows.Forms.TextBox
    $outBox.Multiline = $true; $outBox.ReadOnly = $true; $outBox.ScrollBars = 'Vertical'
    $outBox.Dock = 'Fill'
    $outBox.Font = New-Object System.Drawing.Font('Malgun Gothic', 16)
    $bar = New-Object System.Windows.Forms.FlowLayoutPanel
    $bar.Dock = 'Bottom'; $bar.Height = 44; $bar.FlowDirection = 'RightToLeft'; $bar.Padding = '6,6,6,6'
    $copy = New-Object System.Windows.Forms.Button
    $copy.Text = '복사'; $copy.Width = 90; $copy.Height = 30
    $clear = New-Object System.Windows.Forms.Button
    $clear.Text = '지우기'; $clear.Width = 90; $clear.Height = 30
    $bar.Controls.AddRange(@($copy, $clear))
    $inBox.Add_TextChanged({ $outBox.Text = $script:conv.Convert($inBox.Text) })
    $copy.Add_Click({ if ($outBox.Text) { [System.Windows.Forms.Clipboard]::SetText($outBox.Text); $copy.Text = '복사됨' } })
    $clear.Add_Click({ $inBox.Clear(); $copy.Text = '복사'; $inBox.Focus() })
    $f.Controls.Add($outBox); $f.Controls.Add($inBox); $f.Controls.Add($bar)
    $inBox.Text = '나라의 말씀이 중국과 달라'
    $f.Add_Shown({ $inBox.SelectAll(); $inBox.Focus() })
    $script:form = $f
    $f.Show()
}

# ───────────── 알림 영역 아이콘
function New-TrayIcon([bool]$on) {
    $bmp = New-Object System.Drawing.Bitmap(32, 32)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.TextRenderingHint = 'AntiAliasGridFit'
    $bg = if ($on) { [System.Drawing.Color]::FromArgb(38, 68, 107) } else { [System.Drawing.Color]::FromArgb(120, 120, 120) }
    $g.FillRectangle((New-Object System.Drawing.SolidBrush($bg)), 0, 0, 32, 32)
    $font = New-Object System.Drawing.Font('Malgun Gothic', 15, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = 'Center'; $fmt.LineAlignment = 'Center'
    $g.DrawString('옛', $font, [System.Drawing.Brushes]::White, (New-Object System.Drawing.RectangleF(0, 0, 32, 32)), $fmt)
    $g.Dispose()
    [System.Drawing.Icon]::FromHandle($bmp.GetHicon())
}

$tray = New-Object System.Windows.Forms.NotifyIcon
$tray.Text = '옛글 키보드 (Ctrl+Alt+Y)'
$menu = New-Object System.Windows.Forms.ContextMenuStrip
$miEnabled = $menu.Items.Add('옛글 변환 켜짐   Ctrl+Alt+O')
$miWindow = $menu.Items.Add('변환 창 열기   Ctrl+Alt+W')
$menu.Items.Add((New-Object System.Windows.Forms.ToolStripSeparator)) | Out-Null
$miRules = New-Object System.Windows.Forms.ToolStripMenuItem('바꿀 규칙')
$menu.Items.Add($miRules) | Out-Null
foreach ($k in $ruleNames.Keys) {
    $item = New-Object System.Windows.Forms.ToolStripMenuItem($ruleNames[$k])
    $item.Checked = $state[$k]
    $item.Tag = $k
    $item.CheckOnClick = $true
    $item.Add_CheckedChanged({ param($s, $e) $state[$s.Tag] = $s.Checked; Save-Settings; $script:conv = New-Converter })
    $miRules.DropDownItems.Add($item) | Out-Null
}
$startupLink = Join-Path ([Environment]::GetFolderPath('Startup')) '옛글 키보드.lnk'
$miStartup = New-Object System.Windows.Forms.ToolStripMenuItem('Windows 켤 때 자동 실행')
$miStartup.Checked = Test-Path $startupLink
$miStartup.Add_Click({
    if (Test-Path $startupLink) { Remove-Item $startupLink -Force; $miStartup.Checked = $false }
    else {
        $ws = New-Object -ComObject WScript.Shell
        $lnk = $ws.CreateShortcut($startupLink)
        $lnk.TargetPath = (Get-Command powershell.exe).Source
        $lnk.Arguments = "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$PSCommandPath`""
        $lnk.Save()
        $miStartup.Checked = $true
    }
})
$menu.Items.Add($miStartup) | Out-Null
$menu.Items.Add((New-Object System.Windows.Forms.ToolStripSeparator)) | Out-Null
$miQuit = $menu.Items.Add('끝내기')

function Update-Enabled {
    $miEnabled.Text = $(if ($state.Enabled) { '✓ 옛글 변환 켜짐   Ctrl+Alt+O' } else { '옛글 변환 꺼짐   Ctrl+Alt+O' })
    $tray.Icon = New-TrayIcon $state.Enabled
}
function Toggle-Enabled { $state.Enabled = -not $state.Enabled; Save-Settings; Update-Enabled
    $tray.ShowBalloonTip(1500, '옛글 키보드', $(if ($state.Enabled) { '켜짐: Ctrl+Alt+Y로 바꿉니다' } else { '꺼짐' }), 'Info') }

$miEnabled.Add_Click({ Toggle-Enabled })
$miWindow.Add_Click({ Show-ConvertWindow })
$tray.ContextMenuStrip = $menu
$tray.Add_DoubleClick({ Show-ConvertWindow })
Update-Enabled
$tray.Visible = $true

# ───────────── 전역 단축키
$hot = New-Object Yetgeul.HotkeyWindow
$MOD_ALT = 1; $MOD_CTRL = 2; $MOD_NOREPEAT = 0x4000
$ok1 = $hot.Register(1, $MOD_CTRL + $MOD_ALT + $MOD_NOREPEAT, [System.Windows.Forms.Keys]::Y)
$ok2 = $hot.Register(2, $MOD_CTRL + $MOD_ALT + $MOD_NOREPEAT, [System.Windows.Forms.Keys]::W)
$ok3 = $hot.Register(3, $MOD_CTRL + $MOD_ALT + $MOD_NOREPEAT, [System.Windows.Forms.Keys]::O)
$hot.add_Pressed({
    param($id)
    switch ($id) {
        1 { Convert-InPlace }
        2 { Show-ConvertWindow }
        3 { Toggle-Enabled }
    }
})

$msg = if ($ok1) { '실행됐습니다. 치고 나서 Ctrl+Alt+Y를 누르면 옛 표기로 바뀝니다.' } else { 'Ctrl+Alt+Y를 다른 프로그램이 쓰고 있어 등록하지 못했습니다. 변환 창(아이콘 더블클릭)을 쓰세요.' }
$tray.ShowBalloonTip(3000, '옛글 키보드', $msg, 'Info')

$miQuit.Add_Click({
    $tray.Visible = $false
    $hot.Dispose()
    [System.Windows.Forms.Application]::Exit()
})

$ctx = New-Object System.Windows.Forms.ApplicationContext
[System.Windows.Forms.Application]::Run($ctx)
$tray.Dispose()
$mutex.ReleaseMutex()
