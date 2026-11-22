package com.example.bookapp.data

import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.MessageDigest

/** بسته رسمی دفتر معین‌البکا؛ فقط در Flavor مدیر ساخته و امضا می‌شود. */
object DirectorOfficePublicationExporter {
    const val MIME_TYPE = "application/vnd.tazieh.director-office+json"
    private const val SCHEMA = "tazieh-director-office"
    private const val SCHEMA_VERSION = 1

    private fun privateKey() = run {
        val encoded = com.example.bookapp.BuildConfig.POLICY_PRIVATE_KEY
        require(encoded.isNotBlank()) {
            "کلید خصوصی امضای بسته رسمی تنظیم نشده است. مقدار ADMIN_POLICY_PRIVATE_KEY را در local.properties یا GitHub Secrets قرار دهید."
        }
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(bytes))
    }

    private fun sign(payloadText: String): String {
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(privateKey())
        signer.update(payloadText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(signer.sign(), Base64.NO_WRAP)
    }

    suspend fun createShareUri(context: Context, db: AppDatabase, planId: Long): Uri {
        val dao = db.directorDao()
        val plan = dao.plan(planId) ?: error("دفتر انتخاب‌شده پیدا نشد.")
        require(plan.status == DirectorPlanEntity.STATUS_PUBLISHED && !plan.isDeleted) {
            "فقط دفتر منتشرشده و فعال قابل انتقال به Viewer است."
        }
        val versions = dao.versions(plan.id)
            .filter { it.status == DirectorPlanVersionEntity.STATUS_PUBLISHED }
            .sortedBy { it.versionNumber }
        require(versions.isNotEmpty()) { "برای این دفتر نسخه منتشرشده‌ای وجود ندارد." }

        val planJson = JSONObject()
            .put("uid", plan.uid)
            .put("taziehUid", plan.taziehUid)
            .put("title", plan.title)
            .put("description", plan.description)
            .put("createdAt", plan.createdAt)
            .put("updatedAt", plan.updatedAt)
        val versionArray = JSONArray()
        for (version in versions) {
            val turnsArray = JSONArray()
            val turns = dao.turns(version.id).sortedWith(compareBy<DirectorTurnEntity> { it.sequenceIndex }.thenBy { it.id })
            val sequenceById = turns.associate { it.id to it.sequenceIndex }
            for (turn in turns) {
                val participants = JSONArray()
                dao.participants(turn.id).forEach { participant ->
                    participants.put(JSONObject()
                        .put("roleUid", participant.roleUid)
                        .put("roleTitle", participant.roleTitle)
                        .put("orderIndex", participant.orderIndex)
                        .put("isLead", participant.isLead))
                }
                val segments = JSONArray()
                dao.textSegments(turn.id).forEach { segment ->
                    segments.put(JSONObject()
                        .put("segmentIndex", segment.segmentIndex)
                        .put("text", segment.text)
                        .put("sectionUid", segment.sectionUid ?: JSONObject.NULL)
                        .put("sourcePageStart", segment.sourcePageStart ?: JSONObject.NULL)
                        .put("sourcePageEnd", segment.sourcePageEnd ?: JSONObject.NULL))
                }
                turnsArray.put(JSONObject()
                    .put("turnNumber", turn.turnNumber)
                    .put("sequenceIndex", turn.sequenceIndex)
                    .put("turnType", turn.turnType)
                    .put("title", turn.title)
                    .put("continuityGroupUid", turn.continuityGroupUid)
                    .put("isEnabled", turn.isEnabled)
                    .put("notes", turn.notes)
                    .put("participants", participants)
                    .put("segments", segments))
            }
            val cuesArray = JSONArray()
            dao.stageCues(version.id).forEach { cue ->
                cuesArray.put(JSONObject()
                    .put("turnSequenceIndex", cue.turnId?.let { sequenceById[it] } ?: JSONObject.NULL)
                    .put("cueType", cue.cueType)
                    .put("title", cue.title)
                    .put("instruction", cue.instruction)
                    .put("orderIndex", cue.orderIndex)
                    .put("blocksNextTurn", cue.blocksNextTurn)
                    .put("continuationRoleUid", cue.continuationRoleUid))
            }
            versionArray.put(JSONObject()
                .put("versionNumber", version.versionNumber)
                .put("title", version.title)
                .put("changeNotes", version.changeNotes)
                .put("createdAt", version.createdAt)
                .put("publishedAt", version.publishedAt ?: JSONObject.NULL)
                .put("turns", turnsArray)
                .put("cues", cuesArray))
        }
        val payload = JSONObject()
            .put("schema", SCHEMA)
            .put("schemaVersion", SCHEMA_VERSION)
            .put("issuedAt", System.currentTimeMillis())
            .put("plan", planJson)
            .put("versions", versionArray)
        val payloadText = payload.toString()
        val envelope = JSONObject()
            .put("schema", SCHEMA)
            .put("schemaVersion", SCHEMA_VERSION)
            .put("payload", payloadText)
            .put("signature", sign(payloadText))
        val safeName = plan.uid.replace(Regex("[^A-Za-z0-9_-]"), "_").take(48)
        val file = File(context.cacheDir, "director_official_${safeName}.tazoffice")
        file.writeText(envelope.toString(2), Charsets.UTF_8)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun createShareIntent(uri: Uri): Intent = Intent(Intent.ACTION_SEND).apply {
        type = MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(null, "بسته رسمی دفتر معین‌البکا", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
