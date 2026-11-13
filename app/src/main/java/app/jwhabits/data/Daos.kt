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
