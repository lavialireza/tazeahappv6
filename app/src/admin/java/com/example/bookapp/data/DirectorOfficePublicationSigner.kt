package com.example.bookapp.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

/** فقط در flavor ادمین کامپایل می‌شود؛ کلید خصوصی هرگز به Viewer وارد نمی‌شود. */
object DirectorOfficePublicationSigner {

    private fun sign(payload: String): String {
        val encoded = com.example.bookapp.BuildConfig.POLICY_PRIVATE_KEY
        require(encoded.isNotBlank()) { "کلید خصوصی امضای انتشار تنظیم نشده است." }
        val key = KeyFactory.getInstance("RSA").generatePrivate(
            PKCS8EncodedKeySpec(Base64.decode(encoded, Base64.NO_WRAP))
        )
        return Signature.getInstance("SHA256withRSA").run {
            initSign(key)
            update(payload.toByteArray(Charsets.UTF_8))
            sign().joinToString("") { "%02x".format(it) }
        }
    }

    suspend fun createShareUri(context: Context, db: AppDatabase, planId: Long, versionId: Long): Uri {
        val dao = db.directorDao()
        val plan = dao.plan(planId) ?: error("دفتر انتخاب‌شده پیدا نشد.")
        require(plan.status == DirectorPlanEntity.STATUS_PUBLISHED) { "ابتدا دفتر را منتشر کنید." }
        val version = dao.version(versionId) ?: error("نسخه انتخاب‌شده پیدا نشد.")
        require(version.planId == plan.id && version.status == DirectorPlanVersionEntity.STATUS_PUBLISHED) {
            "فقط نسخه منتشرشده همین دفتر قابل ارسال است."
        }

        val turnsJson = JSONArray()
        dao.turns(version.id).forEach { turn ->
            val participants = JSONArray()
            dao.participants(turn.id).forEach { person ->
                participants.put(JSONObject().put("roleUid", person.roleUid).put("roleTitle", person.roleTitle)
                    .put("orderIndex", person.orderIndex).put("isLead", person.isLead))
            }
            val segments = JSONArray()
            dao.textSegments(turn.id).forEach { segment ->
                segments.put(JSONObject().put("segmentIndex", segment.segmentIndex).put("text", segment.text)
                    .put("sectionUid", segment.sectionUid ?: JSONObject.NULL))
            }
            turnsJson.put(JSONObject().put("turnNumber", turn.turnNumber).put("sequenceIndex", turn.sequenceIndex)
                .put("turnType", turn.turnType).put("title", turn.title)
                .put("continuityGroupUid", turn.continuityGroupUid).put("isEnabled", turn.isEnabled)
                .put("notes", turn.notes).put("participants", participants).put("segments", segments))
        }
        val cuesJson = JSONArray()
        dao.stageCues(version.id).forEach { cue ->
            val turnIndex = cue.turnId?.let { id -> dao.turns(version.id).indexOfFirst { it.id == id } } ?: -1
            cuesJson.put(JSONObject().put("turnIndex", turnIndex).put("cueType", cue.cueType)
                .put("title", cue.title).put("instruction", cue.instruction).put("orderIndex", cue.orderIndex)
                .put("blocksNextTurn", cue.blocksNextTurn).put("continuationRoleUid", cue.continuationRoleUid))
        }
        val payload = JSONObject()
            .put("schema", DirectorOfficePublicationContract.SCHEMA).put("schemaVersion", DirectorOfficePublicationContract.SCHEMA_VERSION)
            .put("issuedAt", System.currentTimeMillis())
            .put("plan", JSONObject().put("uid", plan.uid).put("taziehUid", plan.taziehUid)
                .put("title", plan.title).put("description", plan.description)
                .put("createdAt", plan.createdAt).put("updatedAt", plan.updatedAt))
            .put("version", JSONObject().put("versionNumber", version.versionNumber)
                .put("title", version.title).put("changeNotes", version.changeNotes)
                .put("publishedAt", version.publishedAt ?: JSONObject.NULL))
            .put("turns", turnsJson).put("stageCues", cuesJson)
        val payloadText = payload.toString()
        val envelope = JSONObject().put("schema", DirectorOfficePublicationContract.SCHEMA).put("schemaVersion", DirectorOfficePublicationContract.SCHEMA_VERSION)
            .put("payload", payload).put("signature", sign(payloadText))
        val safeUid = plan.uid.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(context.cacheDir, "director-office-$safeUid.json")
        file.writeText(envelope.toString(2), Charsets.UTF_8)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
