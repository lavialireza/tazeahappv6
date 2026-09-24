package com.example.bookapp.data

/** Detailed, read-only content quality report. It never changes the database. */
data class ContentHealthReport(
    val warnings: List<String>,
    val errors: List<String>,
    val checkedFields: Int,
    val checkedTaziehs: Int,
    val checkedRoles: Int,
    val checkedSections: Int,
    val emptyTitles: Int,
    val emptyTexts: Int,
    val duplicateTitles: Int,
    val orderIssues: Int,
    val invalidAudio: Int
) {
    val totalIssues: Int get() = warnings.size + errors.size
}

suspend fun buildDetailedContentHealthReport(db: AppDatabase): ContentHealthReport {
    val warnings = mutableListOf<String>()
    val errors = mutableListOf<String>()
    var emptyTitles = 0
    var emptyTexts = 0
    var duplicateTitles = 0
    var orderIssues = 0
    var invalidAudio = 0

    val fields = db.fieldDao().getAll()
    val taziehs = db.taziehDao().getAll()
    val sections = db.sectionDao().getAll()
    val roles = taziehs.flatMap { db.roleDao().getByTazieh(it.id) }

    fun duplicateCount(values: List<String>): Int =
        values.groupingBy { it.trim() }.eachCount().filter { it.key.isNotEmpty() && it.value > 1 }.values.sumOf { it - 1 }

    duplicateTitles += duplicateCount(fields.map { it.title })
    if (duplicateCount(fields.map { it.title }) > 0) warnings += "چند زمینه با عنوان یکسان وجود دارد."

    for (field in fields) {
        if (field.title.isBlank()) { emptyTitles++; errors += "زمینه بدون عنوان (شناسه داخلی ${field.id})" }
        val children = taziehs.filter { it.fieldId == field.id }
        val d = duplicateCount(children.map { it.title })
        duplicateTitles += d
        if (d > 0) warnings += "در زمینه «${field.title}» تعزیه‌های تکراری وجود دارد."
    }

    for (tazieh in taziehs) {
        if (tazieh.title.isBlank()) { emptyTitles++; errors += "تعزیه بدون عنوان (شناسه داخلی ${tazieh.id})" }
        val children = roles.filter { it.taziehId == tazieh.id }
        val d = duplicateCount(children.map { it.title })
        duplicateTitles += d
        if (d > 0) warnings += "در تعزیه «${tazieh.title}» نقش‌های تکراری وجود دارد."

        for (role in children) {
            if (role.title.isBlank()) { emptyTitles++; errors += "نقش بدون عنوان در «${tazieh.title}»" }
            val roleSections = sections.filter { it.roleId == role.id }.sortedWith(compareBy<SectionEntity> { it.orderIndex }.thenBy { it.id })
            val sd = duplicateCount(roleSections.map { it.title })
            duplicateTitles += sd
            if (sd > 0) warnings += "در نقش «${role.title}» بخش‌های تکراری وجود دارد."
            val expected = roleSections.indices.toList()
            if (roleSections.map { it.orderIndex } != expected) {
                orderIssues++
                warnings += "ترتیب بخش‌های نقش «${role.title}» پیوسته نیست."
            }
            for (section in roleSections) {
                if (section.title.isBlank()) { emptyTitles++; errors += "بخش بدون عنوان در نقش «${role.title}»" }
                if (section.content.isBlank()) { emptyTexts++; warnings += "بخش «${section.title.ifBlank { "بدون عنوان" }}» متن ندارد." }
                val audio = section.audioUrl?.trim().orEmpty()
                if (audio.isNotEmpty() && !audio.startsWith("https://") && !audio.startsWith("/") && !audio.startsWith("content://")) {
                    invalidAudio++
                    warnings += "آدرس صوت بخش «${section.title.ifBlank { "بدون عنوان" }}» قالب شناخته‌شده‌ای ندارد."
                }
            }
        }
    }

    for (tazieh in taziehs) {
        val taziehRoles = roles.filter { it.taziehId == tazieh.id }.sortedWith(compareBy<RoleEntity> { it.orderIndex }.thenBy { it.id })
        if (taziehRoles.map { it.orderIndex } != taziehRoles.indices.toList()) {
            orderIssues++
            warnings += "ترتیب نقش‌های تعزیه «${tazieh.title}» پیوسته نیست."
        }
    }

    if (duplicateTitles > 0) warnings += "در مجموع $duplicateTitles عنوان تکراری در سطح والد مربوطه شناسایی شد."
    if (emptyTitles > 0) warnings += "$emptyTitles مورد بدون عنوان شناسایی شد."
    if (emptyTexts > 0) warnings += "$emptyTexts بخش بدون متن شناسایی شد."
    if (orderIssues > 0) warnings += "$orderIssues مورد مشکل ترتیب شناسایی شد."
    if (invalidAudio > 0) warnings += "$invalidAudio آدرس صوت نیازمند بررسی است."

    return ContentHealthReport(
        warnings = warnings.distinct(),
        errors = errors.distinct(),
        checkedFields = fields.size,
        checkedTaziehs = taziehs.size,
        checkedRoles = roles.size,
        checkedSections = sections.size,
        emptyTitles = emptyTitles,
        emptyTexts = emptyTexts,
        duplicateTitles = duplicateTitles,
        orderIssues = orderIssues,
        invalidAudio = invalidAudio
    )
}


fun ContentHealthReport.toPersianText(): String = buildString {
    appendLine("گزارش سلامت محتوا")
    appendLine("====================")
    appendLine("زمینه‌ها: $checkedFields")
    appendLine("تعزیه‌ها: $checkedTaziehs")
    appendLine("نقش‌ها: $checkedRoles")
    appendLine("بخش‌ها: $checkedSections")
    appendLine("عنوان‌های خالی: $emptyTitles")
    appendLine("متن‌های خالی: $emptyTexts")
    appendLine("عنوان‌های تکراری: $duplicateTitles")
    appendLine("مشکلات ترتیب: $orderIssues")
    appendLine("صوت‌های مشکوک: $invalidAudio")
    appendLine()
    if (warnings.isEmpty()) appendLine("نتیجه: موردی برای بررسی یافت نشد.")
    else { appendLine("موارد نیازمند بررسی:"); warnings.forEachIndexed { i, w -> appendLine("${i + 1}. $w") } }
}
