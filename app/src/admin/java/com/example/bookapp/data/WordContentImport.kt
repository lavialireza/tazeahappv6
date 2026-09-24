package com.example.bookapp.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/** Native DOCX -> companion JSON schema parser. No third-party library is required. */
data class WordImportPreview(
    val valid: Boolean,
    val errors: List<String> = emptyList(),
    val fields: Int = 0,
    val taziehs: Int = 0,
    val roles: Int = 0,
    val sections: Int = 0,
    val paragraphs: Int = 0,
    val existingItems: Int = 0,
    val newItems: Int = 0
)

private data class WordParagraph(val style: String, val text: String)

fun readWordFromUri(context: Context, uri: Uri): ByteArray =
    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IllegalArgumentException("امکان خواندن فایل Word وجود ندارد")

fun wordToCompatibleJson(bytes: ByteArray): String {
    val entries = unzipDocx(bytes)
    val documentXml = entries["word/document.xml"] ?: throw IllegalArgumentException("فایل Word معتبر نیست: document.xml پیدا نشد")
    val stylesXml = entries["word/styles.xml"]
    val styleNames = parseStyles(stylesXml)
    val paragraphs = parseDocument(documentXml, styleNames)

    val fields = JSONArray()
    var currentField: JSONObject? = null
    var currentTazieh: JSONObject? = null
    var currentRole: JSONObject? = null
    var currentSection: JSONObject? = null

    fun ensureField(): JSONObject {
        if (currentField == null) {
            currentField = JSONObject().apply { put("title", "بدون زمینه"); put("taziehs", JSONArray()) }
            fields.put(currentField)
        }
        return currentField!!
    }
    fun ensureTazieh(): JSONObject {
        val f = ensureField()
        if (currentTazieh == null) {
            currentTazieh = JSONObject().apply { put("title", "بدون تعزیه"); put("roles", JSONArray()) }
            f.getJSONArray("taziehs").put(currentTazieh)
        }
        return currentTazieh!!
    }
    fun ensureRole(): JSONObject {
        val t = ensureTazieh()
        if (currentRole == null) {
            currentRole = JSONObject().apply { put("title", "بدون نقش"); put("sections", JSONArray()) }
            t.getJSONArray("roles").put(currentRole)
        }
        return currentRole!!
    }

    paragraphs.forEach { p ->
        val text = p.text.trim()
        if (text.isBlank()) return@forEach
        when (headingLevel(p.style)) {
            1 -> {
                currentField = JSONObject().apply { put("title", text); put("taziehs", JSONArray()) }
                fields.put(currentField)
                currentTazieh = null; currentRole = null; currentSection = null
            }
            2 -> {
                val f = ensureField()
                currentTazieh = JSONObject().apply { put("title", text); put("roles", JSONArray()) }
                f.getJSONArray("taziehs").put(currentTazieh)
                currentRole = null; currentSection = null
            }
            3 -> {
                val t = ensureTazieh()
                currentRole = JSONObject().apply { put("title", text); put("sections", JSONArray()) }
                t.getJSONArray("roles").put(currentRole)
                currentSection = null
            }
            4 -> {
                val r = ensureRole()
                currentSection = JSONObject().apply { put("title", text); put("content", "") }
                r.getJSONArray("sections").put(currentSection)
            }
            else -> {
                if (currentSection != null) {
                    val old = currentSection!!.optString("content", "")
                    currentSection!!.put("content", if (old.isBlank()) text else "$old\n$text")
                }
            }
        }
    }

    return fields.toString(2)
}

suspend fun previewWordImport(db: AppDatabase, bytes: ByteArray): Pair<WordImportPreview, String> {
    val json = try { wordToCompatibleJson(bytes) } catch (e: Exception) {
        return WordImportPreview(false, listOf(e.message ?: "خطا در خواندن فایل Word")) to ""
    }
    val jsonPreview = previewContentImport(db, json)
    val root = JSONArray(json)
    var paragraphs = 0
    for (i in 0 until root.length()) {
        val f = root.getJSONObject(i)
        val ts = f.getJSONArray("taziehs")
        for (j in 0 until ts.length()) {
            val rs = ts.getJSONObject(j).getJSONArray("roles")
            for (k in 0 until rs.length()) {
                val ss = rs.getJSONObject(k).getJSONArray("sections")
                for (m in 0 until ss.length()) {
                    val c = ss.getJSONObject(m).optString("content", "")
                    if (c.isNotBlank()) paragraphs += c.split('\n').count { it.isNotBlank() }
                }
            }
        }
    }
    val preview = WordImportPreview(
        valid = jsonPreview.valid,
        errors = jsonPreview.errors,
        fields = jsonPreview.fields,
        taziehs = jsonPreview.taziehs,
        roles = jsonPreview.roles,
        sections = jsonPreview.sections,
        paragraphs = paragraphs,
        existingItems = jsonPreview.existingItems,
        newItems = jsonPreview.newItems
    )
    return preview to json
}

suspend fun importWordContent(db: AppDatabase, bytes: ByteArray): WordImportPreview {
    val (preview, json) = previewWordImport(db, bytes)
    if (!preview.valid) return preview
    importContentJson(db, json)
    return preview
}

private fun unzipDocx(bytes: ByteArray): Map<String, ByteArray> {
    val out = mutableMapOf<String, ByteArray>()
    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        while (true) {
            val e = zip.nextEntry ?: break
            if (!e.isDirectory) out[e.name] = zip.readBytes()
            zip.closeEntry()
        }
    }
    return out
}

private fun parseStyles(xml: ByteArray?): Map<String, String> {
    if (xml == null) return emptyMap()
    val map = mutableMapOf<String, String>()
    val parser = newParser(xml)
    var currentId: String? = null
    var currentName: String? = null
    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
        if (parser.eventType == XmlPullParser.START_TAG) {
            when (parser.name) {
                "style" -> { currentId = parser.attr("styleId"); currentName = null }
                "name" -> if (currentId != null) currentName = parser.attr("val")
            }
        } else if (parser.eventType == XmlPullParser.END_TAG && parser.name == "style") {
            if (currentId != null) map[currentId!!] = currentName ?: currentId!!
            currentId = null; currentName = null
        }
        parser.next()
    }
    return map
}

private fun parseDocument(xml: ByteArray, styleNames: Map<String, String>): List<WordParagraph> {
    val result = mutableListOf<WordParagraph>()
    val parser = newParser(xml)
    var inParagraph = false
    var styleId = ""
    var text = StringBuilder()
    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
        if (parser.eventType == XmlPullParser.START_TAG) {
            when (parser.name) {
                "p" -> { inParagraph = true; styleId = ""; text = StringBuilder() }
                "pStyle" -> if (inParagraph) styleId = parser.attr("val") ?: ""
                "t" -> if (inParagraph) text.append(parser.nextText())
                "tab" -> if (inParagraph) text.append('\t')
                "br", "cr" -> if (inParagraph) text.append('\n')
            }
        } else if (parser.eventType == XmlPullParser.END_TAG && parser.name == "p") {
            if (inParagraph) result += WordParagraph(styleNames[styleId] ?: styleId, text.toString())
            inParagraph = false
        }
        parser.next()
    }
    return result
}

private fun headingLevel(style: String): Int {
    val s = style.lowercase().replace(" ", "").replace("-", "")
    return when {
        s == "heading1" || s == "heading01" || s.contains("heading1") || s.contains("عنوان1") || s.contains("عنوان۱") -> 1
        s == "heading2" || s == "heading02" || s.contains("heading2") || s.contains("عنوان2") || s.contains("عنوان۲") -> 2
        s == "heading3" || s == "heading03" || s.contains("heading3") || s.contains("عنوان3") || s.contains("عنوان۳") -> 3
        s == "heading4" || s == "heading04" || s.contains("heading4") || s.contains("عنوان4") || s.contains("عنوان۴") -> 4
        else -> 0
    }
}

private fun newParser(bytes: ByteArray): XmlPullParser =
    XmlPullParserFactory.newInstance().newPullParser().also {
        it.setInput(ByteArrayInputStream(bytes), "UTF-8")
    }

private fun XmlPullParser.attr(name: String): String? =
    getAttributeValue("http://schemas.openxmlformats.org/wordprocessingml/2006/main", name)
        ?: getAttributeValue(null, name)
