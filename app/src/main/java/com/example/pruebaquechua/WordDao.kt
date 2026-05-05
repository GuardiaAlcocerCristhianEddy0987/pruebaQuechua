package com.example.pruebaquechua

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface WordDao {
    @Query("SELECT * FROM diccionario WHERE category = :category ORDER BY definition ASC")
    suspend fun getWordsByCategory(category: String): List<WordEntity>

    @Query("SELECT * FROM diccionario ORDER BY category ASC, definition ASC")
    suspend fun getAllWords(): List<WordEntity>

    @Query("SELECT * FROM diccionario WHERE remoteId IS NULL")
    suspend fun getLocalOnlyWords(): List<WordEntity>

    @Query("SELECT * FROM diccionario WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getWordByRemoteId(remoteId: String): WordEntity?

    @Query("SELECT * FROM diccionario WHERE word = :word AND definition = :definition LIMIT 1")
    suspend fun findWordByContent(word: String, definition: String): WordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: WordEntity): Long

    @Update
    suspend fun update(word: WordEntity)

    @Delete
    suspend fun delete(word: WordEntity)

    @Insert
    suspend fun insertAll(words: List<WordEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM diccionario")
    suspend fun getCount(): Int

    @Query("SELECT * FROM diccionario WHERE category = :category AND (word LIKE '%' || :searchQuery || '%' OR definition LIKE '%' || :searchQuery || '%') ORDER BY definition ASC")
    suspend fun searchWords(category: String, searchQuery: String): List<WordEntity>

    @Query("SELECT * FROM diccionario WHERE word LIKE '%' || :searchQuery || '%' OR definition LIKE '%' || :searchQuery || '%' ORDER BY category ASC, definition ASC")
    suspend fun searchAllWords(searchQuery: String): List<WordEntity>
}
