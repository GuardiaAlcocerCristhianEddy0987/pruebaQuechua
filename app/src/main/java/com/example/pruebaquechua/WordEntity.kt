package com.example.pruebaquechua

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diccionario")
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val category: String,
    val type: String = "Sustantivo", // Verbo, Sustantivo, Participio, Sujeto, etc.
    val definition: String,
    val audioResId: Int? = null,
    val audioPath: String? = null // Para audios grabados localmente
)
