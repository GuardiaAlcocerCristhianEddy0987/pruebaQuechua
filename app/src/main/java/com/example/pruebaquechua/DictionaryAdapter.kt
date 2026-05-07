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
    private var words: List<WordEntity>,
    private var isAdmin: Boolean = false,
    private val onAudioClick: (WordEntity) -> Unit,
    private val onEditClick: (WordEntity) -> Unit = {},
    private val onDeleteClick: (WordEntity) -> Unit = {}
) : RecyclerView.Adapter<DictionaryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvWord: TextView = view.findViewById(R.id.tvWord)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvType: TextView = view.findViewById(R.id.tvType)
        val tvDefinition: TextView = view.findViewById(R.id.tvDefinition)
        val btnAudio: ImageButton = view.findViewById(R.id.btnAudio)
        val llAdminActions: View = view.findViewById(R.id.llAdminActions)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
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
        holder.tvType.text = item.type
        holder.tvDefinition.text = item.definition
        
        holder.btnAudio.setOnClickListener {
            onAudioClick(item)
        }

        if (isAdmin) {
            holder.llAdminActions.visibility = View.VISIBLE
            holder.btnEdit.setOnClickListener { onEditClick(item) }
            holder.btnDelete.setOnClickListener { onDeleteClick(item) }
        } else {
            holder.llAdminActions.visibility = View.GONE
        }
    }

    override fun getItemCount() = words.size

    fun updateList(newList: List<WordEntity>) {
        words = newList
        notifyDataSetChanged()
    }

    fun setAdminMode(admin: Boolean) {
        isAdmin = admin
        notifyDataSetChanged()
    }
}
