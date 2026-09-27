#!/usr/bin/env python3
"""
ساخت خودکار فایل update.json برای سرور بروزرسانی Tazieh Studio، داخل CI.

این اسکریپت فقط مقادیر آماده (نام پکیج/versionCode/versionName استخراج‌شده
از خود APK با aapt، و SHA-256 محاسبه‌شده با sha256sum) را می‌گیرد و دقیقاً
همان قالب JSON را می‌سازد که UpdateHelper.checkForUpdate در برنامه انتظار
دارد (همان کلیدهایی که در update-server-template/*/update.json هست).

Usage:
  generate_update_manifest.py \
      --package com.example.bookapp \
      --version-code 123 \
      --version-name "1.0-build123+abcdef" \
      --apk-url https://.../TaziehStudio-Admin.apk \
      --sha256 <64 hex chars> \
      --output out/admin/update.json \
      [--force-update true|false] \
      [--min-supported-version 0] \
      [--release-notes "خط اول\nخط دوم"]
"""
import argparse
import datetime
import json
import re
import sys

KNOWN_PACKAGES = {
    "com.example.bookapp": "admin",
    "com.example.bookapp.viewer": "viewer",
}

SHA256_RE = re.compile(r"^[0-9a-f]{64}$")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--package", required=True)
    parser.add_argument("--version-code", required=True, type=int)
    parser.add_argument("--version-name", required=True)
    parser.add_argument("--apk-url", required=True)
    parser.add_argument("--sha256", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--force-update", default="false")
    parser.add_argument("--min-supported-version", default="0", type=int)
    parser.add_argument("--release-notes", default="")
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    access = KNOWN_PACKAGES.get(args.package)
    if access is None:
        print(f"ERROR: unknown package name {args.package!r} (expected one of {list(KNOWN_PACKAGES)})", file=sys.stderr)
        return 1

    apk_url = args.apk_url.strip()
    if not apk_url.startswith("https://"):
        print("ERROR: --apk-url must start with https:// (plain-text/downgraded connections are rejected by the app).", file=sys.stderr)
        return 1

    sha256 = args.sha256.strip().lower()
    if not SHA256_RE.match(sha256):
        print("ERROR: --sha256 must be exactly 64 hex characters.", file=sys.stderr)
        return 1

    if args.version_code <= 0:
        print("ERROR: --version-code must be a positive integer.", file=sys.stderr)
        return 1

    notes = [line.strip() for line in args.release_notes.splitlines() if line.strip()]

    manifest = {
        "packageName": args.package,
        "access": access,
        "versionCode": args.version_code,
        "versionName": args.version_name,
        "apkUrl": apk_url,
        "sha256": sha256,
        "forceUpdate": str(args.force_update).strip().lower() == "true",
        "minSupportedVersion": args.min_supported_version,
        "releaseDate": datetime.date.today().isoformat(),
        "releaseNotes": notes,
    }

    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)
        f.write("\n")

    print(f"Wrote {args.output}  (package={args.package}, access={access}, versionCode={args.version_code}, sha256={sha256[:12]}...)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
