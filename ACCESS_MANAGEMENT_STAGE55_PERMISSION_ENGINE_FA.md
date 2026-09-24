# Tazieh Android ACCESS — Stage55

## Permission Engine & Viewer Enforcement

Base: Stage54.1

Implemented:
- Added `ViewerPermissionEngine` as the central permission-checking layer.
- `AppNavigation.featureEnabled()` now delegates Viewer access checks to the central engine.
- Kept parent/child effective-permission inheritance in `ViewerAccessPolicy.hasPermission()`.
- Upgraded Admin access editor to a three-level UI: category -> feature parent -> child capability.
- Category switches now bulk-control their feature parents; feature-parent switches control their real child capabilities.
- Child switches are disabled when their parent is disabled.
- Search can expose matching parents/children.
- Footnote permissions remain granular, including view/add/edit/delete and dictionary synchronization.
- Removed an accidental duplicate `markSentIfSpecial()` call from the direct policy-send path.

Important:
- This package was checked for ZIP integrity after creation.
- Android SDK/Gradle was not available in this environment, so Android Build success is NOT claimed.
