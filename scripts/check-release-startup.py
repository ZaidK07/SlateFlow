#!/usr/bin/env python3
"""Install a signed release APK and catch delayed startup crashes without clearing app data."""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time

adb = os.environ.get('ADB') or shutil.which('adb') or str(Path.home() / 'Library/Android/sdk/platform-tools/adb')
package = 'com.zaid01.slateflow'

def run(*args):
    return subprocess.run([adb, *args], check=True, capture_output=True, text=True).stdout

if len(sys.argv) != 2:
    sys.exit('Usage: python3 scripts/check-release-startup.py SIGNED_APK')
print(run('install', '-r', sys.argv[1]).strip())
run('shell', 'am', 'force-stop', package)
run('logcat', '-b', 'crash', '-c')
print(run('shell', 'am', 'start', '-W', '-n', package + '/.MainActivity').strip())
for _ in range(10):
    time.sleep(1)
    crash = run('logcat', '-b', 'crash', '-d')
    if package in crash:
        sys.exit(crash)
    run('shell', 'pidof', package)
run('shell', 'uiautomator', 'dump', '/sdcard/slateflow-startup.xml')
ui = run('shell', 'cat', '/sdcard/slateflow-startup.xml')
if 'package="' + package + '"' not in ui:
    sys.exit('SlateFlow did not remain visible after launch.')
print('PASS: signed release stayed alive and visible for 10 seconds; no app crash logged.')
