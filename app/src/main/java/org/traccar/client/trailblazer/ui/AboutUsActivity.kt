package org.traccar.client.trailblazer.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import org.traccar.client.trailblazer.ui.compose.AboutScreen
import org.traccar.client.trailblazer.ui.theme.TrailblazerTheme

class AboutUsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()

        setContent {
            TrailblazerTheme {
                AboutScreen(
                    onBack = { finish() },
                    onOpenPrivacyPolicy = ::openPrivacyPolicy,
                )
            }
        }
    }

    private fun openPrivacyPolicy() {
        val intent = Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri())
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "No browser available", Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val PRIVACY_POLICY_URL = "https://sbmserv.co.za/privacy-policy/"
    }
}

private fun String.toUri(): Uri = Uri.parse(this)
