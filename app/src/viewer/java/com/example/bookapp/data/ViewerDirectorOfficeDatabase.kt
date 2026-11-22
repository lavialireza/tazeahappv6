package com.example.bookapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * پایگاه داده مستقل دفتر معین‌البکای Viewer.
 * فایل رسمی فقط برای خواندن داده‌های رسمی محلیِ باقی‌مانده از نسخه‌های قبلی نگه داشته شده است؛
 * در معماری فعلی مسیر دریافت/واردکردن دفتر وجود ندارد.
 * فایل شخصی محل داده‌های قابل ویرایش همین نصب است و از کتابخانه محلی ساخته می‌شود.
 * هیچ‌کدام bookapp.db ادمین نیستند.
 */
@Database(
    entities = [
        DirectorPlanEntity::class,
        DirectorPlanVersionEntity::class,
        DirectorSourceFileEntity::class,
        DirectorTurnEntity::class,
        DirectorParticipantEntity::class,
        DirectorTextSegmentEntity::class,
        DirectorChangeHistoryEntity::class,
        DirectorStageCueEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ViewerDirectorOfficeDatabase : RoomDatabase() {
    abstract fun directorDao(): ViewerDirectorOfficeDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `director_text_segments` ADD COLUMN `sourceTaziehUid` TEXT")
                db.execSQL("ALTER TABLE `director_text_segments` ADD COLUMN `sourceRoleUid` TEXT")
                db.execSQL("ALTER TABLE `director_text_segments` ADD COLUMN `sourceSectionTitle` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `director_text_segments` ADD COLUMN `sourceSectionUids` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `director_text_segments` ADD COLUMN `sourceMethod` TEXT NOT NULL DEFAULT 'LEGACY'")
                db.execSQL("ALTER TABLE `director_text_segments` ADD COLUMN `sourceContentHash` TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE IF NOT EXISTS `director_change_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `planId` INTEGER NOT NULL, `versionId` INTEGER NOT NULL, `turnId` INTEGER, `entityType` TEXT NOT NULL, `fieldName` TEXT NOT NULL, `changeType` TEXT NOT NULL, `summary` TEXT NOT NULL, `oldValue` TEXT NOT NULL, `newValue` TEXT NOT NULL, `changedAt` INTEGER NOT NULL, `restoredFromId` INTEGER)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_director_change_history_planId` ON `director_change_history` (`planId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_director_change_history_versionId` ON `director_change_history` (`versionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_director_change_history_turnId_changedAt` ON `director_change_history` (`turnId`, `changedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_director_change_history_changedAt` ON `director_change_history` (`changedAt`)")
            }
        }

        @Volatile private var officialInstance: ViewerDirectorOfficeDatabase? = null
        @Volatile private var personalInstance: ViewerDirectorOfficeDatabase? = null

        fun official(context: Context): ViewerDirectorOfficeDatabase =
            officialInstance ?: synchronized(this) {
                officialInstance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ViewerDirectorOfficeDatabase::class.java,
                    "director_official.db"
                ).addMigrations(MIGRATION_1_2).addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onOpen(db)
                        installOfficialReadOnlyTriggers(db)
                    }
                }).build().also { officialInstance = it }
            }

        fun personal(context: Context): ViewerDirectorOfficeDatabase =
            personalInstance ?: synchronized(this) {
                personalInstance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ViewerDirectorOfficeDatabase::class.java,
                    "director_personal_${ViewerAccessPolicy.installationId(context).replace(Regex("[^A-Za-z0-9_-]"), "_")}.db"
                ).addMigrations(MIGRATION_1_2).addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onOpen(db)
                        installPersonalNoPublishTriggers(db)
                    }
                }).build().also { personalInstance = it }
            }

        private val officialTables = listOf(
            "director_plans", "director_plan_versions", "director_source_files", "director_turns",
            "director_turn_participants", "director_text_segments", "director_stage_cues", "director_change_history"
        )

        private fun installOfficialReadOnlyTriggers(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            officialTables.forEach { table ->
                db.execSQL("CREATE TRIGGER IF NOT EXISTS `ro_${table}_insert` BEFORE INSERT ON `$table` BEGIN SELECT RAISE(ABORT, 'official director office is read-only'); END")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS `ro_${table}_update` BEFORE UPDATE ON `$table` BEGIN SELECT RAISE(ABORT, 'official director office is read-only'); END")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS `ro_${table}_delete` BEFORE DELETE ON `$table` BEGIN SELECT RAISE(ABORT, 'official director office is read-only'); END")
            }
        }

        private fun installPersonalNoPublishTriggers(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("CREATE TRIGGER IF NOT EXISTS `personal_plan_no_publish_insert` BEFORE INSERT ON `director_plans` WHEN NEW.status = 'PUBLISHED' BEGIN SELECT RAISE(ABORT, 'personal director office cannot be published'); END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS `personal_plan_no_publish_update` BEFORE UPDATE OF status ON `director_plans` WHEN NEW.status = 'PUBLISHED' BEGIN SELECT RAISE(ABORT, 'personal director office cannot be published'); END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS `personal_version_no_publish_insert` BEFORE INSERT ON `director_plan_versions` WHEN NEW.status = 'PUBLISHED' BEGIN SELECT RAISE(ABORT, 'personal director office cannot be published'); END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS `personal_version_no_publish_update` BEFORE UPDATE OF status ON `director_plan_versions` WHEN NEW.status = 'PUBLISHED' BEGIN SELECT RAISE(ABORT, 'personal director office cannot be published'); END")
        }
    }
}
