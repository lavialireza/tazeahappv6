# Footnote Stage 24 — Remaining Footnote Features

Implemented on top of Stage 23 without changing the existing gallery/access-management work:

1. Footnote terms appearing in the text are marked with an underline and are tappable.
2. Tapping a marked term opens its footnote word/explanation.
3. Each footnote result has a search/jump action that moves the reader near the matching term in the text.
4. A footnote can be transferred to the persistent user dictionary with one tap.
5. User-added dictionary entries persist in app preferences and are displayed in the dictionary screen.
6. Duplicate checking now covers both the built-in dictionary and persistent user dictionary.
7. Duplicate footnote terms are blocked within the same section, with Persian/Arabic character normalization; editing also blocks duplicates except the current record.

Build note: Android SDK/Gradle wrapper is not available in this execution environment, so no APK build was claimed. ZIP structure and Kotlin delimiter balance were checked locally.
