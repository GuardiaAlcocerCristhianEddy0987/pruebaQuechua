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

class RegisterActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        db = AppDatabase.getDatabase(this)

        val tilName = findViewById<TextInputLayout>(R.id.tilRegisterName)
        val tilEmail = findViewById<TextInputLayout>(R.id.tilRegisterEmail)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilRegisterPassword)
        val tilConfirmPassword = findViewById<TextInputLayout>(R.id.tilRegisterConfirmPassword)

        val btnFinalizar = findViewById<Button>(R.id.btnFinalizarRegistro)
        val btnVolver = findViewById<Button>(R.id.btnVolverLogin)

        btnFinalizar.setOnClickListener {
            val username = tilEmail.editText?.text.toString() // Usamos el email como nombre de usuario para el login
            val password = tilPassword.editText?.text.toString()
            val confirmPassword = tilConfirmPassword.editText?.text.toString()

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 8) {
                Toast.makeText(this, "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val existingUser = db.userDao().getUserByUsername(username)
                
                withContext(Dispatchers.Main) {
                    if (existingUser != null) {
                        Toast.makeText(this@RegisterActivity, "El usuario ya existe", Toast.LENGTH_SHORT).show()
                    } else {
                        saveUser(username, password)
                    }
                }
            }
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun saveUser(username: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val newUser = UserEntity(username = username, password = password, role = "USER")
            db.userDao().insert(newUser)
            
            withContext(Dispatchers.Main) {
                Toast.makeText(this@RegisterActivity, "Registro completado con éxito", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}
