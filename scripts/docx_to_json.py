# -*- coding: utf-8 -*-
"""
اسکریپت تبدیل فایل ورد (docx) به فایل JSON مورد نیاز اپلیکیشن تعزیه.

نحوه‌ی نوشتن فایل ورد شما (قرارداد ساده با استایل‌های Heading):
    Heading 1  -> عنوان زمینه        (مثلاً: اصفهان، تهران، میرعزا)
    Heading 2  -> عنوان تعزیه        (مثلاً: عاشورا، بازار شام)
    Heading 3  -> عنوان نقش          (مثلاً: شمر، امام، یزید)
    Heading 4  -> عنوان بخش          (مثلاً: ورود، ساقی‌نامه، شهادت)
    پاراگراف‌های معمولی زیر Heading 4  -> متن اشعار همان بخش

نصب پیش‌نیاز:
    pip install python-docx --break-system-packages

اجرا (روش پیشنهادی و ساده‌تر - افزودن خودکار به‌عنوان فایل جدید):
    python docx_to_json.py new_majles.docx
    (خروجی خودش به‌صورت یک فایل تازه و شماره‌دار، مثلاً 002_new-majles.json،
     در پوشه‌ی app/src/admin/assets/content/ ساخته می‌شود. کافیست این فایل تازه
     را commit/push کنید؛ محتوای قبلی دست‌نخورده می‌ماند و فقط همین مجلس تازه
     به برنامه اضافه می‌شود - نیازی به جایگزینی کل فایل داده نیست.)

اجرا (روش قدیمی - مسیر خروجی دلخواه):
    python docx_to_json.py input.docx output.json
"""

import os
import re
import sys
import json
import hashlib
from docx import Document

# پوشه‌ی پیش‌فرض محتوای تدریجی برنامه (نسبت به مسیر همین اسکریپت)
DEFAULT_CONTENT_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "content"
)


def convert(docx_path: str) -> list:
    doc = Document(docx_path)

    fields = []
    current_field = None
    current_tazieh = None
    current_role = None
    current_section = None

    for para in doc.paragraphs:
        style = para.style.name if para.style else ""
        text = para.text.strip()
        if not text:
            continue

        if style == "Heading 1":
            current_field = {"title": text, "taziehs": []}
            fields.append(current_field)
            current_tazieh = current_role = current_section = None

        elif style == "Heading 2":
            if current_field is None:
                current_field = {"title": "بدون زمینه", "taziehs": []}
                fields.append(current_field)
            current_tazieh = {"title": text, "roles": []}
            current_field["taziehs"].append(current_tazieh)
            current_role = current_section = None

        elif style == "Heading 3":
            if current_field is None:
                current_field = {"title": "بدون زمینه", "taziehs": []}
                fields.append(current_field)
            if current_tazieh is None:
                current_tazieh = {"title": "بدون تعزیه", "roles": []}
                current_field["taziehs"].append(current_tazieh)
            current_role = {"title": text, "sections": []}
            current_tazieh["roles"].append(current_role)
            current_section = None

        elif style == "Heading 4":
            if current_field is None:
                current_field = {"title": "بدون زمینه", "taziehs": []}
                fields.append(current_field)
            if current_tazieh is None:
                current_tazieh = {"title": "بدون تعزیه", "roles": []}
                current_field["taziehs"].append(current_tazieh)
            if current_role is None:
                current_role = {"title": "بدون نقش", "sections": []}
                current_tazieh["roles"].append(current_role)
            current_section = {"title": text, "content": ""}
            current_role["sections"].append(current_section)

        else:
            # پاراگراف متن معمولی (شعر)
            if current_section is not None:
                if current_section["content"]:
                    current_section["content"] += "\n" + text
                else:
                    current_section["content"] = text

    return fields


def stable_uid(kind: str, parent_uid: str, title: str, occurrence: int = 0) -> str:
    normalized = re.sub(r"\s+", " ", title.strip())
    return hashlib.sha256(f"{kind}|{parent_uid}|{normalized}|{occurrence}".encode("utf-8")).hexdigest()

def add_stable_uids(fields: list) -> list:
    for field in fields:
        field["uid"] = stable_uid("field", "root", field["title"])
        for tazieh in field.get("taziehs", []):
            tazieh["uid"] = stable_uid("tazieh", field["uid"], tazieh["title"])
            for role in tazieh.get("roles", []):
                role["uid"] = stable_uid("role", tazieh["uid"], role["title"])
                occurrences = {}
                for section in role.get("sections", []):
                    key = re.sub(r"\s+", " ", section["title"].strip())
                    occurrence = occurrences.get(key, 0)
                    occurrences[key] = occurrence + 1
                    section["uid"] = stable_uid("section", role["uid"], section["title"], occurrence)
    return fields


def next_content_filename(content_dir: str, input_path: str) -> str:
    """نام فایل بعدی و شماره‌دار (مثل 002_my-file.json) را در پوشه‌ی content تعیین می‌کند،
    بدون اینکه فایلی بسازد؛ فقط مسیر کامل خروجی را برمی‌گرداند."""
    os.makedirs(content_dir, exist_ok=True)

    existing = [f for f in os.listdir(content_dir) if re.match(r"^\d{3}_.*\.json$", f)]
    next_num = 1
    for f in existing:
        n = int(f[:3])
        if n >= next_num:
            next_num = n + 1

    base_name = os.path.splitext(os.path.basename(input_path))[0]
    slug = re.sub(r"[^\w\-]+", "-", base_name).strip("-") or "majles"
    output_name = f"{next_num:03d}_{slug}.json"
    return os.path.join(content_dir, output_name)


if __name__ == "__main__":
    if len(sys.argv) not in (2, 3):
        print("استفاده:")
        print("  python docx_to_json.py input.docx                (افزودن خودکار به assets/content)")
        print("  python docx_to_json.py input.docx output.json     (نوشتن در مسیر دلخواه)")
        sys.exit(1)

    input_path = sys.argv[1]
    result = convert(input_path)

    if len(sys.argv) == 3:
        # روش قدیمی: مسیر خروجی صراحتاً داده شده
        output_path = sys.argv[2]
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(add_stable_uids(result), f, ensure_ascii=False, indent=2)
        print(f"تمام شد. فایل خروجی: {output_path}")
    else:
        # روش جدید: ساخت خودکار یک فایل تازه و شماره‌دار در پوشه‌ی content
        output_path = next_content_filename(DEFAULT_CONTENT_DIR, input_path)

        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(add_stable_uids(result), f, ensure_ascii=False, indent=2)

        print(f"تمام شد. فایل جدید ساخته شد: app/src/admin/assets/content/{os.path.basename(output_path)}")
        print("این فایل را commit/push کنید؛ همین یک فایل به‌عنوان محتوای تازه اضافه می‌شود.")
