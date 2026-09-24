package com.example.bookapp.data

/** حالت تطبیق عبارت جستجو. */
enum class SearchMatchMode {
    EXACT_PHRASE,
    ALL_WORDS,
    ANY_WORD
}

enum class SearchSortMode {
    RELEVANCE,
    TITLE,
    HIERARCHY
}

data class SearchCorpusRow(
    val sectionId: Long,
    val sectionTitle: String,
    val content: String,
    val roleTitle: String,
    val taziehTitle: String,
    val fieldTitle: String,
    val fieldId: Long,
    val taziehId: Long,
    val roleId: Long,
    val footnotesText: String?
)

data class AdvancedSearchResult(
    val sectionId: Long,
    val sectionTitle: String,
    val roleTitle: String,
    val taziehTitle: String,
    val fieldTitle: String,
    val snippet: String,
    val score: Int,
    val matchSource: String
)

data class AdvancedSearchOptions(
    val matchMode: SearchMatchMode = SearchMatchMode.ALL_WORDS,
    val fieldId: Long? = null,
    val taziehId: Long? = null,
    val roleId: Long? = null,
    val sectionId: Long? = null,
    val inSectionTitle: Boolean = true,
    val inText: Boolean = true,
    val inRole: Boolean = true,
    val inTazieh: Boolean = true,
    val inField: Boolean = true,
    val inFootnote: Boolean = true,
    val sortMode: SearchSortMode = SearchSortMode.RELEVANCE
)

/**
 * جستجوی فارسی را قبل از مقایسه یکدست می‌کند تا شکل‌های رایج «ی/ي»، «ک/ك»،
 * اعراب و نیم‌فاصله باعث از دست رفتن نتیجه نشوند.
 */
fun normalizeSearchText(value: String): String = value
    .lowercase()
    .replace('ي', 'ی')
    .replace('ى', 'ی')
    .replace('ك', 'ک')
    .replace('ۀ', 'ه')
    .replace('ة', 'ه')
    .replace('\u0640', ' ')
    .replace('\u200c', ' ')
    .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
    .replace(Regex("\\s+"), " ")
    .trim()

object AdvancedSearchEngine {
    fun search(
        corpus: List<SearchCorpusRow>,
        query: String,
        options: AdvancedSearchOptions,
        limit: Int = 200
    ): List<AdvancedSearchResult> {
        val normalizedQuery = normalizeSearchText(query)
        if (normalizedQuery.length < 2) return emptyList()
        val tokens = normalizedQuery.split(' ').filter { it.isNotBlank() }.distinct()
        if (tokens.isEmpty()) return emptyList()

        val filtered = corpus.asSequence()
            .filter { options.fieldId == null || it.fieldId == options.fieldId }
            .filter { options.taziehId == null || it.taziehId == options.taziehId }
            .filter { options.roleId == null || it.roleId == options.roleId }
            .filter { options.sectionId == null || it.sectionId == options.sectionId }
            .mapNotNull { row -> scoreRow(row, normalizedQuery, tokens, options.matchMode, options) }
            .toList()

        val sorted = when (options.sortMode) {
            SearchSortMode.TITLE -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.sectionTitle })
            SearchSortMode.HIERARCHY -> filtered.sortedWith(
                compareBy<AdvancedSearchResult> { it.fieldTitle }
                    .thenBy { it.taziehTitle }
                    .thenBy { it.roleTitle }
                    .thenBy { it.sectionTitle }
            )
            SearchSortMode.RELEVANCE -> filtered.sortedWith(
                compareByDescending<AdvancedSearchResult> { it.score }
                    .thenBy { it.fieldTitle }
                    .thenBy { it.taziehTitle }
                    .thenBy { it.roleTitle }
            )
        }
        return sorted.take(limit)
    }

    private fun scoreRow(
        row: SearchCorpusRow,
        query: String,
        tokens: List<String>,
        mode: SearchMatchMode,
        options: AdvancedSearchOptions
    ): AdvancedSearchResult? {
        val title = normalizeSearchText(row.sectionTitle)
        val content = normalizeSearchText(row.content)
        val role = normalizeSearchText(row.roleTitle)
        val tazieh = normalizeSearchText(row.taziehTitle)
        val field = normalizeSearchText(row.fieldTitle)
        val footnotes = normalizeSearchText(row.footnotesText.orEmpty())
        val scoped = buildList {
            if (options.inSectionTitle) add(title)
            if (options.inText) add(content)
            if (options.inRole) add(role)
            if (options.inTazieh) add(tazieh)
            if (options.inField) add(field)
            if (options.inFootnote) add(footnotes)
        }
        val allText = scoped.joinToString(" ")
        if (allText.isBlank()) return null

        val matches = when (mode) {
            SearchMatchMode.EXACT_PHRASE -> allText.contains(query)
            SearchMatchMode.ALL_WORDS -> tokens.all { allText.contains(it) }
            SearchMatchMode.ANY_WORD -> tokens.any { allText.contains(it) }
        }
        if (!matches) return null

        var score = 0
        if (options.inSectionTitle && title.contains(query)) score += 1200
        if (options.inText && content.contains(query)) score += 700
        if (options.inRole && role.contains(query)) score += 500
        if (options.inTazieh && tazieh.contains(query)) score += 450
        if (options.inField && field.contains(query)) score += 350
        if (options.inFootnote && footnotes.contains(query)) score += 250
        tokens.forEach { token ->
            if (options.inSectionTitle && title.contains(token)) score += 120
            if (options.inText && content.contains(token)) score += 40
            if (options.inRole && role.contains(token)) score += 60
            if (options.inTazieh && tazieh.contains(token)) score += 50
            if (options.inField && field.contains(token)) score += 35
            if (options.inFootnote && footnotes.contains(token)) score += 25
        }
        if (mode == SearchMatchMode.EXACT_PHRASE) score += 300

        val source = when {
            options.inSectionTitle && title.contains(query) -> "عنوان بخش"
            options.inText && content.contains(query) -> "متن"
            options.inFootnote && footnotes.contains(query) -> "پانویس"
            options.inRole && role.contains(query) -> "نقش"
            options.inTazieh && tazieh.contains(query) -> "تعزیه"
            options.inField && field.contains(query) -> "زمینه"
            else -> "متن انتخاب‌شده"
        }
        return AdvancedSearchResult(
            sectionId = row.sectionId,
            sectionTitle = row.sectionTitle,
            roleTitle = row.roleTitle,
            taziehTitle = row.taziehTitle,
            fieldTitle = row.fieldTitle,
            snippet = makeSnippet(row.content, query, tokens),
            score = score,
            matchSource = source
        )
    }

    private fun makeSnippet(content: String, query: String, tokens: List<String>): String {
        if (content.isBlank()) return ""
        val normalized = normalizeSearchText(content)
        val needle = if (normalized.contains(query)) query else tokens.firstOrNull { normalized.contains(it) }.orEmpty()
        val index = if (needle.isBlank()) 0 else normalized.indexOf(needle)
        if (index < 0) return content.take(150).replace('\n', ' ')
        val start = (index - 65).coerceAtLeast(0)
        val end = (index + needle.length + 90).coerceAtMost(content.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < content.length) "…" else ""
        return prefix + content.substring(start, end).replace('\n', ' ').replace(Regex("\\s+"), " ").trim() + suffix
    }
}
