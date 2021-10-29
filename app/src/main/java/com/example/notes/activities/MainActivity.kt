package com.example.notes.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.notes.R
import com.example.notes.adapters.NotesAdapter
import com.example.notes.database.NotesDatabase
import com.example.notes.entities.Note
import com.example.notes.listeners.NotesListener
import kotlinx.android.synthetic.main.activity_create_note.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_add_url.view.*
import java.lang.Exception
import java.util.ArrayList

class MainActivity : AppCompatActivity(), NotesListener {

    val noteList = ArrayList<Note>()

    lateinit var noteAdapter: NotesAdapter
    private var noteClickedPosition = -1
    private var dialogAddURL: AlertDialog? = null

    companion object {
        const val REQUEST_CODE_ADD_NOTE = 1
        const val REQUEST_CODE_UPDATE_NOTE = 2
        const val REQUEST_CODE_SHOW_NOTE = 3
        const val REQUEST_CODE_SELECT_IMAGE = 4
        const val REQUEST_CODE_STORAGE_PERMISSION = 5
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageAddNoteMain.setOnClickListener {
            startActivityForResult(
                Intent(applicationContext, CreateNoteActivity::class.java),
                REQUEST_CODE_ADD_NOTE
            )
        }

        imageSetting.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        notesRecyclerView.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        //   noteList = ArrayList()
        noteAdapter = NotesAdapter(noteList, this)
        notesRecyclerView.adapter = noteAdapter

        getNotes(REQUEST_CODE_SHOW_NOTE, false)

        inputSearch.addTextChangedListener(object :TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                if (noteList.size != 0){
                    noteAdapter.searchNotes(p0.toString())
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
               
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                noteAdapter.cancelTimer()
            }

        })

        imageAddNote.setOnClickListener {
            startActivityForResult(
                Intent(applicationContext, CreateNoteActivity::class.java),
                REQUEST_CODE_ADD_NOTE
            )
        }
        imageAddImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_STORAGE_PERMISSION
                )
            } else {
                selectImage()
            }
        }

        imageAddWebLink.setOnClickListener {
            showAddUrlDialog()
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, CreateNoteActivity.REQUEST_CODE_SELECT_IMAGE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CreateNoteActivity.REQUEST_CODE_STORAGE_PERMISSION && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage()
            } else {
                Toast.makeText(applicationContext, "Permissions Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun getPathFromUri(uri: Uri): String {
        var filePath = ""
        val cursor = contentResolver.query(uri, null, null, null, null)
        if (cursor == null) {
            filePath = uri.path!!
        } else {
            cursor.moveToFirst()
            val index = cursor.getColumnIndex("_data")
            filePath = cursor.getString(index)
            cursor.close()
        }
        return filePath
    }
    override fun onNoteClicked(note: Note, position: Int) {
        noteClickedPosition = position
        val intent = Intent(applicationContext, CreateNoteActivity::class.java)
        intent.putExtra("isViewOrUpdate", true)
        intent.putExtra("note", note)
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE)
    }

    private fun getNotes(requestCodeShowNote: Int, isNoteDeleted: Boolean) {

        class GetNotesTask : AsyncTask<Void, Void, List<Note>>() {

            override fun doInBackground(vararg params: Void?): List<Note> {
                return NotesDatabase.getDatabase(applicationContext).noteDao().getAllNotes()
            }

            override fun onPostExecute(result: List<Note>?) {
                super.onPostExecute(result)

                if (requestCodeShowNote == REQUEST_CODE_SHOW_NOTE) {
                    noteList.addAll(result!!)
                    noteAdapter.notifyDataSetChanged()
                } else if (requestCodeShowNote == REQUEST_CODE_ADD_NOTE) {
                    noteList.add(0, result!![0])
                    noteAdapter.notifyItemInserted(0)
                    notesRecyclerView.smoothScrollToPosition(0)
                } else if (requestCodeShowNote == REQUEST_CODE_UPDATE_NOTE) {
                    noteList.removeAt(noteClickedPosition)
                    if (isNoteDeleted) {
                        noteAdapter.notifyItemRemoved(noteClickedPosition)
                    } else {
                        noteList.add(noteClickedPosition, result!![noteClickedPosition])
                        noteAdapter.notifyItemChanged(noteClickedPosition)
                    }
                }
            }

        }
        GetNotesTask().execute()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == Activity.RESULT_OK) {
            getNotes(REQUEST_CODE_ADD_NOTE, false)
        } else if (requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == Activity.RESULT_OK) {
            getNotes((REQUEST_CODE_UPDATE_NOTE), data!!.getBooleanExtra("isNoteDeleted", false))
        } else if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == Activity.RESULT_OK){
            if (data != null){
                val selectedImageUri = data.data
                if (data.data != null){
                    try {
                        val selectedImagePath = getPathFromUri(selectedImageUri!!)
                        val intent = Intent(applicationContext, CreateNoteActivity::class.java)
                        intent.putExtra("isFromQuickAction", true)
                        intent.putExtra("quickActionType", "image")
                        intent.putExtra("imagePath", selectedImageUri)
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE)

                    } catch (exception: Exception){
                        Toast.makeText(applicationContext, exception.message, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }
    private fun showAddUrlDialog() {
        if (dialogAddURL == null) {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setCancelable(false)
            val view = LayoutInflater.from(this).inflate(
                R.layout.layout_add_url,
                findViewById<ViewGroup>(R.id.addLayoutUrlContainer)
            )
            builder.setView(view)
            dialogAddURL = builder.create()
            if (dialogAddURL!!.window != null) {
                dialogAddURL!!.window!!.setBackgroundDrawable(ColorDrawable(0))
            }
            val inputUrl = view.inputUrl
            inputUrl.requestFocus()
            view.textAdd.setOnClickListener {
                if (inputUrl.text.toString().trim().isEmpty()) {
                    Toast.makeText(this@MainActivity, "Enter URL", Toast.LENGTH_SHORT).show()
                } else if (!Patterns.WEB_URL.matcher(inputUrl.text.toString()).matches()) {
                    Toast.makeText(this@MainActivity, "Enter valid URL", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    dialogAddURL!!.dismiss()
                    dialogAddURL = null
                    val intent = Intent(applicationContext, CreateNoteActivity::class.java)
                    intent.putExtra("isFromQuickAction", true)
                    intent.putExtra("quickActionType", "URL")
                    intent.putExtra("URL", inputUrl.text.toString())
                    startActivityForResult(intent, REQUEST_CODE_ADD_NOTE)
                }
            }
            view.textCancel.setOnClickListener {
                dialogAddURL!!.dismiss()
                dialogAddURL = null
            }
            dialogAddURL!!.show()
        }
    }


}