package app.jwhabits.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MaterialDao {
    @Query("SELECT * FROM material WHERE id = :id")
    fun observe(id: Long): Flow<MaterialEntity?>

    @Query("SELECT * FROM material WHERE id = :id")
    suspend fun get(id: Long): MaterialEntity?

    @Query("SELECT * FROM material WHERE habitId = :habitId AND archived = 0 ORDER BY sortOrder, id")
    fun observeByHabit(habitId: Long): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM material WHERE habitId = :habitId AND archived = 0 ORDER BY sortOrder, id")
    suspend fun getByHabit(habitId: Long): List<MaterialEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM material WHERE habitId = :habitId")
    suspend fun maxSortOrder(habitId: Long): Int

    @Insert
    suspend fun insert(material: MaterialEntity): Long

    @Update
    suspend fun update(material: MaterialEntity)
}

@Dao
interface UnitRecordDao {
    @Query("SELECT * FROM unit_record WHERE materialId = :materialId AND round = :round")
    fun observeRound(materialId: Long, round: Int): Flow<List<UnitRecordEntity>>

    @Query("SELECT * FROM unit_record WHERE materialId = :materialId AND round = :round")
    suspend fun getRound(materialId: Long, round: Int): List<UnitRecordEntity>

    @Query("SELECT * FROM unit_record WHERE materialId = :materialId AND round = :round AND unitIndex = :unitIndex")
    suspend fun get(materialId: Long, round: Int, unitIndex: Int): UnitRecordEntity?

    @Query("SELECT COUNT(*) FROM unit_record WHERE materialId = :materialId AND date = :date")
    fun observeCountOn(materialId: Long, date: LocalDate): Flow<Int>

    @Query("SELECT * FROM unit_record WHERE id = :id")
    suspend fun getById(id: Long): UnitRecordEntity?

    @Query("SELECT MAX(unitIndex) FROM unit_record WHERE materialId = :materialId AND round = :round")
    suspend fun maxIndex(materialId: Long, round: Int): Int?

    /** 習慣ごとに、各資料の「今の周」で記録した単位の数。 */
    @Query(
        "SELECT u.materialId AS materialId, COUNT(*) AS count FROM unit_record u " +
            "JOIN material m ON m.id = u.materialId " +
            "WHERE m.habitId = :habitId AND u.round = m.round GROUP BY u.materialId"
    )
    fun observeCurrentCounts(habitId: Long): Flow<List<MaterialCount>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<UnitRecordEntity>): List<Long>

    @Update
    suspend fun update(record: UnitRecordEntity)

    @Query("DELETE FROM unit_record WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}

@Dao
interface HabitDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(habits: List<HabitEntity>)

    @Query("SELECT * FROM habit ORDER BY sortOrder")
    fun observeAll(): Flow<List<HabitEntity>>
}

data class MaterialCount(val materialId: Long, val count: Int)

@Dao
interface DayRecordDao {
    /** すでにあれば -1 を返す（入れ直さない）。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: DayRecordEntity): Long

    @Query("DELETE FROM day_record WHERE habitId = :habitId AND date = :date")
    suspend fun delete(habitId: Long, date: LocalDate)

    @Query("SELECT EXISTS(SELECT 1 FROM day_record WHERE habitId = :habitId AND date = :date)")
    fun observeExists(habitId: Long, date: LocalDate): Flow<Boolean>
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM service_record WHERE date BETWEEN :from AND :to ORDER BY date DESC, COALESCE(startEpochMs, 0) DESC, id DESC")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<ServiceRecordEntity>>

    @Query("SELECT * FROM service_record WHERE startEpochMs IS NOT NULL AND endEpochMs IS NULL AND minutes IS NULL LIMIT 1")
    fun observeRunning(): Flow<ServiceRecordEntity?>

    @Query("SELECT * FROM service_record WHERE startEpochMs IS NOT NULL AND endEpochMs IS NULL AND minutes IS NULL LIMIT 1")
    suspend fun getRunning(): ServiceRecordEntity?

    /** id が 0 なら新しく、あればその id で入れ直す（元に戻す用）。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ServiceRecordEntity): Long

    @Update
    suspend fun update(record: ServiceRecordEntity)

    @Query("DELETE FROM service_record WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM service_month WHERE yearMonth = :ym")
    fun observeMonth(ym: String): Flow<ServiceMonthEntity?>

    @Query("SELECT * FROM service_month WHERE yearMonth = :ym")
    suspend fun getMonth(ym: String): ServiceMonthEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMonth(month: ServiceMonthEntity)
}
