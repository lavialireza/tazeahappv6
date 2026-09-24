package com.example.bookapp.data

import android.content.Context
import androidx.room.withTransaction
import java.util.Calendar

/** Bridges the old SharedPreferences user data into Room while the UI migrates gradually. */
suspend fun migratePrefsUserDataToRoom(context: Context, db: AppDatabase) {
    db.withTransaction {
        val sections = db.sectionDao().getAll()
        val byId = sections.associateBy { it.id }
        val dao = db.userDataDao()

        dao.deleteAllBookmarks()
        Prefs.getBookmarks(context).forEach { id ->
            byId[id]?.let { section ->
                dao.insertBookmark(BookmarkEntity(sectionUid = section.uid))
            }
        }

        dao.deleteAllTags()
        Prefs.getAllTags(context).forEach { (id, tag) ->
            byId[id]?.let { section ->
                dao.insertTag(SectionTagEntity(sectionUid = section.uid, tag = tag))
            }
        }

        dao.deleteAllRecent()
        Prefs.getRecent(context).forEachIndexed { index, id ->
            byId[id]?.let { section ->
                dao.insertRecent(RecentSectionEntity(sectionUid = section.uid, position = index))
            }
        }

        dao.deleteAllReadingHistory()
        Prefs.getReadSectionIds(context).forEach { id ->
            byId[id]?.let { section ->
                dao.insertReading(ReadingHistoryEntity(sectionUid = section.uid))
            }
        }

        dao.deleteAllActiveDays()
        Prefs.getActiveDayValues(context).forEach { day ->
            dao.insertActiveDay(ActiveDayEntity(dayKey = day))
        }

        dao.deleteAllMyRoles()
        val taziehs = db.taziehDao().getAll().associateBy { it.id }
        Prefs.getAllMyRoles(context).forEach { (taziehId, roleId) ->
            val tazieh = taziehs[taziehId]
            val role = try { db.roleDao().getById(roleId) } catch (_: Exception) { null }
            if (tazieh != null && role != null) {
                dao.insertMyRole(MyRoleEntity(taziehUid = tazieh.uid, roleUid = role.uid))
            }
        }
    }
}

fun localDayKeyString(): String {
    val c = Calendar.getInstance()
    return "%04d-%02d-%02d".format(
        c.get(Calendar.YEAR),
        c.get(Calendar.MONTH) + 1,
        c.get(Calendar.DAY_OF_MONTH)
    )
}
