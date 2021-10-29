package com.example.notes.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.notes.dao.NoteDao
import com.example.notes.entities.Note

@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class NotesDatabase : RoomDatabase() {

    companion object {
         var notesDatabase: NotesDatabase? = null

         @Synchronized fun getDatabase(context: Context): NotesDatabase{
             if (notesDatabase == null){
                 notesDatabase = Room.databaseBuilder(context, NotesDatabase::class.java , "notes_db").build()
             }
             return notesDatabase!!
         }

    }

     abstract fun noteDao(): NoteDao
}