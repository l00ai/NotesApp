package com.example.notes.adapters

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notes.R
import com.example.notes.entities.Note
import com.example.notes.listeners.NotesListener
import kotlinx.android.synthetic.main.item_container_note.view.*
import java.util.*
import kotlin.collections.ArrayList

class NotesAdapter(var notes: List<Note>, var notesListener: NotesListener) :
    RecyclerView.Adapter<NotesAdapter.ViewHolder>() {

        var noteSource: List<Note> = notes
        var timer: Timer? = null


    class ViewHolder(item: View) : RecyclerView.ViewHolder(item) {
        private val textTitle = item.textTitle
        private val textSubTitle = item.textSubTitle
        private val textDateTime = item.textDateTime
        private val imageNote = item.imageNote
        private val notificationProblem = item.notificationProblem

        val layoutNote = item

        fun setNote(note: Note) {
            Log.e("LOI", "note: $note")
            textTitle.text = note.title
            if (note.subtitle!!.trim().isEmpty()) {
                textSubTitle.visibility = View.GONE
            } else {
                textSubTitle.text = note.subtitle
            }
            textDateTime.text = note.dateTime

            val gradientDrawable = layoutNote.background as GradientDrawable
            if (note.color!!.trim().isNotEmpty()) {
                gradientDrawable.setColor(Color.parseColor(note.color))
            } else {
                gradientDrawable.setColor(Color.parseColor("#333333"))
            }
            if (note.imagePath != null) {
                imageNote.setImageBitmap(BitmapFactory.decodeFile(note.imagePath))
                imageNote.visibility = View.VISIBLE
            } else {
                imageNote.visibility = View.GONE
            }

            if(!note.isUploaded!!){
                notificationProblem.visibility = View.VISIBLE
            }else{
                notificationProblem.visibility = View.GONE
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_container_note, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return notes.size
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setNote(notes[position])
        holder.layoutNote.setOnClickListener {
            notesListener.onNoteClicked(notes[position], position)
        }
    }
    fun searchNotes(searchKeyword: String){
        timer = Timer()
        timer!!.schedule(object :TimerTask(){
            override fun run() {
                if (searchKeyword.trim().isEmpty()){
                    notes = noteSource
                } else {
                    val timp = ArrayList<Note>()
                    for (note in noteSource){
                        if (note.title!!.toLowerCase().contains(searchKeyword.toLowerCase())
                            ||note.subtitle!!.toLowerCase().contains(searchKeyword.toLowerCase())
                            ||note.noteText!!.toLowerCase().contains(searchKeyword.toLowerCase())){
                            timp.add(note)
                        }
                    }
                    notes = timp
                }
                Handler(Looper.getMainLooper()).post {
                    notifyDataSetChanged()
                }
            }

        }, 500)
    }

    fun cancelTimer(){
        if (timer != null){
            timer!!.cancel()
        }
    }

}