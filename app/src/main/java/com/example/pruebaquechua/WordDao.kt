package com.example.pruebaquechua

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WordDao {
    @Query("SELECT * FROM diccionario WHERE category = :category")
    suspend fun getWordsByCategory(category: String): List<WordEntity>

    @Insert
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
