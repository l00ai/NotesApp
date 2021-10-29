package com.example.notes.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.example.notes.R
import com.example.notes.database.NotesDatabase
import com.example.notes.entities.Note
import com.example.notes.utilities.Constants
import com.example.notes.utilities.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener
import kotlinx.android.synthetic.main.activity_create_note.*
import kotlinx.android.synthetic.main.layout_add_url.view.*
import kotlinx.android.synthetic.main.layout_add_url.view.textAdd
import kotlinx.android.synthetic.main.layout_add_url.view.textCancel
import kotlinx.android.synthetic.main.layout_delete_note.view.*
import kotlinx.android.synthetic.main.layout_miscellaneous.*
import kotlinx.android.synthetic.main.layout_miscellaneous.view.*
import kotlinx.android.synthetic.main.layout_pick_color.view.*
import kotlinx.android.synthetic.main.layout_signin_google.view.*
import java.text.SimpleDateFormat
import java.util.*

class CreateNoteActivity : AppCompatActivity() {
    private lateinit var selectedNoteColor: String
    private lateinit var selectedImagePath: String
    private var dialogAddUrl: AlertDialog? = null
    private var dialogDeleteNote: AlertDialog? = null
    private var dialogPickColor: AlertDialog? = null
    private var dialogSignIn: AlertDialog? = null
    private var alreadyAvailableNote: Note? = null
    private var note = Note()
    private val db: FirebaseFirestore by lazy {
        Firebase.firestore
    }
    private val preferenceManager: PreferenceManager by lazy {
        PreferenceManager(this)
    }

    companion object {
        const val REQUEST_CODE_STORAGE_PERMISSION = 1
        const val REQUEST_CODE_SELECT_IMAGE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_note)

        imageBack.setOnClickListener {
            onBackPressed()
        }

        textDateTime.text =
            SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(Date())

        imageSave.setOnClickListener {
            saveNote()
        }

        selectedNoteColor = "#333333"
        selectedImagePath = ""

        if (intent.getBooleanExtra("isViewOrUpdate", false)) {
            alreadyAvailableNote = intent.getSerializableExtra("note") as Note
            setViewOrUpdate()
        }

        imageRemoveWebUrl.setOnClickListener {
            textWebUrl.text = ""
            layoutWebUrl.visibility = View.GONE
        }

        imageRemoveImage.setOnClickListener {
            imageNote.setImageBitmap(null)
            imageNote.visibility = View.GONE
            imageRemoveImage.visibility = View.GONE
            selectedImagePath = ""
        }

        initMiscellaneous()
        setSubTitleIndicatorColor()

        if (intent.getBooleanExtra("isFromQuickAction", false)){
            val type = intent.getStringExtra("quickActionType")
            if (type != null){
                if (type == "image"){
                    selectedImagePath = intent.getStringExtra("imagePath")!!
                    imageNote.setImageBitmap(BitmapFactory.decodeFile(selectedImagePath))
                    imageNote.visibility = View.VISIBLE
                    imageRemoveImage.visibility = View.VISIBLE
                }else if (type == "URL"){
                    textWebUrl.text = intent.getStringExtra("URL")
                    layoutWebUrl.visibility = View.VISIBLE
                }
            }

        }

        if(!preferenceManager.getBoolean(Constants.KEY_IS_LOGIN)){
            showSignInDialog()
        }

    }

    private fun setViewOrUpdate() {
        inputNoteTitle.setText(alreadyAvailableNote!!.title)
        inputNoteSubtitle.setText(alreadyAvailableNote!!.subtitle)
        inputNote.setText(alreadyAvailableNote!!.noteText)
        textDateTime.text = alreadyAvailableNote!!.dateTime

        if (alreadyAvailableNote!!.imagePath!!.isNotEmpty()) {
            imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote!!.imagePath))
            imageNote.visibility = View.VISIBLE
            selectedImagePath = alreadyAvailableNote!!.imagePath!!
            imageRemoveImage.visibility = View.VISIBLE
        }
        if (alreadyAvailableNote!!.webLink!!.isNotEmpty()) {
            textWebUrl.text = alreadyAvailableNote!!.webLink
            layoutWebUrl.visibility = View.VISIBLE
        }
    }

    private fun saveNote() {
        if (inputNoteTitle.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Note title can't be empty", Toast.LENGTH_SHORT).show()
            return
        } else if (inputNoteSubtitle.text.toString().trim().isEmpty()
            && inputNote.text.toString().trim().isEmpty()
        ) {
            Toast.makeText(this, "Note  can't be empty", Toast.LENGTH_SHORT).show()
            return
        }

        note.title = inputNoteTitle.text.toString()
        note.subtitle = inputNoteSubtitle.text.toString()
        note.noteText = inputNote.text.toString()
        note.dateTime = textDateTime.text.toString()
        note.color = selectedNoteColor
        note.imagePath = selectedImagePath

        if (layoutWebUrl.visibility == View.VISIBLE) {
            note.webLink = textWebUrl.text.toString()
        }

        if (alreadyAvailableNote != null) {
            note.id = alreadyAvailableNote!!.id
            note.isUploaded = true
        }

        uploadToCloudOrNot()



    }

    private fun uploadToCloud(){

        progressSave.visibility = View.VISIBLE
        imageSave.visibility = View.GONE

        note.title = inputNoteTitle.text.toString()
        note.subtitle = inputNoteSubtitle.text.toString()
        note.noteText = inputNote.text.toString()
        note.dateTime = textDateTime.text.toString()
        note.color = selectedNoteColor
        note.imagePath = selectedImagePath
        note.id = alreadyAvailableNote!!.id
        note.isUploaded = true

        if (layoutWebUrl.visibility == View.VISIBLE) {
            note.webLink = textWebUrl.text.toString()
        }



        db.collection("notes")
            .document(preferenceManager.getString(Constants.KEY_EMAIL)!!)
            .collection("My")
            .document()
            .set(note)
            .addOnSuccessListener {

                class SaveNotesTask : AsyncTask<Void, Void, String>() {

                    override fun doInBackground(vararg params: Void?): String {
                        NotesDatabase.getDatabase(applicationContext).noteDao().isUploaded(note.id, true)
                        return ""
                    }

                    override fun onPostExecute(result: String?) {
                        super.onPostExecute(result)
                        val intent = Intent()
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }

                }
                SaveNotesTask().execute()

            }
            .addOnFailureListener {
                Toast.makeText(this, "Try again", Toast.LENGTH_SHORT).show()
                progressSave.visibility = View.GONE
                imageSave.visibility = View.VISIBLE
            }
    }

    private fun uploadToCloudOrNot() {
        if (preferenceManager.getBoolean(Constants.KEY_IS_LOGIN)
            && (preferenceManager.getBoolean(Constants.KEY_AUTO_SAVE_CLOUD) || cbSaveInCloud.isChecked)){
            progressSave.visibility = View.VISIBLE
            imageSave.visibility = View.GONE



                    class SaveNotesTask1 : AsyncTask<Void, Void, Long>() {

                        override fun doInBackground(vararg params: Void?): Long {
                            note.isUploaded = true
                           val x = NotesDatabase.getDatabase(applicationContext).noteDao().insertNote(note)
                            return x
                        }

                        override fun onPostExecute(result: Long?) {
                            super.onPostExecute(result)
                            val intent = Intent()
                            val newNote = note
                            newNote.id = result!!.toInt()
                            db.collection("notes")
                                .document(preferenceManager.getString(Constants.KEY_EMAIL)!!)
                                .collection("My")
                                .document()
                                .set(newNote)
                                .addOnSuccessListener {

                                }
                                .addOnFailureListener {
                                    Toast.makeText(this@CreateNoteActivity, "Try again", Toast.LENGTH_SHORT).show()
                                    progressSave.visibility = View.GONE
                                    imageSave.visibility = View.VISIBLE
                                }

                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        }

                    }
                    SaveNotesTask1().execute()


        }else{
            class SaveNotesTask : AsyncTask<Void, Void, String>() {

                override fun doInBackground(vararg params: Void?): String {
                    NotesDatabase.getDatabase(applicationContext).noteDao().insertNote(note)
                    return ""
                }

                override fun onPostExecute(result: String?) {
                    super.onPostExecute(result)
                    val intent = Intent()
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }

            }
            SaveNotesTask().execute()
        }

    }

    private fun initMiscellaneous() {
        val layoutMiscellaneous = findViewById<LinearLayout>(R.id.layoutMiscellaneous)
        val bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous)
        layoutMiscellaneous.findViewById<TextView>(R.id.textMiscellaneous).setOnClickListener {
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                bottomSheetBehavior.state != BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        val imageColor1: ImageView = layoutMiscellaneous.imageColor1
        val imageColor2: ImageView = layoutMiscellaneous.imageColor2
        val imageColor3: ImageView = layoutMiscellaneous.imageColor3
        val imageColor4: ImageView = layoutMiscellaneous.imageColor4
        val imageColor5: ImageView = layoutMiscellaneous.imageColor5
        val colorPickerIndicator = layoutMiscellaneous.viewColorPickerIndicator


        layoutMiscellaneous.textColorPicker.setOnClickListener {
            showPickColorDialog()
        }

        layoutMiscellaneous.viewColor1.setOnClickListener {
            selectedNoteColor = "#333333"
            imageColor1.setImageResource(R.drawable.ic_done)
            imageColor2.setImageResource(0)
            imageColor3.setImageResource(0)
            imageColor4.setImageResource(0)
            imageColor5.setImageResource(0)
            colorPickerIndicator.visibility = View.GONE
            setSubTitleIndicatorColor()
        }
        layoutMiscellaneous.viewColor2.setOnClickListener {
            selectedNoteColor = "#FDBE3B"
            imageColor2.setImageResource(R.drawable.ic_done)
            imageColor1.setImageResource(0)
            imageColor3.setImageResource(0)
            imageColor4.setImageResource(0)
            imageColor5.setImageResource(0)
            colorPickerIndicator.visibility = View.GONE
            setSubTitleIndicatorColor()
        }
        layoutMiscellaneous.viewColor3.setOnClickListener {
            selectedNoteColor = "#FF4842"
            imageColor3.setImageResource(R.drawable.ic_done)
            imageColor1.setImageResource(0)
            imageColor2.setImageResource(0)
            imageColor4.setImageResource(0)
            imageColor5.setImageResource(0)
            colorPickerIndicator.visibility = View.GONE
            setSubTitleIndicatorColor()
        }
        layoutMiscellaneous.viewColor4.setOnClickListener {
            selectedNoteColor = "#3D52DC"
            imageColor4.setImageResource(R.drawable.ic_done)
            imageColor1.setImageResource(0)
            imageColor2.setImageResource(0)
            imageColor3.setImageResource(0)
            imageColor5.setImageResource(0)
            colorPickerIndicator.visibility = View.GONE
            setSubTitleIndicatorColor()
        }
        layoutMiscellaneous.viewColor5.setOnClickListener {
            selectedNoteColor = "#000000"
            imageColor5.setImageResource(R.drawable.ic_done)
            imageColor1.setImageResource(0)
            imageColor2.setImageResource(0)
            imageColor3.setImageResource(0)
            imageColor4.setImageResource(0)
            colorPickerIndicator.visibility = View.GONE
            setSubTitleIndicatorColor()
        }
        if (alreadyAvailableNote != null && alreadyAvailableNote!!.color!!.isNotEmpty()) {
            when (alreadyAvailableNote!!.color) {
                "#333333" -> layoutMiscellaneous.viewColor1.performClick()
                "#FDBE3B" -> layoutMiscellaneous.viewColor2.performClick()
                "#FF4842" -> layoutMiscellaneous.viewColor3.performClick()
                "#3D52DC" -> layoutMiscellaneous.viewColor4.performClick()
                "#000000" -> layoutMiscellaneous.viewColor5.performClick()
                else -> {
                    selectedNoteColor = alreadyAvailableNote!!.color!!
                    setSubTitleIndicatorColor()
                    layoutMiscellaneous.imageColor1.setImageResource(0)
                    layoutMiscellaneous.viewColorPickerIndicator.visibility = View.VISIBLE
                }
            }
        }
        layoutMiscellaneous.layoutAddImage.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            if (ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this@CreateNoteActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_STORAGE_PERMISSION)
            } else {
                selectImage()
            }
        }
        layoutMiscellaneous.layoutAddUrl.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            showAddUrlDialog()
        }

        if (alreadyAvailableNote != null) {
            if (!alreadyAvailableNote!!.isUploaded!!){
                layoutUploadToCloud.visibility = View.VISIBLE
                layoutUploadToCloud.setOnClickListener {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    uploadToCloud()
                }
            }
            layoutDeleteNote.visibility = View.VISIBLE
            layoutDeleteNote.setOnClickListener {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                showDeleteNoteDialog()
            }
        }else{
            if (preferenceManager.getBoolean(Constants.KEY_IS_LOGIN) && !preferenceManager.getBoolean(Constants.KEY_AUTO_SAVE_CLOUD)){
                layoutUpload.visibility = View.VISIBLE
            }
        }

    }

    private  fun showPickColorDialog(){
        if(dialogPickColor == null){
            val builder = AlertDialog.Builder(this@CreateNoteActivity)
            builder.setCancelable(false)
            val view = LayoutInflater.from(this).inflate(R.layout.layout_pick_color, findViewById(R.id.LayoutPickColorContainer))
            builder.setView(view)
            dialogPickColor = builder.create()
            if (dialogPickColor!!.window != null) {
                dialogPickColor!!.window!!.setBackgroundDrawable(ColorDrawable(0))
            }
            val random = Random()
            var pickedColor = 0
            view.colorPicker.setColorSelectionListener(object : SimpleColorSelectionListener() {
                override fun onColorSelected(color: Int) {
                    super.onColorSelected(color)
                    // Do whatever you want with the color
                    view.imageColorSelected.background.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                    pickedColor = color
                }
            })

            view.textSet.setOnClickListener {
                if (pickedColor == 0){
                    layoutMiscellaneous.viewColor1.performClick()
                    dialogPickColor!!.dismiss()
                    dialogPickColor = null
                } else {
                    val hexColor = String.format("#%06X", 0xFFFFFF and pickedColor)
                    selectedNoteColor = hexColor
                    imageColor1.setImageResource(0)
                    imageColor2.setImageResource(0)
                    imageColor3.setImageResource(0)
                    imageColor4.setImageResource(0)
                    imageColor5.setImageResource(0)
                    layoutMiscellaneous.viewColorPickerIndicator.background.setColorFilter(pickedColor, PorterDuff.Mode.MULTIPLY)
                    layoutMiscellaneous.viewColorPickerIndicator.visibility = View.VISIBLE
                    setSubTitleIndicatorColor()
                    dialogPickColor!!.dismiss()
                    dialogPickColor = null
                }
            }

            view.textRandomColor.setOnClickListener {
                val color = ColorUtils.HSLToColor(
                    floatArrayOf(random.nextInt(360).toFloat(), random.nextFloat(), random.nextFloat()))
                pickedColor = color
                view.imageColorSelected.background.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                view.colorPicker.setColor(color)
            }
            view.textCancel.setOnClickListener {
                dialogPickColor!!.dismiss()
                dialogPickColor = null
            }
        }
        dialogPickColor!!.show()
    }

    private fun showSignInDialog() {
        if (dialogSignIn == null) {
            val builder = AlertDialog.Builder(this@CreateNoteActivity)
            builder.setCancelable(false)
            val view = LayoutInflater.from(this).inflate(
                R.layout.layout_signin_google,
                findViewById<ViewGroup>(R.id.layoutSignInGoogleContainer)
            )
            builder.setView(view)
            dialogSignIn = builder.create()
            if (dialogSignIn!!.window != null) {
                dialogSignIn!!.window!!.setBackgroundDrawable(ColorDrawable(0))
            }
            view.textSignInGoogle.setOnClickListener {
                val intent = Intent(this, SettingActivity::class.java)
                intent.putExtra("fromSignInDialog", true)
                startActivity(intent)
                dialogSignIn!!.dismiss()
            }
            view.textCancel.setOnClickListener {
                dialogSignIn!!.dismiss()
                dialogSignIn = null
            }
            dialogSignIn!!.show()
        }
    }

    private fun showDeleteNoteDialog() {
        if (dialogDeleteNote == null) {
            val builder = AlertDialog.Builder(this@CreateNoteActivity)
            builder.setCancelable(false)
            val view = LayoutInflater.from(this).inflate(
                R.layout.layout_delete_note,
                findViewById<ViewGroup>(R.id.layoutDeleteNoteContainer)
            )
            builder.setView(view)
            dialogDeleteNote = builder.create()
            if (dialogDeleteNote!!.window != null) {
                dialogDeleteNote!!.window!!.setBackgroundDrawable(ColorDrawable(0))
            }
            view.textDeleteNote.setOnClickListener {

                class DeleteNoteTask : AsyncTask<Void, Void, String>() {

                    override fun doInBackground(vararg p0: Void?): String {
                        NotesDatabase.getDatabase(applicationContext).noteDao().deleteNote(alreadyAvailableNote!!)
                        return ""
                    }

                    override fun onPostExecute(result: String?) {
                        super.onPostExecute(result)
                        Log.e("LOI", alreadyAvailableNote!!.id.toString())
                            db.collection("notes")
                            .document(preferenceManager.getString(Constants.KEY_EMAIL)!!)
                            .collection("My").whereEqualTo("id", alreadyAvailableNote!!.id)
                                .get()
                                .addOnSuccessListener {querySnapShot->

                                    for (document in querySnapShot.documents){
                                        db.collection("notes").document(preferenceManager.getString(Constants.KEY_EMAIL)!!)
                                            .collection("My").document(document.id).delete()
                                            .addOnSuccessListener {
                                                val intent = Intent()
                                                intent.putExtra("isNoteDeleted", true)
                                                setResult(Activity.RESULT_OK, intent)
                                                dialogDeleteNote!!.dismiss()
                                                finish()
                                            }
                                    }

                            }
                            .addOnFailureListener {
                                Toast.makeText(this@CreateNoteActivity, "not delete", Toast.LENGTH_SHORT).show()
                            }


                    }
                }
                DeleteNoteTask().execute()
            }
            view.textCancel.setOnClickListener {
                dialogDeleteNote!!.dismiss()
                dialogDeleteNote = null
            }
            dialogDeleteNote!!.show()
        }
    }

    private fun showAddUrlDialog() {
        if (dialogAddUrl == null) {
            val builder = AlertDialog.Builder(this@CreateNoteActivity)
            builder.setCancelable(false)
            val view = LayoutInflater.from(this).inflate(
                R.layout.layout_add_url,
                findViewById<ViewGroup>(R.id.addLayoutUrlContainer)
            )
            builder.setView(view)
            dialogAddUrl = builder.create()
            if (dialogAddUrl!!.window != null) {
                dialogAddUrl!!.window!!.setBackgroundDrawable(ColorDrawable(0))
            }
            val inputUrl = view.inputUrl
            inputUrl.requestFocus()
            view.textAdd.setOnClickListener {
                if (inputUrl.text.toString().trim().isEmpty()) {
                    Toast.makeText(this@CreateNoteActivity, "Enter URL", Toast.LENGTH_SHORT).show()
                } else if (!Patterns.WEB_URL.matcher(inputUrl.text.toString()).matches()) {
                    Toast.makeText(this@CreateNoteActivity, "Enter valid URL", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    textWebUrl.text = inputUrl.text.toString()
                    layoutWebUrl.visibility = View.VISIBLE
                    dialogAddUrl!!.dismiss()
                    dialogAddUrl = null
                }
            }
            view.textCancel.setOnClickListener {
                dialogAddUrl!!.dismiss()
                dialogAddUrl = null
            }
            dialogAddUrl!!.show()
        }
    }

    private fun setSubTitleIndicatorColor() {
        val gradientDrawable = viewSubTitleIndicator.background as GradientDrawable
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor))

        val gradientDrawable2 = viewColorPickerIndicator.background as GradientDrawable
        gradientDrawable2.setColor(Color.parseColor(selectedNoteColor))
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage()
            } else {
                Toast.makeText(applicationContext, "Permissions Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val selectedImageUri = data.data
                if (selectedImageUri != null) {
                    try {
                        val inputStream = contentResolver.openInputStream(selectedImageUri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)

                        val bitmapSize = bitmap.byteCount
                        if (bitmapSize >= 192000000) {
                            Toast.makeText(
                                applicationContext,
                                "The Image is too large!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {

                            imageNote.setImageBitmap(bitmap)
                            imageNote.setImageURI(data.data)
                            imageNote.visibility = View.VISIBLE
                            selectedImagePath = getPathFromUri(selectedImageUri)
                            imageRemoveImage.visibility = View.VISIBLE
                        }


                    } catch (exception: Exception) {
                        Toast.makeText(applicationContext, exception.message, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
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
}