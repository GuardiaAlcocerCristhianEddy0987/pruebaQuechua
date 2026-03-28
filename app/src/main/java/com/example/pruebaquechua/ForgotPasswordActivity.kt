package com.example.pruebaquechua

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val btnEnviar = findViewById<Button>(R.id.btnEnviarRecuperacion)
        val btnVolver = findViewById<Button>(R.id.btnVolverLoginForgot)

        btnEnviar.setOnClickListener {
            Toast.makeText(this, "Se han enviado las instrucciones a tu correo", Toast.LENGTH_LONG).show()
            finish()
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }
}
