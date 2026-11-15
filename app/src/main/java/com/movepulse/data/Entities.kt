package com.movepulse.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import android.content.Context
import kotlinx.coroutines.flow.Flow

/** One row per day+hour. Powers the daily analysis charts. */
@Entity(tableName = "hour_stats", primaryKeys = ["date", "hour"])
data class HourStat(
    val date: String,          // yyyy-MM-dd
    val hour: Int,             // 0..23
    val steps: Int = 0,
    val calories: Float = 0f,
    val walkSeconds: Int = 0,
    val jogSeconds: Int = 0,
    val runSeconds: Int = 0,
    val moveSeconds: Int = 0,  // movement without steps (dancing, chores, exercise)
) {
    val activeSeconds: Int get() = walkSeconds + jogSeconds + runSeconds + moveSeconds
}

/** GPS points of walks/jogs/runs, drawn on the map. */
@Entity(tableName = "route_points")
data class RoutePoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val ts: Long,
    val lat: Double,
    val lon: Double,
)

@Dao
interface StatsDao {
    @Query("SELECT * FROM hour_stats WHERE date = :date ORDER BY hour")
    fun hoursForDay(date: String): Flow<List<HourStat>>

    @Query("SELECT * FROM hour_stats WHERE date >= :from ORDER BY date, hour")
    fun since(from: String): Flow<List<HourStat>>

    @Query("SELECT * FROM hour_stats WHERE date = :date AND hour = :hour")
    suspend fun get(date: String, hour: Int): HourStat?

    @Upsert
    suspend fun upsert(stat: HourStat)

    @Query("SELECT COALESCE(SUM(steps), 0) FROM hour_stats WHERE date = :date")
    suspend fun totalSteps(date: String): Int

    @Query("SELECT COALESCE(SUM(calories), 0) FROM hour_stats WHERE date = :date")
    suspend fun totalCalories(date: String): Float

    @Insert
    suspend fun insertPoint(point: RoutePoint)

    @Query("SELECT * FROM route_points WHERE date = :date ORDER BY ts")
    fun routeFor(date: String): Flow<List<RoutePoint>>
}

@Database(entities = [HourStat::class, RoutePoint::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "movepulse.db"
            ).build().also { instance = it }
        }
    }
}
