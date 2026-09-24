package com.example.bookapp.data

import org.json.JSONArray
import org.json.JSONObject

/** Strict validation for content files before they are imported. */
object ContentValidator {
    fun validate(text: String): List<String> {
        val errors = mutableListOf<String>()
        val root = try { JSONObject(text) } catch (_: Exception) { null }
        val fields = if (root != null) root.optJSONArray("fields") else try { JSONArray(text) } catch (_: Exception) { null }
        if (fields == null) return listOf("ریشه فایل باید آرایه JSON یا شیء دارای fields باشد")

        val fieldUids = mutableSetOf<String>()
        val taziehUids = mutableSetOf<String>()
        val roleUids = mutableSetOf<String>()
        val sectionUids = mutableSetOf<String>()

        for (fi in 0 until fields.length()) {
            val f = fields.optJSONObject(fi)
            if (f == null) {
                errors += "Field شماره $fi معتبر نیست"
                continue
            }
            validateOptionalUid(f, "Field", fieldUids, errors)
            if (f.optString("title").isBlank()) errors += "Field شماره $fi عنوان ندارد"

            val taziehs = f.optJSONArray("taziehs")
            if (taziehs == null) {
                errors += "Field شماره $fi آرایه taziehs ندارد"
                continue
            }
            for (ti in 0 until taziehs.length()) {
                val t = taziehs.optJSONObject(ti)
                if (t == null) {
                    errors += "Tazieh $fi/$ti معتبر نیست"
                    continue
                }
                validateOptionalUid(t, "Tazieh", taziehUids, errors)

                val roles = t.optJSONArray("roles")
                if (roles == null) {
                    errors += "Tazieh $fi/$ti آرایه roles ندارد"
                    continue
                }
                for (ri in 0 until roles.length()) {
                    val r = roles.optJSONObject(ri)
                    if (r == null) {
                        errors += "Role $fi/$ti/$ri معتبر نیست"
                        continue
                    }
                    validateOptionalUid(r, "Role", roleUids, errors)

                    val sections = r.optJSONArray("sections")
                    if (sections == null) {
                        errors += "Role $fi/$ti/$ri آرایه sections ندارد"
                        continue
                    }
                    for (si in 0 until sections.length()) {
                        val s = sections.optJSONObject(si)
                        if (s == null) {
                            errors += "Section $fi/$ti/$ri/$si معتبر نیست"
                            continue
                        }
                        validateOptionalUid(s, "Section", sectionUids, errors)
                        if (!s.has("content")) errors += "Section ${s.optString("uid", "")} فیلد content ندارد"
                    }
                }
            }
        }
        return errors
    }

    /**
     * UIDs are optional because the public/external content schema intentionally
     * contains only title/taziehs/roles/sections/content. When a UID is supplied
     * by an internal content package, still validate its uniqueness.
     */
    private fun validateOptionalUid(
        obj: JSONObject,
        kind: String,
        seen: MutableSet<String>,
        errors: MutableList<String>
    ) {
        val uid = obj.optString("uid").trim()
        if (uid.isNotBlank() && !seen.add(uid)) {
            errors += "$kind دارای uid تکراری است: $uid"
        }
    }
}
