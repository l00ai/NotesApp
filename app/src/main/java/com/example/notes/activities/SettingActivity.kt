package com.example.notes.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.notes.R
import com.example.notes.utilities.Constants
import com.example.notes.utilities.PreferenceManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_setting.*

class SettingActivity : AppCompatActivity() {


    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001


    private val preferenceManager: PreferenceManager by lazy {
        PreferenceManager(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)


        setUserData()


        switch1.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked){
                preferenceManager.putBoolean(Constants.KEY_AUTO_SAVE_CLOUD, true)
            }else{
                preferenceManager.putBoolean(Constants.KEY_AUTO_SAVE_CLOUD, false)
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        if (intent.getBooleanExtra("fromSignInDialog", false)){
            signIn()
        }


        textAddAccount.setOnClickListener {
            signIn()
        }

    }

    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(
            signInIntent, RC_SIGN_IN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task =
                GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Signed in successfully
            Toast.makeText(this, "Signed in successfully", Toast.LENGTH_SHORT).show()
           // val googleId = account?.id ?: ""
            val googleFirstName = account?.givenName ?: ""
            preferenceManager.putString(Constants.KEY_FIRST_NAME, googleFirstName)

            val googleLastName = account?.familyName ?: ""
            preferenceManager.putString(Constants.KEY_LAST_NAME, googleLastName)

            val googleEmail = account?.email ?: ""
            preferenceManager.putString(Constants.KEY_EMAIL, googleEmail)

            val googleProfilePicURL = account?.photoUrl.toString()
            preferenceManager.putString(Constants.KEY_GOOGLE_IMAGE_URI, googleProfilePicURL)

            val googleIdToken = account?.idToken ?: ""
            preferenceManager.putString(Constants.KEY_GOOGLE_ID_TOKEN, googleIdToken)
            preferenceManager.putBoolean(Constants.KEY_IS_LOGIN, true)

            setUserData()
        } catch (e: ApiException) {
            // Sign in was unsuccessful
            Toast.makeText(this, "Sign in was unsuccessful", Toast.LENGTH_SHORT).show()
            Log.e("LOI", " ApiException: ${e.statusCode}")
        }
    }

    private fun setUserData() {
        if (preferenceManager.getBoolean(Constants.KEY_IS_LOGIN)){
            textNote.visibility = View.GONE
            textAddAccount.visibility = View.GONE
            imageSignIn.visibility = View.VISIBLE
            textFullName.text = String.format("%s %s", preferenceManager.getString(Constants.KEY_FIRST_NAME) , preferenceManager.getString(Constants.KEY_LAST_NAME))
            textEmail.text = preferenceManager.getString(Constants.KEY_EMAIL)
            switch1.isEnabled = true
            switch1.isClickable = true
            switch1.isChecked = preferenceManager.getBooleanSwitch(Constants.KEY_AUTO_SAVE_CLOUD)
        }

    }

    private fun signOut() {
        mGoogleSignInClient.signOut()
            .addOnCompleteListener(this) {
                preferenceManager.clearPreference()
            }
    }
}

