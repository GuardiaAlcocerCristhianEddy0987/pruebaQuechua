package com.example.pruebaquechua

import androidx.room.*

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    fun getUserByUsername(username: String): UserEntity?

    @Insert
    fun insert(user: UserEntity)

    @Update
    fun update(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users")
    fun getCount(): Int
}
