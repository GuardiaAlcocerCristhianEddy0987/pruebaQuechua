package com.example.pruebaquechua

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Locale

class DictionaryActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var adapter: DictionaryAdapter
    private lateinit var fullWordList: MutableList<WordEntity>
    private lateinit var drawerLayout: DrawerLayout
    private var tts: TextToSpeech? = null
    private lateinit var db: AppDatabase
    private var currentCategory = "Educación"

    private var mediaRecorder: MediaRecorder? = null
    private var audioPath: String? = null
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dictionary)

        db = AppDatabase.getDatabase(this)
        tts = TextToSpeech(this, this)
        drawerLayout = findViewById(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navView)
        val rvDictionary = findViewById<RecyclerView>(R.id.rvDictionary)
        val tilSearch = findViewById<TextInputLayout>(R.id.tilSearch)
        val tvDictTitle = findViewById<TextView>(R.id.tvDictTitle)
        val btnMenu = findViewById<ImageButton>(R.id.btnMenuCategorias)
        val fabAddWord = findViewById<ExtendedFloatingActionButton>(R.id.fabAddWord)
        val etSearch = tilSearch.editText

        val isGuest = intent.getBooleanExtra("IS_GUEST", false)
        val isAdmin = intent.getBooleanExtra("IS_ADMIN", false)
        currentCategory = intent.getStringExtra("CATEGORY") ?: "Educación"
        tvDictTitle.text = "Diccionario: $currentCategory"

        if (isGuest) {
            val menu = navView.menu
            menu.findItem(R.id.nav_medicina).isVisible = false
            menu.findItem(R.id.nav_biologia).isVisible = false
        }

        if (!isAdmin) {
            fabAddWord.visibility = View.GONE
        }

        fullWordList = mutableListOf()
        adapter = DictionaryAdapter(
            words = fullWordList,
            isAdmin = isAdmin,
            onAudioClick = { item -> reproducirAudio(item) },
            onEditClick = { item -> mostrarDialogoEditar(item) },
            onDeleteClick = { item -> mostrarConfirmacionEliminar(item) }
        )
        rvDictionary.layoutManager = LinearLayoutManager(this)
        rvDictionary.adapter = adapter

        cargarDatos()

        fabAddWord.setOnClickListener {
            mostrarDialogoAgregar()
        }

        btnMenu.setOnClickListener {
            ocultarTeclado()
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            currentCategory = when (menuItem.itemId) {
                R.id.nav_educacion -> "Educación"
                R.id.nav_tecnologia -> "Tecnología"
                R.id.nav_medicina -> "Medicina"
                R.id.nav_biologia -> "Biología"
                else -> "Educación"
            }
            tvDictTitle.text = "Diccionario: $currentCategory"
            cargarDatos()
            etSearch?.text?.clear()
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        etSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrar(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            val words = withContext(Dispatchers.IO) {
                db.wordDao().getWordsByCategory(currentCategory)
            }
            fullWordList.clear()
            fullWordList.addAll(words)
            adapter.updateList(fullWordList)
        }
    }

    private fun filtrar(text: String) {
        val filtered = fullWordList.filter {
            it.word.contains(text, ignoreCase = true)
        }
        adapter.updateList(filtered)
    }

    private fun mostrarDialogoAgregar() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_word, null)
        val etWord = dialogView.findViewById<EditText>(R.id.etWord)
        val etDefinition = dialogView.findViewById<EditText>(R.id.etDefinition)
        val spCategory = dialogView.findViewById<Spinner>(R.id.spCategory)
        val spType = dialogView.findViewById<Spinner>(R.id.spType)
        val btnRecord = dialogView.findViewById<Button>(R.id.btnRecord)
        val tvAudioStatus = dialogView.findViewById<TextView>(R.id.tvAudioStatus)

        val categories = arrayOf("Educación", "Tecnología", "Medicina", "Biología")
        spCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spCategory.setSelection(categories.indexOf(currentCategory))

        val types = arrayOf("Sustantivo", "Verbo", "Participio", "Sujeto")
        spType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)

        audioPath = null
        isRecording = false
// ... rest of the method

        btnRecord.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            } else {
                toggleRecording(btnRecord, tvAudioStatus)
            }
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val word = etWord.text.toString()
                val definition = etDefinition.text.toString()
                val category = spCategory.selectedItem.toString()
                val type = spType.selectedItem.toString()

                if (word.isNotEmpty() && definition.isNotEmpty()) {
                    val newWord = WordEntity(
                        word = word,
                        definition = definition,
                        category = category,
                        type = type,
                        audioPath = audioPath
                    )
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.wordDao().insert(newWord)
                        withContext(Dispatchers.Main) {
                            if (category == currentCategory) cargarDatos()
                            Toast.makeText(this@DictionaryActivity, "Palabra guardada", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEditar(item: WordEntity) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_word, null)
        val etWord = dialogView.findViewById<EditText>(R.id.etWord)
        val etDefinition = dialogView.findViewById<EditText>(R.id.etDefinition)
        val spCategory = dialogView.findViewById<Spinner>(R.id.spCategory)
        val spType = dialogView.findViewById<Spinner>(R.id.spType)
        val btnRecord = dialogView.findViewById<Button>(R.id.btnRecord)
        val tvAudioStatus = dialogView.findViewById<TextView>(R.id.tvAudioStatus)

        etWord.setText(item.word)
        etDefinition.setText(item.definition)
        
        val categories = arrayOf("Educación", "Tecnología", "Medicina", "Biología")
        spCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spCategory.setSelection(categories.indexOf(item.category))

        val types = arrayOf("Sustantivo", "Verbo", "Participio", "Sujeto")
        spType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spType.setSelection(types.indexOf(item.type))

        audioPath = item.audioPath
        if (audioPath != null) tvAudioStatus.text = "Audio actual conservado"

        btnRecord.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            } else {
                toggleRecording(btnRecord, tvAudioStatus)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Editar Palabra")
            .setView(dialogView)
            .setPositiveButton("Actualizar") { _, _ ->
                val word = etWord.text.toString()
                val definition = etDefinition.text.toString()
                val category = spCategory.selectedItem.toString()
                val type = spType.selectedItem.toString()

                if (word.isNotEmpty() && definition.isNotEmpty()) {
                    val updatedWord = item.copy(
                        word = word,
                        definition = definition,
                        category = category,
                        type = type,
                        audioPath = audioPath
                    )
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.wordDao().update(updatedWord)
                        withContext(Dispatchers.Main) {
                            cargarDatos()
                            Toast.makeText(this@DictionaryActivity, "Palabra actualizada", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarConfirmacionEliminar(item: WordEntity) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Palabra")
            .setMessage("¿Estás seguro de que deseas eliminar '${item.word}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.wordDao().delete(item)
                    withContext(Dispatchers.Main) {
                        cargarDatos()
                        Toast.makeText(this@DictionaryActivity, "Palabra eliminada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun toggleRecording(btn: Button, status: TextView) {
        if (!isRecording) {
            startRecording()
            btn.text = "Detener Graba..."
            status.text = "Grabando..."
        } else {
            stopRecording()
            btn.text = "Grabar Audio"
            status.text = "Audio listo"
        }
        isRecording = !isRecording
    }

    private fun startRecording() {
        val file = File(externalCacheDir, "audio_${System.currentTimeMillis()}.mp3")
        audioPath = file.absolutePath
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioPath)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
    }

    private fun reproducirAudio(item: WordEntity) {
        when {
            item.audioPath != null -> {
                val mp = MediaPlayer()
                try {
                    mp.setDataSource(item.audioPath)
                    mp.prepare()
                    mp.start()
                    mp.setOnCompletionListener { it.release() }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al reproducir audio grabado", Toast.LENGTH_SHORT).show()
                }
            }
            item.audioResId != null -> {
                val mp = MediaPlayer.create(this, item.audioResId)
                mp.start()
                mp.setOnCompletionListener { it.release() }
            }
            else -> {
                tts?.speak(item.word, TextToSpeech.QUEUE_FLUSH, null, "")
            }
        }
    }

    private fun ocultarTeclado() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
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
