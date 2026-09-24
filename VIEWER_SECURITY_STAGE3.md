# Viewer Security Stage 3 — Key Hardening

- Public Viewer remains fully usable offline.
- Existing AES-GCM `.taz` content remains compatible (Key Version 1).
- The V1 key material is no longer stored as one readable Kotlin password string.
- Release R8/obfuscation from Stage 2 remains enabled.
- Public Viewer remote content sync remains disabled.
- Admin features, reader features, PDF restriction, and Backup/Restore were not changed.

## Update compatibility
The key format is explicitly versioned. Existing V1 content continues to use Key Version 1. Future content formats can introduce a new key version without breaking existing content.

## Security boundary
This is offline APK hardening. It does not claim absolute secrecy against a rooted or fully compromised device, because the Viewer must decrypt content at runtime.
