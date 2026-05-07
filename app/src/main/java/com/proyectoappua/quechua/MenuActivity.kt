package com.proyectoappua.quechua

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

        val cardGeneral = findViewById<MaterialCardView>(R.id.cardGeneral)
        val cardEducacion = findViewById<MaterialCardView>(R.id.cardEducacion)
        val cardTecnologia = findViewById<MaterialCardView>(R.id.cardTecnologia)
        val cardMedicina = findViewById<MaterialCardView>(R.id.cardMedicina)
        val cardBiologia = findViewById<MaterialCardView>(R.id.cardBiologia)
        val cardArtesPlasticas = findViewById<MaterialCardView>(R.id.cardArtesPlasticas)
        val cardEducacionEspecial = findViewById<MaterialCardView>(R.id.cardEducacionEspecial)
        val cardMatematicas = findViewById<MaterialCardView>(R.id.cardMatematicas)
        val cardCienciasSociales = findViewById<MaterialCardView>(R.id.cardCienciasSociales)
        val cardValoresReligiones = findViewById<MaterialCardView>(R.id.cardValoresReligiones)
        val cardComunicacionLenguajes = findViewById<MaterialCardView>(R.id.cardComunicacionLenguajes)
        val cardMusica = findViewById<MaterialCardView>(R.id.cardMusica)
        val cardFisica = findViewById<MaterialCardView>(R.id.cardFisica)
        val cardQuimica = findViewById<MaterialCardView>(R.id.cardQuimica)
        val cardLenguaExtranjera = findViewById<MaterialCardView>(R.id.cardLenguaExtranjera)
        val cardFilosofia = findViewById<MaterialCardView>(R.id.cardFilosofia)
        val cardGastronomia = findViewById<MaterialCardView>(R.id.cardGastronomia)
        val cardJuridicoLegal = findViewById<MaterialCardView>(R.id.cardJuridicoLegal)
        val cardGuestWarning = findViewById<MaterialCardView>(R.id.cardGuestWarning)

        // Si es invitado, mostramos la tarjeta de aviso y ocultamos algunas categorías
        if (isGuest) {
            cardGuestWarning.visibility = View.VISIBLE
            cardMedicina.visibility = View.GONE
            cardBiologia.visibility = View.GONE
            cardArtesPlasticas.visibility = View.GONE
            cardEducacionEspecial.visibility = View.GONE
            cardMatematicas.visibility = View.GONE
            cardCienciasSociales.visibility = View.GONE
            cardValoresReligiones.visibility = View.GONE
            cardComunicacionLenguajes.visibility = View.GONE
            cardMusica.visibility = View.GONE
            cardFisica.visibility = View.GONE
            cardQuimica.visibility = View.GONE
            cardLenguaExtranjera.visibility = View.GONE
            cardFilosofia.visibility = View.GONE
            cardGastronomia.visibility = View.GONE
            cardJuridicoLegal.visibility = View.GONE
        } else {
            cardGuestWarning.visibility = View.GONE
            cardMedicina.visibility = View.VISIBLE
            cardBiologia.visibility = View.VISIBLE
            cardArtesPlasticas.visibility = View.VISIBLE
            cardEducacionEspecial.visibility = View.VISIBLE
            cardMatematicas.visibility = View.VISIBLE
            cardCienciasSociales.visibility = View.VISIBLE
            cardValoresReligiones.visibility = View.VISIBLE
            cardComunicacionLenguajes.visibility = View.VISIBLE
            cardMusica.visibility = View.VISIBLE
            cardFisica.visibility = View.VISIBLE
            cardQuimica.visibility = View.VISIBLE
            cardLenguaExtranjera.visibility = View.VISIBLE
            cardFilosofia.visibility = View.VISIBLE
            cardGastronomia.visibility = View.VISIBLE
            cardJuridicoLegal.visibility = View.VISIBLE
        }

        val categorias = mapOf(
            cardGeneral to "GENERAL",
            cardEducacion to "Educación",
            cardTecnologia to "Tecnología",
            cardMedicina to "Medicina",
            cardBiologia to "Biología",
            cardArtesPlasticas to "Artes Plásticas",
            cardEducacionEspecial to "Educación Especial",
            cardMatematicas to "Matemáticas",
            cardCienciasSociales to "Ciencias Sociales",
            cardValoresReligiones to "Valores y Religiones",
            cardComunicacionLenguajes to "Comunicación y Lenguajes",
            cardMusica to "Música",
            cardFisica to "Física",
            cardQuimica to "Química",
            cardLenguaExtranjera to "Lengua Extranjera",
            cardFilosofia to "Filosofía",
            cardGastronomia to "Gastronomía",
            cardJuridicoLegal to "Jurídico Legal"
        )

        categorias.forEach { (card, nombre) ->
            card.setOnClickListener {
                val intent = Intent(this, DictionaryActivity::class.java)
                intent.putExtra("CATEGORY", nombre)
                intent.putExtra("IS_GUEST", isGuest)
                intent.putExtra("IS_ADMIN", isAdmin)
                startActivity(intent)
            }
        }
    }
}
