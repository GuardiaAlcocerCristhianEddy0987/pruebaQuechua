package com.example.pruebaquechua

import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale

class DictionaryActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var adapter: DictionaryAdapter
    private lateinit var fullWordList: List<WordItem>
    private lateinit var drawerLayout: DrawerLayout
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dictionary)

        tts = TextToSpeech(this, this)
        drawerLayout = findViewById(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navView)
        val rvDictionary = findViewById<RecyclerView>(R.id.rvDictionary)
        val tilSearch = findViewById<TextInputLayout>(R.id.tilSearch)
        val tvDictTitle = findViewById<TextView>(R.id.tvDictTitle)
        val btnMenu = findViewById<ImageButton>(R.id.btnMenuCategorias)
        val etSearch = tilSearch.editText

        // Base de datos actualizada con palabras aspiradas y explosivas
        fullWordList = listOf(
            // Educación
            WordItem("Phukuy", "Educación", "Soplar (Aspirada)."),
            WordItem("T'anta", "Educación", "Pan (Explosiva)."),
            // Tecnología
            WordItem("Qhaway", "Tecnología", "Mirar/Observar (Aspirada)."),
            WordItem("K'anchay", "Tecnología", "Luz/Brillo (Explosiva)."),
            // Medicina
            WordItem("Qhali", "Medicina", "Sano/Saludable (Aspirada)."),
            WordItem("K'iri", "Medicina", "Herida (Explosiva).")
        )

        var category = intent.getStringExtra("CATEGORY") ?: "Educación"
        tvDictTitle.text = "Diccionario: $category"
        val initialList = fullWordList.filter { it.category == category }
        
        adapter = DictionaryAdapter(initialList) { item ->
            reproducirAudio(item)
        }
        
        rvDictionary.layoutManager = LinearLayoutManager(this)
        rvDictionary.adapter = adapter

        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        navView.setNavigationItemSelectedListener { menuItem ->
            val newCat = when (menuItem.itemId) {
                R.id.nav_educacion -> "Educación"
                R.id.nav_tecnologia -> "Tecnología"
                R.id.nav_medicina -> "Medicina"
                else -> "Educación"
            }
            category = newCat
            tvDictTitle.text = "Diccionario: $category"
            adapter.updateList(fullWordList.filter { it.category == category })
            etSearch?.text?.clear()
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        etSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s.toString()
                val filtered = fullWordList.filter {
                    it.category == category && (it.word.contains(text, ignoreCase = true))
                }
                adapter.updateList(filtered)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun reproducirAudio(item: WordItem) {
        if (item.audioResId != null) {
            // Si tienes el archivo grabado en res/raw
            val mp = MediaPlayer.create(this, item.audioResId)
            mp.start()
            mp.setOnCompletionListener { it.release() }
        } else {
            // Respaldo con voz del sistema
            tts?.speak(item.word, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.setLanguage(Locale("es", "ES"))
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
