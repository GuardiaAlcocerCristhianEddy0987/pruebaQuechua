package com.example.pruebaquechua

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
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
        escucharCambiosFirebase()
        sincronizarPalabrasLocales()

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

    private fun sincronizarPalabrasLocales() {
        lifecycleScope.launch(Dispatchers.IO) {
            val localWords = db.wordDao().getLocalOnlyWords()
            for (word in localWords) {
                val wordMap = hashMapOf(
                    "word" to word.word,
                    "definition" to word.definition,
                    "category" to word.category,
                    "type" to word.type,
                    "audioUrl" to word.audioUrl
                )
                firestore.collection("palabras")
                    .add(wordMap)
                    .addOnSuccessListener { docRef ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            db.wordDao().update(word.copy(remoteId = docRef.id))
                        }
                    }
            }
        }
    }

    private fun escucharCambiosFirebase() {
        firestore.collection("palabras")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                snapshots?.let {
                    lifecycleScope.launch(Dispatchers.IO) {
                        for (doc in it.documentChanges) {
                            val data = doc.document.data
                            val remoteWord = data["word"] as String
                            val remoteDef = data["definition"] as String
                            
                            val word = WordEntity(
                                word = remoteWord,
                                definition = remoteDef,
                                category = data["category"] as String,
                                type = data["type"] as String? ?: "Sustantivo",
                                audioUrl = data["audioUrl"] as String?,
                                remoteId = doc.document.id
                            )

                            when (doc.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                                    val existingById = db.wordDao().getWordByRemoteId(doc.document.id)
                                    if (existingById == null) {
                                        val existingByContent = db.wordDao().findWordByContent(remoteWord, remoteDef)
                                        if (existingByContent == null) {
                                            db.wordDao().insert(word)
                                        } else {
                                            db.wordDao().update(existingByContent.copy(remoteId = doc.document.id))
                                        }
                                    }
                                }
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    val existing = db.wordDao().getWordByRemoteId(doc.document.id)
                                    if (existing != null) {
                                        db.wordDao().update(word.copy(id = existing.id))
                                    }
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    val existing = db.wordDao().getWordByRemoteId(doc.document.id)
                                    if (existing != null) {
                                        db.wordDao().delete(existing)
                                    }
                                }
                            }
                        }
                        withContext(Dispatchers.Main) {
                            cargarDatos()
                        }
                    }
                }
            }
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

        btnRecord.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            } else {
                toggleRecording(btnRecord, tvAudioStatus)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Agregar Palabra")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val word = etWord.text.toString()
                val definition = etDefinition.text.toString()
                val category = spCategory.selectedItem.toString()
                val type = spType.selectedItem.toString()

                if (word.isNotEmpty() && definition.isNotEmpty()) {
                    if (audioPath != null) {
                        val file = Uri.fromFile(File(audioPath!!))
                        val audioRef = storage.reference.child("audios/${file.lastPathSegment}")
                        
                        audioRef.putFile(file).addOnSuccessListener {
                            audioRef.downloadUrl.addOnSuccessListener { uri ->
                                val downloadUrl = uri.toString()
                                val newWord = WordEntity(
                                    word = word,
                                    definition = definition,
                                    category = category,
                                    type = type,
                                    audioPath = audioPath,
                                    audioUrl = downloadUrl
                                )
                                guardarPalabra(newWord)
                            }
                        }.addOnFailureListener {
                            Toast.makeText(this, "Error al subir audio", Toast.LENGTH_SHORT).show()
                            val newWord = WordEntity(word = word, definition = definition, category = category, type = type)
                            guardarPalabra(newWord)
                        }
                    } else {
                        val newWord = WordEntity(word = word, definition = definition, category = category, type = type)
                        guardarPalabra(newWord)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarPalabra(word: WordEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            val localId = db.wordDao().insert(word)
            val wordWithId = word.copy(id = localId.toInt())

            withContext(Dispatchers.Main) {
                if (word.category == currentCategory) cargarDatos()
                Toast.makeText(this@DictionaryActivity, "Sincronizando...", Toast.LENGTH_SHORT).show()
            }

            val wordMap = hashMapOf(
                "word" to word.word,
                "definition" to word.definition,
                "category" to word.category,
                "type" to word.type,
                "audioUrl" to word.audioUrl
            )
            
            firestore.collection("palabras")
                .add(wordMap)
                .addOnSuccessListener { docRef ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.wordDao().update(wordWithId.copy(remoteId = docRef.id))
                    }
                }
                .addOnFailureListener {
                    Log.e("Firebase", "Error de sincronización", it)
                }
        }
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
                    // 1. Si el audioPath es diferente al original, es que grabamos uno nuevo
                    if (audioPath != null && audioPath != item.audioPath) {
                        val file = Uri.fromFile(File(audioPath!!))
                        val audioRef = storage.reference.child("audios/${file.lastPathSegment}")

                        audioRef.putFile(file).addOnSuccessListener {
                            audioRef.downloadUrl.addOnSuccessListener { uri ->
                                val nuevoAudioUrl = uri.toString()
                                realizarActualizacion(item, word, definition, category, type, audioPath, nuevoAudioUrl)
                            }
                        }.addOnFailureListener {
                            Toast.makeText(this, "Error al subir nuevo audio", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // 2. Si no cambió el audio, actualizamos usando el audioUrl que ya tenía
                        realizarActualizacion(item, word, definition, category, type, item.audioPath, item.audioUrl)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun realizarActualizacion(
        item: WordEntity,
        word: String,
        definition: String,
        category: String,
        type: String,
        path: String?,
        url: String?
    ) {
        val updatedWord = item.copy(
            word = word,
            definition = definition,
            category = category,
            type = type,
            audioPath = path,
            audioUrl = url
        )

        lifecycleScope.launch(Dispatchers.IO) {
            // Actualizar Localmente
            db.wordDao().update(updatedWord)
            
            // Actualizar en Firestore
            if (item.remoteId != null) {
                val wordMap = hashMapOf(
                    "word" to word,
                    "definition" to definition,
                    "category" to category,
                    "type" to type,
                    "audioUrl" to url
                )
                firestore.collection("palabras").document(item.remoteId!!).set(wordMap)
            }
            
            withContext(Dispatchers.Main) {
                cargarDatos()
                Toast.makeText(this@DictionaryActivity, "Palabra actualizada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarConfirmacionEliminar(item: WordEntity) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Palabra")
            .setMessage("¿Estás seguro de que quieres eliminar esta palabra?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.wordDao().delete(item)
                    if (item.remoteId != null) {
                        firestore.collection("palabras").document(item.remoteId!!)
                            .delete()
                            .addOnSuccessListener { Log.d("Firebase", "Documento borrado") }
                        
                        item.audioUrl?.let { url ->
                            try {
                                val storageRef = storage.getReferenceFromUrl(url)
                                storageRef.delete().addOnSuccessListener { 
                                    Log.d("Firebase", "Audio borrado") 
                                }
                            } catch (e: Exception) {
                                Log.e("Firebase", "Error al borrar audio", e)
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        cargarDatos()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun reproducirAudio(word: WordEntity) {
        if (word.audioUrl != null) {
            val mediaPlayer = android.media.MediaPlayer()
            try {
                mediaPlayer.setDataSource(word.audioUrl)
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener { it.start() }
            } catch (e: Exception) {
                tts?.speak(word.word, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        } else {
            tts?.speak(word.word, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun toggleRecording(btn: Button, status: TextView) {
        if (!isRecording) {
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
                    isRecording = true
                    btn.text = "Detener Graba..."
                    status.text = "Grabando..."
                } catch (e: IOException) {
                    Log.e("Audio", "Error prepare()", e)
                }
            }
        } else {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            btn.text = "Grabar Audio"
            status.text = "Audio listo"
        }
    }

    private fun ocultarTeclado() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
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
