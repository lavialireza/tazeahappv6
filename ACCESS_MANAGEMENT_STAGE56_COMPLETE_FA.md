# Stage56 — Clean Permission Tree + Role & Tazieh Correction Dictionary

Base: Stage55 Permission Engine.

## Implemented
- Complete parent/child permission tree for the existing Viewer feature set.
- Collapsed category UI by default; matching paths expand when searching.
- Category and parent bulk toggles update child permissions as well.
- Gallery children: view, add, edit caption, delete, large preview.
- Notes children: view, add, edit, delete.
- Bookmarks children: view, add, delete.
- Footnotes children: view, add, edit, delete, dictionary sync.
- My Role children: view, select, remove, rehearse, PDF.
- General Tazieh glossary children: view, add, edit, delete.
- New persistent Tazieh Correction Dictionary with view, add, edit, delete and apply-to-text permissions.
- Calendar children: view and suggestions.
- Text reader uses child permissions for navigation, audio, TTS, copy and sharing.
- Legacy Stage53 `footnoteSync` import compatibility retained and mapped to `footnoteSync.dictionary`.
- Parent/child permission enforcement remains centralized through ViewerPermissionEngine.

## Verification
- ZIP integrity verified with `unzip -t` after packaging.
- Android SDK/Gradle is not available in this environment, so Android Build success is NOT claimed.
