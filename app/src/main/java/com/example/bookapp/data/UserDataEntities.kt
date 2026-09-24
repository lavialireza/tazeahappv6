package com.example.bookapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks", indices = [Index(value = ["sectionUid"], unique = true)])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionUid: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "section_tags", indices = [Index(value = ["sectionUid"], unique = true)])
data class SectionTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionUid: String,
    val tag: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_sections", indices = [Index(value = ["sectionUid"], unique = true), Index("position")])
data class RecentSectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionUid: String,
    val position: Int,
    val visitedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_history", indices = [Index("sectionUid"), Index("lastReadAt")])
data class ReadingHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionUid: String,
    val firstReadAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long = System.currentTimeMillis(),
    val readCount: Int = 1,
    val totalSeconds: Long = 0
)

@Entity(tableName = "my_roles", indices = [Index(value = ["taziehUid"], unique = true)])
data class MyRoleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taziehUid: String,
    val roleUid: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "active_days", indices = [Index(value = ["dayKey"], unique = true)])
data class ActiveDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayKey: String
)
