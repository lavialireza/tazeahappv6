Stage53.2 — Parent/Child Access Management

Base: Tazieh Android ACCESS Stage53

Implemented:
- All access categories are presented as parent groups.
- Existing feature permissions are the child permissions of each parent.
- Parent switch enables/disables all children in that category.
- Each child remains independently configurable.
- Parent state shows all/partial/none of its children.
- Search still filters child capabilities without removing the parent relationship.
- Footnote dictionary sync remains an independent child permission of the research parent.
- Fixed the Stage53 compile error by passing canAddFootnoteToDictionary into FootnotesSection.

Important:
- This package was not Android-built in this environment; no Build Success claim is made.
- The supplied CI log showed the Stage53 compile failure in TextScreen.kt; the missing parameter connection is fixed in this source package.
