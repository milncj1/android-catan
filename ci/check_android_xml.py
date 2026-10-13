#!/usr/bin/env python3
"""Fail fast on malformed Android XML before the more expensive Gradle build."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

files = [Path("app/src/main/AndroidManifest.xml")]
files += sorted(Path("app/src/main/res").rglob("*.xml"))
errors = []

for path in files:
    try:
        ET.parse(path)
    except ET.ParseError as exc:
        errors.append(f"{path}: {exc}")

print(f"Validated {len(files)} Android XML files")
for error in errors:
    print(error, file=sys.stderr)

if errors:
    sys.exit(1)
