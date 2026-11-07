param([string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot))
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Speech
$synthesizer = New-Object System.Speech.Synthesis.SpeechSynthesizer
$voice = $synthesizer.GetInstalledVoices() | Where-Object { $_.Enabled -and $_.VoiceInfo.Culture.Name -eq 'ja-JP' } | Select-Object -First 1
if (-not $voice) { throw 'Install a Japanese Windows speech voice before regenerating audio. Existing WAV assets can be used on any OS.' }
$synthesizer.SelectVoice($voice.VoiceInfo.Name)
$synthesizer.Rate = -1
$audioRoot = Join-Path $ProjectRoot 'src/main/resources/static/audio'
New-Item -ItemType Directory -Path $audioRoot -Force | Out-Null
$clips = @{
    n0 = 'おはようございます。私はアンです。学生です。よろしくお願いします。'
    n5 = '毎日、九時から五時まで働きます。お昼に一時間休みます。'
    n4 = '資料を確認してから、会議を始めます。まだ確認していない方は、先に資料を読んでください。'
    n3 = '検証結果は、午後三時までに担当者へ送ってください。変更点も一緒に報告してください。'
    n2 = '一部の検証が完了していないため、全体への適用は見合わせます。影響範囲を確認したうえで、改めて判断する必要があります。'
}
try {
    foreach ($clip in $clips.GetEnumerator()) {
        $synthesizer.SetOutputToWaveFile((Join-Path $audioRoot ($clip.Key + '.wav')))
        $synthesizer.Speak($clip.Value)
        $synthesizer.SetOutputToNull()
    }
} finally { $synthesizer.Dispose() }
