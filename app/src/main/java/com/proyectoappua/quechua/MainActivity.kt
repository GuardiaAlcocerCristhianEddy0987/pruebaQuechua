package com.proyectoappua.quechua

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)
        
        // Asegurar que los usuarios y palabras existan apenas abre la app
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.prepopulate(this@MainActivity, db)
        }

        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val btnInvitado = findViewById<Button>(R.id.btnInvitado)
        val btnRegistrarse = findViewById<Button>(R.id.btnRegistrarse)
        val btnOlvido = findViewById<Button>(R.id.btnOlvido)
        
        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)

        btnIngresar.setOnClickListener {
            val username = tilEmail.editText?.text.toString()
            val password = tilPassword.editText?.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos:", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val user = db.userDao().getUserByUsername(username)
                val hashedPassword = HashUtils.sha256(password)
                
                withContext(Dispatchers.Main) {
                    if (user != null && user.password == hashedPassword) {
                        val intent = Intent(this@MainActivity, MenuActivity::class.java)
                        intent.putExtra("IS_GUEST", false)
                        intent.putExtra("IS_ADMIN", user.role == "ADMIN")
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@MainActivity, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnInvitado.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            intent.putExtra("IS_GUEST", true)
            startActivity(intent)
        }

        btnRegistrarse.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        btnOlvido.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }
}
