package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notifications")
data class DbNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val category: String, // "SISTEMA", "TRANSAÇÃO", "MERCADO", "PLANILHA"
    val date: String,
    val iconName: String, // "WALLET", "TRENDING", "UPDATE", "FILE"
    val isRead: Boolean = false
)

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY id DESC")
    fun getAllNotifications(): Flow<List<DbNotification>>

    @Query("SELECT * FROM notifications ORDER BY id DESC")
    suspend fun getAllNotificationsSync(): List<DbNotification>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: DbNotification)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<DbNotification>)

    @Update
    suspend fun updateNotification(notification: DbNotification)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}

@Entity(tableName = "changelogs")
data class DbChangelog(
    @PrimaryKey val versionName: String,
    val releaseNotes: String, // store split by \n
    val date: String = "Sincronizado"
)

@Dao
interface ChangelogDao {
    @Query("SELECT * FROM changelogs ORDER BY versionName DESC")
    fun getAllChangelogs(): Flow<List<DbChangelog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChangelog(changelog: DbChangelog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(changelogs: List<DbChangelog>)
    
    @Query("DELETE FROM changelogs")
    suspend fun deleteAll()
}
