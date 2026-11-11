package com.manito.backlogquest.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

enum class GameStatus(val label: String) {
    BACKLOG("Pendiente"),
    PLAYING("Jugando"),
    PAUSED("Pausado"),
    REPLAY("Rejugar"),
    ONGOING("Sin final"),
    COMPLETED("Terminado"),
    ABANDONED("Abandonado"),
    WISHLIST("Wishlist")
}

class GameStatusConverter {
    @TypeConverter
    fun fromStatus(value: GameStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): GameStatus = GameStatus.valueOf(value)
}

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val platform: String,
    val genre: String = "",
    val status: GameStatus = GameStatus.BACKLOG,
    val priority: Int = 3,
    val estimatedHours: Double? = null,
    val playedMinutes: Int = 0,
    val progressPercent: Int = 0,
    val notes: String = "",
    val imageUrl: String? = null,
    val platinum: Boolean = false,
    val allAchievements: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(
    tableName = "play_sessions",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gameId")]
)
data class PlaySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val durationMinutes: Int,
    val progressAfter: Int,
    val notes: String = "",
    val playedAt: Long = System.currentTimeMillis()
)

@Dao
interface GameDao {
    @Query(
        """
        SELECT * FROM games
        ORDER BY
            CASE status
                WHEN 'PLAYING' THEN 0
                WHEN 'BACKLOG' THEN 1
                WHEN 'PAUSED' THEN 2
                WHEN 'REPLAY' THEN 3
                WHEN 'ONGOING' THEN 4
                WHEN 'COMPLETED' THEN 5
                WHEN 'ABANDONED' THEN 6
                ELSE 7
            END,
            priority DESC,
            updatedAt DESC
        """
    )
    fun observeGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :gameId LIMIT 1")
    fun observeGame(gameId: Long): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE id = :gameId LIMIT 1")
    suspend fun getGame(gameId: Long): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity): Long

    @Update
    suspend fun updateGame(game: GameEntity)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Query("SELECT * FROM play_sessions WHERE gameId = :gameId ORDER BY playedAt DESC")
    fun observeSessions(gameId: Long): Flow<List<PlaySessionEntity>>

    @Insert
    suspend fun insertSession(session: PlaySessionEntity)

    @Query("SELECT * FROM games ORDER BY id")
    suspend fun getAllGames(): List<GameEntity>

    @Query("SELECT * FROM play_sessions ORDER BY id")
    suspend fun getAllSessions(): List<PlaySessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<GameEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<PlaySessionEntity>)

    @Query("DELETE FROM play_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM games")
    suspend fun deleteAllGames()

    @Transaction
    suspend fun replaceAllData(
        games: List<GameEntity>,
        sessions: List<PlaySessionEntity>
    ) {
        deleteAllSessions()
        deleteAllGames()
        insertGames(games)
        insertSessions(sessions)
    }
}

@Database(
    entities = [GameEntity::class, PlaySessionEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(GameStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun create(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "backlog-quest.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE games ADD COLUMN imageUrl TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE games ADD COLUMN platinum INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE games ADD COLUMN allAchievements " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "UPDATE games SET allAchievements = platinum, platinum = 0 " +
                        "WHERE platinum = 1 AND (" +
                        "LOWER(platform) LIKE '%steam%' OR " +
                        "LOWER(platform) LIKE '%pc%' OR " +
                        "LOWER(platform) LIKE '%windows%')"
                )
            }
        }
    }
}
