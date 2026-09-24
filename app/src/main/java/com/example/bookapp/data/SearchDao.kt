package com.example.bookapp.data

import androidx.room.Dao
import androidx.room.Query

data class SearchResult(
    val sectionId: Long,
    val sectionTitle: String,
    val roleTitle: String,
    val taziehTitle: String,
    val fieldTitle: String
)

/**
 * جستجو با LIKE به‌جای FTS انجام می‌شود. تجربه‌ی واقعی نشان داد که
 * tokenizer های FTS4 (حتی unicode61) روی همه‌ی گوشی‌های اندروید پشتیبانی
 * نمی‌شوند و باعث می‌شد جستجوی متن فارسی گاهی هیچ نتیجه‌ای پیدا نکند.
 * LIKE کندتر است ولی صددرصد و روی هر گوشی‌ای درست کار می‌کند؛ برای حجم
 * محتوای این برنامه (چند صد تا چند هزار بخش) این سرعت کاملاً کافی است.
 */
@Dao
interface SearchDao {
    @Query(
        """
        SELECT
            sections.id AS sectionId,
            sections.title AS sectionTitle,
            roles.title AS roleTitle,
            taziehs.title AS taziehTitle,
            fields.title AS fieldTitle
        FROM sections
        INNER JOIN roles ON sections.roleId = roles.id
        INNER JOIN taziehs ON roles.taziehId = taziehs.id
        INNER JOIN fields ON taziehs.fieldId = fields.id
        WHERE sections.title LIKE '%' || :query || '%'
           OR sections.content LIKE '%' || :query || '%'
           OR roles.title LIKE '%' || :query || '%'
           OR taziehs.title LIKE '%' || :query || '%'

        UNION

        SELECT
            sections.id AS sectionId,
            sections.title AS sectionTitle,
            roles.title AS roleTitle,
            taziehs.title AS taziehTitle,
            fields.title AS fieldTitle
        FROM footnotes
        INNER JOIN sections ON footnotes.sectionId = sections.id
        INNER JOIN roles ON sections.roleId = roles.id
        INNER JOIN taziehs ON roles.taziehId = taziehs.id
        INNER JOIN fields ON taziehs.fieldId = fields.id
        WHERE footnotes.term LIKE '%' || :query || '%'
           OR footnotes.explanation LIKE '%' || :query || '%'

        ORDER BY fieldTitle, taziehTitle, roleTitle
        LIMIT 200
        """
    )
    suspend fun search(query: String): List<SearchResult>

    @Query(
        """
        SELECT
            sections.id AS sectionId,
            sections.title AS sectionTitle,
            roles.title AS roleTitle,
            taziehs.title AS taziehTitle,
            fields.title AS fieldTitle
        FROM sections
        INNER JOIN roles ON sections.roleId = roles.id
        INNER JOIN taziehs ON roles.taziehId = taziehs.id
        INNER JOIN fields ON taziehs.fieldId = fields.id
        WHERE fields.id = :fieldId
          AND (
            sections.title LIKE '%' || :query || '%'
            OR sections.content LIKE '%' || :query || '%'
            OR roles.title LIKE '%' || :query || '%'
            OR taziehs.title LIKE '%' || :query || '%'
          )
        ORDER BY taziehs.title, roles.title
        LIMIT 200
        """
    )
    suspend fun searchInField(query: String, fieldId: Long): List<SearchResult>

    @Query(
        """
        SELECT
            sections.id AS sectionId,
            sections.title AS sectionTitle,
            roles.title AS roleTitle,
            taziehs.title AS taziehTitle,
            fields.title AS fieldTitle
        FROM sections
        INNER JOIN roles ON sections.roleId = roles.id
        INNER JOIN taziehs ON roles.taziehId = taziehs.id
        INNER JOIN fields ON taziehs.fieldId = fields.id
        WHERE taziehs.id = :taziehId
          AND (
            sections.title LIKE '%' || :query || '%'
            OR sections.content LIKE '%' || :query || '%'
            OR roles.title LIKE '%' || :query || '%'
          )
        ORDER BY roles.title
        LIMIT 200
        """
    )
    suspend fun searchInTazieh(query: String, taziehId: Long): List<SearchResult>

    @Query(
        """
        SELECT
            sections.id AS sectionId,
            sections.title AS sectionTitle,
            roles.title AS roleTitle,
            taziehs.title AS taziehTitle,
            fields.title AS fieldTitle
        FROM sections
        INNER JOIN roles ON sections.roleId = roles.id
        INNER JOIN taziehs ON roles.taziehId = taziehs.id
        INNER JOIN fields ON taziehs.fieldId = fields.id
        WHERE sections.id IN (:ids)
        """
    )
    suspend fun getByIds(ids: List<Long>): List<SearchResult>

    @Query(
        """
        SELECT
            sections.id AS sectionId,
            sections.title AS sectionTitle,
            roles.title AS roleTitle,
            taziehs.title AS taziehTitle,
            fields.title AS fieldTitle
        FROM sections
        INNER JOIN roles ON sections.roleId = roles.id
        INNER JOIN taziehs ON roles.taziehId = taziehs.id
        INNER JOIN fields ON taziehs.fieldId = fields.id
        ORDER BY RANDOM()
        LIMIT 1
        """
    )
    suspend fun getRandomSection(): SearchResult?

    @Query("SELECT COUNT(*) FROM fields")
    suspend fun countFields(): Int

    @Query("SELECT COUNT(*) FROM taziehs")
    suspend fun countTaziehs(): Int

    @Query("SELECT COUNT(*) FROM roles")
    suspend fun countRoles(): Int

    @Query("SELECT COUNT(*) FROM sections")
    suspend fun countSections(): Int

    @Query(
        """
        SELECT
            sections.id AS sectionId,
            sections.title AS sectionTitle,
            roles.title AS roleTitle,
            taziehs.title AS taziehTitle,
            fields.title AS fieldTitle
        FROM sections
        INNER JOIN roles ON sections.roleId = roles.id
        INNER JOIN taziehs ON roles.taziehId = taziehs.id
        INNER JOIN fields ON taziehs.fieldId = fields.id
        WHERE sections.title = :sectionTitle AND sections.id != :excludeSectionId
        ORDER BY fields.title, taziehs.title, roles.title
        LIMIT 20
        """
    )
    suspend fun getRelatedByTitle(sectionTitle: String, excludeSectionId: Long): List<SearchResult>

    @Query(
        """
        SELECT
            sections.id AS sectionId,
            sections.title AS sectionTitle,
            sections.content AS content,
            roles.title AS roleTitle,
            taziehs.title AS taziehTitle,
            fields.title AS fieldTitle,
            fields.id AS fieldId,
            taziehs.id AS taziehId,
            roles.id AS roleId,
            GROUP_CONCAT(COALESCE(footnotes.term, '') || ' ' || COALESCE(footnotes.explanation, ''), ' ') AS footnotesText
        FROM sections
        INNER JOIN roles ON sections.roleId = roles.id
        INNER JOIN taziehs ON roles.taziehId = taziehs.id
        INNER JOIN fields ON taziehs.fieldId = fields.id
        LEFT JOIN footnotes ON footnotes.sectionId = sections.id
        GROUP BY sections.id, sections.title, sections.content, roles.title, taziehs.title, fields.title, fields.id, taziehs.id, roles.id
        """
    )
    suspend fun getSearchCorpus(): List<SearchCorpusRow>

    @Query(
        """
        SELECT dialogues.id AS dialogueId, dialogues.title AS dialogueTitle, taziehs.title AS taziehTitle
        FROM dialogues
        INNER JOIN taziehs ON dialogues.taziehId = taziehs.id
        WHERE dialogues.title LIKE '%' || :query || '%'
        LIMIT 50
        """
    )
    suspend fun searchDialogues(query: String): List<DialogueSearchResult>
}

data class DialogueSearchResult(
    val dialogueId: Long,
    val dialogueTitle: String,
    val taziehTitle: String
)
