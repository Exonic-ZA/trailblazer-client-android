package org.traccar.client.trailblazer.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import org.traccar.client.R

class AboutUsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide the system status bar and action bar for a cleaner look
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        setContentView(R.layout.activity_about_us)

        // Setup back button
        val backButton = findViewById<ImageButton>(R.id.btn_back)
        backButton.setOnClickListener {
            finish() // Close this activity and return to previous screen
        }
    }

    // For Kotlin
    fun openPrivacyPolicy(view: View) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://sbmserv.co.za/privacy-policy/")
        startActivity(intent)
    }
}