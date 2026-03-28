package com.example.pruebaquechua

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class WordItem(
    val word: String, 
    val category: String, 
    val definition: String,
    val audioResId: Int? = null
)

class DictionaryAdapter(
    private var words: List<WordItem>,
    private val onAudioClick: (WordItem) -> Unit
) : RecyclerView.Adapter<DictionaryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvWord: TextView = view.findViewById(R.id.tvWord)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvDefinition: TextView = view.findViewById(R.id.tvDefinition)
        val btnAudio: ImageButton = view.findViewById(R.id.btnAudio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dictionary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = words[position]
        holder.tvWord.text = item.word
        holder.tvCategory.text = item.category
        holder.tvDefinition.text = item.definition
        
        holder.btnAudio.setOnClickListener {
            onAudioClick(item)
        }
    }

    override fun getItemCount() = words.size

    fun updateList(newList: List<WordItem>) {
        words = newList
        notifyDataSetChanged()
    }
}
