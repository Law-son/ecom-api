@echo off
setlocal enabledelayedexpansion

echo Removing unnecessary JavaDocs and comments...

for /r "src\main\java" %%f in (*.java) do (
    echo Processing: %%f
    powershell -Command "$content = Get-Content '%%f' -Raw; $content = $content -replace '(?s)/\*\*\s*\*\s*(Creates?|Returns?|Gets?|Sets?|Deletes?|Updates?|Retrieves?|Lists?|Finds?) (a|an|the) [^\*]*?\*/', ''; $content = $content -replace '(?m)^\s*//\s*(Creates?|Returns?|Gets?|Sets?|Deletes?|Updates?|Retrieves?|Lists?|Finds?) (a|an|the) .*$', ''; Set-Content '%%f' $content"
)

echo Done!
