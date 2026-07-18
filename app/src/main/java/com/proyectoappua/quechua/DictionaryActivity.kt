package com.proyectoappua.quechua

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
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
import java.util.UUID
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
        
        // Limpiar archivos temporales de sesiones anteriores
        externalCacheDir?.listFiles()?.forEach { it.delete() }

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
            menu.findItem(R.id.nav_artes_plasticas).isVisible = false
            menu.findItem(R.id.nav_educacion_especial).isVisible = false
            menu.findItem(R.id.nav_matematicas).isVisible = false
            menu.findItem(R.id.nav_ciencias_sociales).isVisible = false
            menu.findItem(R.id.nav_valores_religiones).isVisible = false
            menu.findItem(R.id.nav_comunicacion_lenguajes).isVisible = false
            menu.findItem(R.id.nav_musica).isVisible = false
            menu.findItem(R.id.nav_fisica).isVisible = false
            menu.findItem(R.id.nav_quimica).isVisible = false
            menu.findItem(R.id.nav_lengua_extranjera).isVisible = false
            menu.findItem(R.id.nav_filosofia).isVisible = false
            menu.findItem(R.id.nav_gastronomia).isVisible = false
            menu.findItem(R.id.nav_juridico_legal).isVisible = false
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

        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.prepopulate(this@DictionaryActivity, db)
            withContext(Dispatchers.Main) {
                cargarDatos()
                escucharCambiosFirebase()
                // sincronizarPalabrasLocales() // Comentado para evitar crear duplicados innecesarios
            }
        }

        fabAddWord.setOnClickListener {
            mostrarDialogoAgregar()
        }

        btnMenu.setOnClickListener {
            ocultarTeclado()
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            currentCategory = when (menuItem.itemId) {
                R.id.nav_general -> "GENERAL"
                R.id.nav_educacion -> "Educación"
                R.id.nav_tecnologia -> "Tecnología"
                R.id.nav_medicina -> "Medicina"
                R.id.nav_biologia -> "Biología"
                R.id.nav_artes_plasticas -> "Artes Plásticas"
                R.id.nav_educacion_especial -> "Educación Especial"
                R.id.nav_matematicas -> "Matemáticas"
                R.id.nav_ciencias_sociales -> "Ciencias Sociales"
                R.id.nav_valores_religiones -> "Valores y Religiones"
                R.id.nav_comunicacion_lenguajes -> "Comunicación y Lenguajes"
                R.id.nav_musica -> "Música"
                R.id.nav_fisica -> "Física"
                R.id.nav_quimica -> "Química"
                R.id.nav_lengua_extranjera -> "Lengua Extranjera"
                R.id.nav_filosofia -> "Filosofía"
                R.id.nav_gastronomia -> "Gastronomía"
                R.id.nav_juridico_legal -> "Jurídico Legal"
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
                // Generar remoteId antes de enviar a Firebase para asegurar consistencia
                val remoteId = UUID.randomUUID().toString()
                val wordWithRemoteId = word.copy(remoteId = remoteId)
                
                // Actualizar localmente primero
                db.wordDao().update(wordWithRemoteId)

                val wordMap = hashMapOf(
                    "word" to word.word,
                    "definition" to word.definition,
                    "category" to word.category,
                    "type" to word.type,
                    "audioUrl" to word.audioUrl
                )
                
                // Usar set() con el remoteId generado para evitar duplicados en reintentos
                firestore.collection("palabras").document(remoteId)
                    .set(wordMap)
                    .addOnSuccessListener {
                        Log.d("Sync", "Palabra sincronizada con ID: $remoteId")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Sync", "Error al sincronizar palabra ${word.word}: ${e.message}")
                    }
            }
        }
    }

    private fun escucharCambiosFirebase() {
        firestore.collection("palabras")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Sync", "Error en listener: ${e.message}")
                    return@addSnapshotListener
                }

                snapshots?.let {
                    lifecycleScope.launch(Dispatchers.IO) {
                        var huboCambios = false
                        for (doc in it.documentChanges) {
                            val data = doc.document.data
                            val remoteId = doc.document.id
                            val remoteWord = data["word"] as? String ?: ""
                            
                            val wordFromRemote = WordEntity(
                                word = remoteWord,
                                definition = data["definition"] as? String ?: "",
                                category = data["category"] as? String ?: "Educación",
                                type = data["type"] as? String ?: "Sustantivo",
                                audioUrl = data["audioUrl"] as? String,
                                remoteId = remoteId
                            )

                            var existing = db.wordDao().getWordByRemoteId(remoteId)
                            if (existing == null) {
                                existing = db.wordDao().getWordByText(remoteWord)
                            }

                            when (doc.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    if (existing != null) {
                                        val urlCambio = existing.audioUrl != wordFromRemote.audioUrl
                                        val updated = wordFromRemote.copy(
                                            id = existing.id,
                                            audioPath = if (urlCambio) null else existing.audioPath
                                        )
                                        db.wordDao().update(updated)
                                        huboCambios = true
                                    } else {
                                        db.wordDao().insert(wordFromRemote)
                                        huboCambios = true
                                    }
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    existing?.let {
                                        db.wordDao().delete(it)
                                        huboCambios = true
                                    }
                                }
                            }
                        }
                        if (huboCambios) {
                            withContext(Dispatchers.Main) {
                                cargarDatos()
                            }
                        }
                    }
                }
            }
    }

    private fun cargarDatos() {
        val etSearch = findViewById<TextInputLayout>(R.id.tilSearch).editText
        val query = etSearch?.text?.toString() ?: ""

        if (query.isNotEmpty()) {
            filtrar(query)
            return
        }

        lifecycleScope.launch {
            val words = withContext(Dispatchers.IO) {
                if (currentCategory == "GENERAL") {
                    emptyList<WordEntity>()
                } else {
                    // Obtenemos una lista fresca de la DB
                    db.wordDao().getWordsByCategory(currentCategory)
                }
            }
            fullWordList.clear()
            fullWordList.addAll(words)
            // Forzamos al adapter a ver una nueva lista
            adapter.updateList(ArrayList(fullWordList))
        }
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    private fun filtrar(text: String) {
        searchJob?.cancel()
        val query = text.trim()
        val isGuest = intent.getBooleanExtra("IS_GUEST", false)
        
        if (query.isEmpty()) {
            adapter.updateList(fullWordList)
            return
        }

        searchJob = lifecycleScope.launch {
            kotlinx.coroutines.delay(300) 
            val results = withContext(Dispatchers.IO) {
                if (currentCategory == "GENERAL") {
                    val allResults = db.wordDao().searchAllWords(query)
                    if (isGuest) {
                        allResults.filter { it.category == "Educación" || it.category == "Tecnología" }
                    } else {
                        allResults
                    }
                } else {
                    db.wordDao().searchWords(currentCategory, query)
                }
            }
            adapter.updateList(results)
        }
    }

    private fun mostrarDialogoAgregar() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_word, null)
        val etWord = dialogView.findViewById<EditText>(R.id.etWord)
        val etDefinition = dialogView.findViewById<EditText>(R.id.etDefinition)
        val spCategory = dialogView.findViewById<Spinner>(R.id.spCategory)
        val spType = dialogView.findViewById<Spinner>(R.id.spType)
        val btnRecord = dialogView.findViewById<Button>(R.id.btnRecord)
        val tvAudioStatus = dialogView.findViewById<TextView>(R.id.tvAudioStatus)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBarUpload)

        val categories = arrayOf(
            "Educación", "Tecnología", "Medicina", "Biología",
            "Artes Plásticas", "Educación Especial", "Matemáticas",
            "Ciencias Sociales", "Valores y Religiones",
            "Comunicación y Lenguajes", "Música", "Física", "Química",
            "Lengua Extranjera", "Filosofía", "Gastronomía", "Jurídico Legal"
        )
        spCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spCategory.setSelection(categories.indexOf(if (currentCategory == "GENERAL") "Educación" else currentCategory))

        val types = arrayOf("Sustantivo", "Verbo", "Participio", "Sujeto")
        spType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)

        audioPath = null
        stopRecordingSafely()

        btnRecord.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            } else {
                toggleRecording(btnRecord, tvAudioStatus)
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Agregar Palabra")
            .setView(dialogView)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar") { _, _ ->
                stopRecordingSafely()
                eliminarArchivoLocal(audioPath)
                audioPath = null
            }
            .setOnCancelListener {
                stopRecordingSafely()
                eliminarArchivoLocal(audioPath)
                audioPath = null
            }
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val word = etWord.text.toString().trim()
            val definition = etDefinition.text.toString().trim()
            val category = spCategory.selectedItem.toString()
            val type = spType.selectedItem.toString()

            if (word.isNotEmpty() && definition.isNotEmpty()) {
                stopRecordingSafely()
                val currentAudioPath = audioPath
                if (currentAudioPath != null) {
                    progressBar.isIndeterminate = false
                    progressBar.visibility = View.VISIBLE
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    
                    val file = Uri.fromFile(File(currentAudioPath))
                    val audioRef = storage.reference.child("audios/${file.lastPathSegment}")
                    
                    audioRef.putFile(file)
                        .addOnProgressListener { taskSnapshot ->
                            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                            progressBar.progress = progress
                        }
                        .addOnSuccessListener {
                            audioRef.downloadUrl.addOnSuccessListener { uri ->
                                val downloadUrl = uri.toString()
                                val newWord = WordEntity(
                                    word = word,
                                    definition = definition,
                                    category = category,
                                    type = type,
                                    audioPath = currentAudioPath,
                                    audioUrl = downloadUrl
                                )
                                guardarPalabra(newWord)
                                dialog.dismiss()
                            }
                        }.addOnFailureListener {
                            progressBar.visibility = View.GONE
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            Toast.makeText(this, "Error al subir audio", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    val newWord = WordEntity(word = word, definition = definition, category = category, type = type)
                    guardarPalabra(newWord)
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(this, "Complete los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun guardarPalabra(word: WordEntity) {
        // Generar un remoteId localmente para evitar duplicados por reintentos
        val remoteId = UUID.randomUUID().toString()
        val wordToSave = word.copy(remoteId = remoteId)

        lifecycleScope.launch(Dispatchers.IO) {
            val localId = db.wordDao().insert(wordToSave)
            val wordWithId = wordToSave.copy(id = localId.toInt())

            withContext(Dispatchers.Main) {
                if (currentCategory == "GENERAL" || word.category == currentCategory) cargarDatos()
                Toast.makeText(this@DictionaryActivity, "Sincronizando...", Toast.LENGTH_SHORT).show()
            }

            val wordMap: Map<String, Any?> = mapOf(
                "word" to word.word,
                "definition" to word.definition,
                "category" to word.category,
                "type" to word.type,
                "audioUrl" to word.audioUrl
            )
            
            // Usar .document(id).set() en lugar de .add() para asegurar unicidad
            firestore.collection("palabras").document(remoteId)
                .set(wordMap)
                .addOnSuccessListener {
                    Log.d("Firestore", "Palabra guardada con ID: $remoteId")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error al añadir palabra: ${e.message}")
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
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBarUpload)

        etWord.setText(item.word)
        etDefinition.setText(item.definition)
        
        val categories = arrayOf(
            "Educación", "Tecnología", "Medicina", "Biología",
            "Artes Plásticas", "Educación Especial", "Matemáticas",
            "Ciencias Sociales", "Valores y Religiones",
            "Comunicación y Lenguajes", "Música", "Física", "Química",
            "Lengua Extranjera", "Filosofía", "Gastronomía", "Jurídico Legal"
        )
        spCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        val categoryIndex = categories.indexOf(item.category)
        spCategory.setSelection(if (categoryIndex >= 0) categoryIndex else 0)

        val types = arrayOf("Sustantivo", "Verbo", "Participio", "Sujeto")
        spType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spType.setSelection(types.indexOf(item.type))

        stopRecordingSafely()
        val originalAudioPath = item.audioPath
        audioPath = originalAudioPath
        if (audioPath != null) tvAudioStatus.text = "Audio actual conservado"

        btnRecord.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            } else {
                toggleRecording(btnRecord, tvAudioStatus)
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar Palabra")
            .setView(dialogView)
            .setPositiveButton("Actualizar", null)
            .setNegativeButton("Cancelar") { _, _ ->
                stopRecordingSafely()
                if (audioPath != originalAudioPath) {
                    eliminarArchivoLocal(audioPath)
                }
                audioPath = null
            }
            .setOnCancelListener {
                stopRecordingSafely()
                if (audioPath != originalAudioPath) {
                    eliminarArchivoLocal(audioPath)
                }
                audioPath = null
            }
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val word = etWord.text.toString().trim()
            val definition = etDefinition.text.toString().trim()
            val category = spCategory.selectedItem.toString()
            val type = spType.selectedItem.toString()

            if (word.isNotEmpty() && definition.isNotEmpty()) {
                stopRecordingSafely()
                val currentAudioPath = audioPath
                if (currentAudioPath != null && currentAudioPath != originalAudioPath) {
                    progressBar.isIndeterminate = false
                    progressBar.visibility = View.VISIBLE
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    
                    val file = Uri.fromFile(File(currentAudioPath))
                    val audioRef = storage.reference.child("audios/${file.lastPathSegment}")
                    
                    audioRef.putFile(file)
                        .addOnProgressListener { taskSnapshot ->
                            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                            progressBar.progress = progress
                        }
                        .addOnSuccessListener {
                            audioRef.downloadUrl.addOnSuccessListener { uri ->
                                realizarActualizacion(item, word, definition, category, type, currentAudioPath, uri.toString())
                                dialog.dismiss()
                            }
                        }.addOnFailureListener {
                            progressBar.visibility = View.GONE
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            Toast.makeText(this, "Error al subir audio", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    realizarActualizacion(item, word, definition, category, type, item.audioPath, item.audioUrl)
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(this, "Complete los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun realizarActualizacion(item: WordEntity, word: String, definition: String, category: String, type: String, path: String?, url: String?) {
        val oldAudioUrl = item.audioUrl
        // Si no tiene remoteId, generamos uno ahora para mantener la estrategia de UUID
        val remoteId = item.remoteId ?: UUID.randomUUID().toString()
        val updatedWord = item.copy(
            word = word, 
            definition = definition, 
            category = category, 
            type = type, 
            audioPath = path, 
            audioUrl = url,
            remoteId = remoteId
        )
        
        lifecycleScope.launch(Dispatchers.IO) {
            db.wordDao().update(updatedWord)
            
            val wordMap: Map<String, Any?> = mapOf(
                "word" to word,
                "definition" to definition,
                "category" to category,
                "type" to type,
                "audioUrl" to url
            )

            // Usar siempre .document(remoteId).set() (o update) para asegurar unicidad
            firestore.collection("palabras").document(remoteId)
                .set(wordMap) // set() con Merge (o completo) es más robusto para asegurar que el documento exista con estos datos
                .addOnSuccessListener {
                    Log.d("Firestore", "Palabra sincronizada (ID: $remoteId): $word")
                    // ELIMINAR AUDIO ANTERIOR SI CAMBIÓ
                    if (url != oldAudioUrl && !oldAudioUrl.isNullOrEmpty()) {
                        eliminarAudioDeStorage(oldAudioUrl)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error al sincronizar: ${e.message}")
                }
            
            withContext(Dispatchers.Main) {
                cargarDatos()
                Toast.makeText(this@DictionaryActivity, "Se actualizó correctamente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun eliminarAudioDeStorage(url: String?) {
        if (url.isNullOrEmpty()) return
        try {
            val ref = storage.getReferenceFromUrl(url)
            ref.delete().addOnSuccessListener {
                Log.d("Storage", "Archivo eliminado con éxito")
            }.addOnFailureListener {
                Log.e("Storage", "No se pudo eliminar el archivo: ${it.message}")
            }
        } catch (e: Exception) {
            Log.e("Storage", "Error al procesar URL de borrado")
        }
    }

    private fun mostrarConfirmacionEliminar(item: WordEntity) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Palabra")
            .setMessage("¿Estás seguro de eliminar '${item.word}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                val audioUrlAEliminar = item.audioUrl
                val audioPathLocalAEliminar = item.audioPath
                lifecycleScope.launch(Dispatchers.IO) {
                    db.wordDao().delete(item)
                    // Limpieza local
                    eliminarArchivoLocal(audioPathLocalAEliminar)

                    if (item.remoteId != null) {
                        firestore.collection("palabras").document(item.remoteId!!)
                            .delete()
                            .addOnSuccessListener {
                                Log.d("Firestore", "Eliminado de la nube")
                                eliminarAudioDeStorage(audioUrlAEliminar)
                            }
                    }
                    withContext(Dispatchers.Main) {
                        cargarDatos()
                    }
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun reproducirAudio(word: WordEntity) {
        val localFile = word.audioPath?.let { File(it) }
        if (localFile != null && localFile.exists()) {
            playMedia(word, localFile.absolutePath)
        } else if (!word.audioUrl.isNullOrEmpty()) {
            playMedia(word, word.audioUrl!!)
        } else {
            tts?.speak(word.word, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun playMedia(word: WordEntity, source: String) {
        val mediaPlayer = android.media.MediaPlayer()
        try {
            mediaPlayer.setDataSource(source)
            mediaPlayer.setOnPreparedListener { it.start() }
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.setOnErrorListener { mp, _, _ ->
                mp.release()
                tts?.speak(word.word, TextToSpeech.QUEUE_FLUSH, null, null)
                true
            }
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            mediaPlayer.release()
            tts?.speak(word.word, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun toggleRecording(btn: Button, status: TextView) {
        try {
            if (!isRecording) {
                // Limpiar audio anterior si se grabó varias veces en el mismo diálogo
                // En edición, solo borramos si no es el audio original de la palabra
                val isEditing = intent.hasExtra("IS_ADMIN") // Simplificación para detectar contexto
                // Nota: En mostrarDialogoEditar guardamos originalAudioPath, aquí usamos una lógica preventiva
                
                val file = File(externalCacheDir, "audio_${System.currentTimeMillis()}.m4a")
                val newPath = file.absolutePath
                
                mediaRecorder = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(newPath)
                    prepare()
                    start()
                }
                
                // Si ya había un audio grabado en esta sesión de diálogo, lo borramos antes de actualizar la referencia
                // (Solo si el path anterior estaba en cache, para no borrar audios permanentes por error)
                if (audioPath != null && audioPath!!.contains(externalCacheDir?.absolutePath ?: "")) {
                    eliminarArchivoLocal(audioPath)
                }
                
                audioPath = newPath
                isRecording = true
                btn.text = "Detener"
                status.text = "Grabando..."
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
        } catch (e: Exception) {
            Log.e("Audio", "Error en grabación: ${e.message}")
            Toast.makeText(this, "Error con el micrófono", Toast.LENGTH_SHORT).show()
            stopRecordingSafely()
            btn.text = "Grabar Audio"
        }
    }

    private fun eliminarArchivoLocal(path: String?) {
        if (path == null) return
        try {
            val file = File(path)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d("AudioCleanup", "Archivo eliminado: $path -> $deleted")
            }
        } catch (e: Exception) {
            Log.e("AudioCleanup", "Error al eliminar: ${e.message}")
        }
    }

    private fun stopRecordingSafely() {
        if (isRecording || mediaRecorder != null) {
            try {
                mediaRecorder?.stop()
            } catch (e: Exception) {
                Log.e("Audio", "Error al detener grabación: ${e.message}")
            } finally {
                try {
                    mediaRecorder?.release()
                } catch (e: Exception) {
                    Log.e("Audio", "Error al liberar MediaRecorder: ${e.message}")
                }
                mediaRecorder = null
                isRecording = false
            }
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
        stopRecordingSafely()
        tts?.stop(); tts?.shutdown()
        super.onDestroy()
    }
}
