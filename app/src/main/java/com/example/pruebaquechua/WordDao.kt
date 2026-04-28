package com.example.pruebaquechua

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WordDao {
    @Query("SELECT * FROM diccionario WHERE category = :category")
    suspend fun getWordsByCategory(category: String): List<WordEntity>

    @Query("SELECT * FROM diccionario WHERE remoteId IS NULL")
    suspend fun getLocalOnlyWords(): List<WordEntity>

    @Query("SELECT * FROM diccionario WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getWordByRemoteId(remoteId: String): WordEntity?

    @Query("SELECT * FROM diccionario WHERE word = :word AND definition = :definition LIMIT 1")
    suspend fun findWordByContent(word: String, definition: String): WordEntity?

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insert(word: WordEntity): Long

    @androidx.room.Update
    suspend fun update(word: WordEntity)

    @androidx.room.Delete
    suspend fun delete(word: WordEntity)

    @Insert
    suspend fun insertAll(words: List<WordEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM diccionario")
    suspend fun getCount(): Int
}
