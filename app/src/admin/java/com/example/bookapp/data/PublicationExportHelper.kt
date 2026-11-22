package com.example.bookapp.data

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

/** خروجی داخلیِ نسخه‌دار؛ جدا از قرارداد محدود و ثابت برنامه جانبی. */
suspend fun exportInternalContentJson(db: AppDatabase): String {
    val root = JSONObject().put("schema", "tazieh-content-internal").put("schemaVersion", 1)
        .put("exportedAt", System.currentTimeMillis())
    val fields = JSONArray()
    db.fieldDao().getAll().forEach { field ->
        fields.put(JSONObject().put("uid", field.uid).put("title", field.title).put("taziehs", JSONArray().also { ts ->
            db.taziehDao().getByField(field.id).forEach { t ->
                ts.put(JSONObject().put("uid", t.uid).put("title", t.title).put("author", t.author ?: JSONObject.NULL)
                    .put("roles", JSONArray().also { rs ->
                        db.roleDao().getByTazieh(t.id).forEach { r ->
                            rs.put(JSONObject().put("uid", r.uid).put("title", r.title).put("sections", JSONArray().also { ss ->
                                db.sectionDao().getByRole(r.id).forEach { sec ->
                                    ss.put(JSONObject().put("uid", sec.uid).put("sourceUid", sec.sourceUid)
                                        .put("title", sec.title).put("content", sec.content).put("orderIndex", sec.orderIndex))
                                }
                            }))
                        }
                    }))
            }
        }))
    }
    root.put("fields", fields)
    return root.toString(2)
}

/** DOCX ساده و استاندارد OOXML؛ متن فارسی راست‌به‌چپ و بدون وابستگی به کتابخانه حجیم. */
suspend fun exportContentDocx(db: AppDatabase, output: OutputStream) {
    val xml = StringBuilder()
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
    xml.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>")
    fun paragraph(text: String, style: String = "Normal") {
        xml.append("<w:p><w:pPr><w:pStyle w:val=\"").append(style).append("\"/><w:bidi/></w:pPr><w:r><w:rPr><w:rtl/></w:rPr><w:t xml:space=\"preserve\">")
        xml.append(escapeXml(text)).append("</w:t></w:r></w:p>")
    }
    paragraph("مجموعه تعزیه‌ها", "Title")
    db.fieldDao().getAll().forEach { field ->
        paragraph(field.title, "Heading1")
        db.taziehDao().getByField(field.id).forEach { t ->
            paragraph(t.title, "Heading2")
            db.roleDao().getByTazieh(t.id).forEach { role ->
                paragraph("نقش: ${role.title}", "Heading3")
                db.sectionDao().getByRole(role.id).forEach { section ->
                    if (section.title.isNotBlank()) paragraph(section.title, "Heading4")
                    paragraph(section.content)
                }
            }
        }
    }
    xml.append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/><w:pgMar w:top=\"1134\" w:right=\"1134\" w:bottom=\"1134\" w:left=\"1134\"/></w:sectPr></w:body></w:document>")
    ZipOutputStream(output).use { zip ->
        fun entry(name: String, content: String) {
            zip.putNextEntry(ZipEntry(name)); zip.write(content.toByteArray(Charsets.UTF_8)); zip.closeEntry()
        }
        entry("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/></Types>""")
        entry("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>""")
        entry("word/document.xml", xml.toString())
        entry("word/styles.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/></w:style><w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:rPr><w:b/><w:sz w:val="36"/></w:rPr></w:style><w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:rPr><w:b/><w:sz w:val="32"/></w:rPr></w:style><w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:rPr><w:b/><w:sz w:val="28"/></w:rPr></w:style><w:style w:type="paragraph" w:styleId="Heading3"><w:name w:val="heading 3"/><w:rPr><w:b/><w:sz w:val="24"/></w:rPr></w:style><w:style w:type="paragraph" w:styleId="Heading4"><w:name w:val="heading 4"/><w:rPr><w:b/><w:sz w:val="22"/></w:rPr></w:style></w:styles>""")
    }
}

private fun escapeXml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;")
    .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
