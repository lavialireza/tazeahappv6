package com.example.bookapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "fields", indices = [Index(value = ["uid"], unique = true)])
data class FieldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(defaultValue = "''") val uid: String = ContentUid.new()
)

@Entity(
    tableName = "taziehs",
    foreignKeys = [ForeignKey(entity = FieldEntity::class, parentColumns = ["id"], childColumns = ["fieldId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("fieldId"), Index(value = ["uid"], unique = true)]
)
data class TaziehEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fieldId: Long,
    val title: String,
    val author: String? = null,
    val authorEmail: String? = null,
    @ColumnInfo(defaultValue = "''") val uid: String = ContentUid.new()
)

@Entity(
    tableName = "roles",
    foreignKeys = [ForeignKey(entity = TaziehEntity::class, parentColumns = ["id"], childColumns = ["taziehId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("taziehId"), Index(value = ["uid"], unique = true)]
)
data class RoleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taziehId: Long,
    val title: String,
    val orderIndex: Int = 0,
    @ColumnInfo(defaultValue = "''") val uid: String = ContentUid.new()
)

@Entity(
    tableName = "sections",
    foreignKeys = [ForeignKey(entity = RoleEntity::class, parentColumns = ["id"], childColumns = ["roleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("roleId"), Index(value = ["uid"], unique = true), Index(value = ["sourceUid"])]
)
data class SectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roleId: Long,
    val orderIndex: Int,
    val title: String,
    val content: String,
    val audioUrl: String? = null,
    @ColumnInfo(defaultValue = "''") val uid: String = ContentUid.new(),
    /** Stable identifier of the content file/source that owns this section. */
    @ColumnInfo(defaultValue = "''") val sourceUid: String = ""
)

@Entity(
    tableName = "footnotes",
    foreignKeys = [ForeignKey(entity = SectionEntity::class, parentColumns = ["id"], childColumns = ["sectionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sectionId"), Index(value = ["uid"], unique = true)]
)
data class FootnoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionId: Long,
    val term: String,
    val explanation: String,
    @ColumnInfo(defaultValue = "''") val uid: String = ContentUid.new()
)

@Entity(
    tableName = "dialogues",
    foreignKeys = [ForeignKey(entity = TaziehEntity::class, parentColumns = ["id"], childColumns = ["taziehId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("taziehId"), Index(value = ["uid"], unique = true)]
)
data class DialogueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taziehId: Long,
    val title: String,
    @ColumnInfo(defaultValue = "''") val uid: String = ContentUid.new()
)

@Entity(
    tableName = "dialogue_turns",
    foreignKeys = [
        ForeignKey(entity = DialogueEntity::class, parentColumns = ["id"], childColumns = ["dialogueId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SectionEntity::class, parentColumns = ["id"], childColumns = ["sectionId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("dialogueId"), Index("sectionId"), Index(value = ["uid"], unique = true)]
)
data class DialogueTurnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dialogueId: Long,
    val sectionId: Long,
    val orderIndex: Int,
    @ColumnInfo(defaultValue = "''") val uid: String = ContentUid.new()
)

@Entity(
    tableName = "tazieh_images",
    foreignKeys = [ForeignKey(entity = TaziehEntity::class, parentColumns = ["id"], childColumns = ["taziehId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("taziehId"), Index(value = ["uid"], unique = true)]
)
data class TaziehImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taziehId: Long,
    val filePath: String,
    val caption: String = "",
    @ColumnInfo(defaultValue = "''") val uid: String = ContentUid.new()
)
