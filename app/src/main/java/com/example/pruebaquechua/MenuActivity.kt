package com.example.pruebaquechua

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val isGuest = intent.getBooleanExtra("IS_GUEST", false)
        val isAdmin = intent.getBooleanExtra("IS_ADMIN", false)

        val cardEducacion = findViewById<MaterialCardView>(R.id.cardEducacion)
        val cardTecnologia = findViewById<MaterialCardView>(R.id.cardTecnologia)
        val cardMedicina = findViewById<MaterialCardView>(R.id.cardMedicina)
        val cardBiologia = findViewById<MaterialCardView>(R.id.cardBiologia)
        val cardGuestWarning = findViewById<MaterialCardView>(R.id.cardGuestWarning)

        // Si es invitado, mostramos la tarjeta de aviso y ocultamos algunas categorías
        if (isGuest) {
            cardGuestWarning.visibility = View.VISIBLE
            cardMedicina.visibility = View.GONE
            cardBiologia.visibility = View.GONE
        } else {
            cardGuestWarning.visibility = View.GONE
            cardMedicina.visibility = View.VISIBLE
            cardBiologia.visibility = View.VISIBLE
        }

        cardEducacion.setOnClickListener {
            val intent = Intent(this, DictionaryActivity::class.java)
            intent.putExtra("CATEGORY", "Educación")
            intent.putExtra("IS_GUEST", isGuest)
            intent.putExtra("IS_ADMIN", isAdmin)
            startActivity(intent)
        }

        cardTecnologia.setOnClickListener {
            val intent = Intent(this, DictionaryActivity::class.java)
            intent.putExtra("CATEGORY", "Tecnología")
            intent.putExtra("IS_GUEST", isGuest)
            intent.putExtra("IS_ADMIN", isAdmin)
            startActivity(intent)
        }

        cardMedicina.setOnClickListener {
            val intent = Intent(this, DictionaryActivity::class.java)
            intent.putExtra("CATEGORY", "Medicina")
            intent.putExtra("IS_GUEST", isGuest)
            intent.putExtra("IS_ADMIN", isAdmin)
            startActivity(intent)
        }

        cardBiologia.setOnClickListener {
            val intent = Intent(this, DictionaryActivity::class.java)
            intent.putExtra("CATEGORY", "Biología")
            intent.putExtra("IS_GUEST", isGuest)
            intent.putExtra("IS_ADMIN", isAdmin)
            startActivity(intent)
        }
    }
}
