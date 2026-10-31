package com.autohub.park.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY isDefault DESC, createdAt ASC")
    fun observeAll(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: Bookmark): Long

    @Query("DELETE FROM bookmarks WHERE id = :id AND isDefault = 0")
    suspend fun deleteUser(id: Long)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bookmarks WHERE isDefault = 0")
    suspend fun deleteAllUserBookmarks()

    @Query("UPDATE bookmarks SET title = :newTitle WHERE id = :id")
    suspend fun updateTitle(id: Long, newTitle: String)
}

