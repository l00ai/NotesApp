package com.example.notes.dao

import androidx.room.*
import com.example.notes.entities.Note

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY id DESC")
    fun getAllNotes(): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNote(note: Note): Long

    @Delete
    fun deleteNote(note: Note)

    @Query("UPDATE notes SET isUploaded = :b WHERE id = :id")
    fun isUploaded(id: Int, b: Boolean)
}