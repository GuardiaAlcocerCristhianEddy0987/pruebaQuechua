package com.example.pruebaquechua

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [WordEntity::class, UserEntity::class], version = 5)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quechua_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                // Al subir la versión y usar fallbackToDestructiveMigration, la base se limpia sola
                                if (database.userDao().getCount() == 0) {
                                    populateUsers(database.userDao())
                                }
                                if (database.wordDao().getCount() == 0) {
                                    populateDatabase(database.wordDao())
                                }
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateUsers(userDao: UserDao) {
            userDao.insert(UserEntity(username = "admin", password = "123", role = "ADMIN"))
            userDao.insert(UserEntity(username = "user", password = "123", role = "USER"))
        }

        private suspend fun populateDatabase(wordDao: WordDao) {
            val words = listOf(
                // Educación
                WordEntity(word = "Phukuy", category = "Educación", type = "Verbo", definition = "Soplar (Aspirada)."),
                WordEntity(word = "T'anta", category = "Educación", type = "Sustantivo", definition = "Pan (Explosiva)."),
                WordEntity(word = "Thupa", category = "Educación", type = "Sustantivo", definition = "Lima o instrumento para alisar (Aspirada)."),
                WordEntity(word = "Ch'usaq", category = "Educación", type = "Sustantivo", definition = "Vacío o número cero (Explosiva)."),
                
                // Tecnología
                WordEntity(word = "Qhaway", category = "Tecnología", type = "Verbo", definition = "Mirar u observar pantallas (Aspirada)."),
                WordEntity(word = "K'anchay", category = "Tecnología", type = "Sustantivo", definition = "Luz o brillo de dispositivos (Explosiva)."),
                WordEntity(word = "Khuskuchay", category = "Tecnología", type = "Verbo", definition = "Sincronizar u equilibrar datos (Aspirada)."),
                WordEntity(word = "Ch'iwi", category = "Tecnología", type = "Sustantivo", definition = "Señal o frecuencia de radio/internet (Explosiva)."),
                
                // Medicina
                WordEntity(word = "Qhali", category = "Medicina", type = "Sustantivo", definition = "Persona sana o con salud (Aspirada)."),
                WordEntity(word = "K'iri", category = "Medicina", type = "Sustantivo", definition = "Herida o lesión física (Explosiva)."),
                WordEntity(word = "Thani", category = "Medicina", type = "Sustantivo", definition = "Estado de recuperación o sanado (Aspirada)."),
                WordEntity(word = "P'itiy", category = "Medicina", type = "Verbo", definition = "Romper o fractura de hueso (Explosiva)."),
                
                // Biología
                WordEntity(word = "Kawsaykamay", category = "Biología", type = "Sustantivo", definition = "Biología (Estudio de la vida)."),
                WordEntity(word = "Kawsaykamayuq", category = "Biología", type = "Sustantivo", definition = "Biólogo (Profesional de la biología)."),
                WordEntity(word = "Ch'inikawsaykamay", category = "Biología", type = "Sustantivo", definition = "Microbiología."),
                WordEntity(word = "Ch'iñikawsaq", category = "Biología", type = "Sustantivo", definition = "Microorganismo."),
                WordEntity(word = "Ch'iñillku", category = "Biología", type = "Sustantivo", definition = "Bacteria."),
                WordEntity(word = "Ukhukamay", category = "Biología", type = "Sustantivo", definition = "Anatomía (Estudio del cuerpo)."),
                WordEntity(word = "Mirachillikha", category = "Biología", type = "Sustantivo", definition = "Sistema reproductor."),
                WordEntity(word = "Kawsay", category = "Biología", type = "Sustantivo", definition = "Vida.")
            )
            wordDao.insertAll(words)
        }
    }
}
