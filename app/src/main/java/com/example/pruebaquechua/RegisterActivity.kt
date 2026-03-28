package com.example.pruebaquechua

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val btnFinalizar = findViewById<Button>(R.id.btnFinalizarRegistro)
        val btnVolver = findViewById<Button>(R.id.btnVolverLogin)

        btnFinalizar.setOnClickListener {
            Toast.makeText(this, "Registro completado con éxito", Toast.LENGTH_LONG).show()
            finish() // Vuelve al login
        }

        btnVolver.setOnClickListener {
            finish() // Vuelve al login
        }
    }
}
