package com.example.notes.listeners

import com.example.notes.entities.Note

interface NotesListener {
    fun onNoteClicked(note: Note, position: Int)
}