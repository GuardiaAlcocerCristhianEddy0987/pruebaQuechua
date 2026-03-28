package com.example.pruebaquechua

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val cardEducacion = findViewById<MaterialCardView>(R.id.cardEducacion)
        val cardTecnologia = findViewById<MaterialCardView>(R.id.cardTecnologia)
        val cardMedicina = findViewById<MaterialCardView>(R.id.cardMedicina)

        cardEducacion.setOnClickListener {
            val intent = Intent(this, DictionaryActivity::class.java)
            intent.putExtra("CATEGORY", "Educación")
            startActivity(intent)
        }

        cardTecnologia.setOnClickListener {
            val intent = Intent(this, DictionaryActivity::class.java)
            intent.putExtra("CATEGORY", "Tecnología")
            startActivity(intent)
        }

        cardMedicina.setOnClickListener {
            val intent = Intent(this, DictionaryActivity::class.java)
            intent.putExtra("CATEGORY", "Medicina")
            startActivity(intent)
        }
    }
}
