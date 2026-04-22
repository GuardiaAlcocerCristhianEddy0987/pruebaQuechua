package com.example.pruebaquechua

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val user = db.userDao().getUserByUsername(username)
                
                withContext(Dispatchers.Main) {
                    if (user != null && user.password == password) {
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
