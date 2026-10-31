# Mutation check: plant one bug, run the tests that should catch it, restore the file.
# Usage: . docs\evidence\tools\mutate.ps1; Invoke-Mutations $list "docs\evidence\<item>\mutation_check.txt"
# Each entry: @{ Name; File; From; To; Tests (gradle --tests filter or instrumented class); Task (default testDebugUnitTest) }
# Instrumented runs go through scripts\run-tests.cmd on the TEST emulator only (Pixel_API_34_test).
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
if (-not $env:ANDROID_SERIAL -or $env:ANDROID_SERIAL -eq "emulator-5554") { $env:ANDROID_SERIAL = "emulator-5560" }

function Invoke-Mutations($mutations, [string]$report) {
    $lines = @("Mutation check run " + (Get-Date -Format "yyyy-MM-dd HH:mm") + " (instrumented runs on $env:ANDROID_SERIAL)", "")
    foreach ($m in $mutations) {
        $original = [IO.File]::ReadAllText($m.File)
        if (-not $original.Contains($m.From)) { $lines += "SKIPPED (pattern not found): $($m.Name)"; continue }
        $mutated = $original.Replace($m.From, $m.To)
        [IO.File]::WriteAllText($m.File, $mutated)
        try {
            $task = if ($m.Task) { $m.Task } else { "testDebugUnitTest" }
            if ($task -eq "testDebugUnitTest") {
                $out = & .\gradlew.bat $task --tests $m.Tests --console=plain -q 2>&1
            } else {
                # The guarded script refuses anything but the test AVD and leaves the app installed.
                $out = cmd /c "scripts\run-tests.cmd $task `"-Pandroid.testInstrumentationRunnerArguments.class=$($m.Tests)`" --console=plain -q 2>&1"
            }
            $code = $LASTEXITCODE
        } finally {
            [IO.File]::WriteAllText($m.File, $original)
        }
        $verdict = if ($code -ne 0) { "KILLED (tests failed as they should)" } else { "SURVIVED (tests still pass: strengthen them)" }
        $dir = if ($task -eq "testDebugUnitTest") { "app\build\test-results\testDebugUnitTest" } else { "app\build\outputs\androidTest-results\connected" }
        # Name the tests that really failed, from the result XML (root can be <testsuites>).
        $failed = Get-ChildItem $dir -Recurse -Filter *.xml -ErrorAction SilentlyContinue |
            ForEach-Object { ([xml](Get-Content $_.FullName)).SelectNodes("//testcase[failure]") } |
            ForEach-Object { "    failed: " + $_.classname + "." + $_.name }
        if (-not $failed) { $failed = ($out | Select-Object -Last 3 | ForEach-Object { "    " + "$_".Trim() }) }
        $lines += "$verdict : $($m.Name)"
        $lines += "    file: $($m.File)"
        $lines += "    change: '$($m.From.Trim())' -> '$($m.To.Trim())'"
        $lines += "    tests: $($m.Tests)"
        $lines += $failed
        $lines += ""
    }
    [IO.File]::WriteAllLines($report, $lines, (New-Object System.Text.UTF8Encoding $false))
    $lines
}
