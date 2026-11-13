package app.jwhabits.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

class Converters {
    @TypeConverter fun dateToString(d: LocalDate?): String? = d?.toString()
    @TypeConverter fun stringToDate(s: String?): LocalDate? = s?.let(LocalDate::parse)
}

@Database(
    entities = [
        HabitEntity::class, MaterialEntity::class, UnitRecordEntity::class,
        DayRecordEntity::class, ServiceRecordEntity::class, ServiceMonthEntity::class,
    ],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun materialDao(): MaterialDao
    abstract fun unitRecordDao(): UnitRecordDao
    abstract fun dayRecordDao(): DayRecordDao
    abstract fun serviceDao(): ServiceDao

    companion object {
        const val VERSION = 2
        const val HABIT_BIBLE_READING = 1L
        const val HABIT_STUDY = 2L
        const val HABIT_SERVICE = 3L
        const val MATERIAL_BIBLE_READING = 1L

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE material ADD COLUMN issue TEXT")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "jw-habits.db")
                .addCallback(Seed)
                .addMigrations(MIGRATION_1_2)
                .build()
    }

    /** 初回だけ、3つの習慣と聖書通読の資料を入れる。 */
    private object Seed : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT INTO habit (id, name, kind, sortOrder) VALUES " +
                    "($HABIT_BIBLE_READING, '聖書通読', 'CHAPTER_GRID', 0), " +
                    "($HABIT_STUDY, '個人研究', 'MATERIAL', 1), " +
                    "($HABIT_SERVICE, '奉仕', 'TIME', 2)"
            )
            db.execSQL(
                "INSERT INTO material (id, habitId, title, unitLabel, unitCount, template, round, sortOrder, archived) " +
                    "VALUES ($MATERIAL_BIBLE_READING, $HABIT_BIBLE_READING, '聖書通読', '章', " +
                    "${app.jwhabits.domain.BibleBooks.totalChapters}, 'BIBLE_READING', 1, 0, 0)"
            )
        }
    }
}
