# Build companion to a single .exe using PyInstaller
# Run from the companion/ directory

$ErrorActionPreference = "Stop"

Write-Host "Installing dependencies..."
pip install -r requirements.txt

Write-Host "Building .exe with PyInstaller..."
pyinstaller --onefile --name PresenterCompanion --icon NONE --noconsole `
    --hidden-import win32com `
    --hidden-import win32com.client `
    --hidden-import pythoncom `
    --hidden-import pywintypes `
    --add-data "$(python -c 'import qrcode; import os; print(os.path.dirname(qrcode.__file__))');qrcode" `
    main.py

Write-Host ""
Write-Host "Build complete: dist\PresenterCompanion.exe"
