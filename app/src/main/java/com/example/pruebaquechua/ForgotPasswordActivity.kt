package com.example.pruebaquechua

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        db = AppDatabase.getDatabase(this)

        val tilUsername = findViewById<TextInputLayout>(R.id.tilForgotUsername)
        val tilNewPassword = findViewById<TextInputLayout>(R.id.tilForgotNewPassword)
        val btnActualizar = findViewById<Button>(R.id.btnActualizarPassword)
        val btnVolver = findViewById<Button>(R.id.btnVolverLoginForgot)

        btnActualizar.setOnClickListener {
            val username = tilUsername.editText?.text.toString()
            val newPassword = tilNewPassword.editText?.text.toString()

            if (username.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.length < 8) {
                Toast.makeText(this, "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val user = db.userDao().getUserByUsername(username)
                
                withContext(Dispatchers.Main) {
                    if (user != null) {
                        actualizarPassword(user, newPassword)
                    } else {
                        Toast.makeText(this@ForgotPasswordActivity, "El usuario no existe", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun actualizarPassword(user: UserEntity, newPassword: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val updatedUser = user.copy(password = newPassword)
            db.userDao().update(updatedUser) // Asumiendo que existe update en UserDao
            
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ForgotPasswordActivity, "Contraseña actualizada con éxito", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}
