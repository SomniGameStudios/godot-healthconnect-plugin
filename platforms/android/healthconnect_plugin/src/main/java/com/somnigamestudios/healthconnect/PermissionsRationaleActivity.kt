package com.somnigamestudios.healthconnect

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PermissionsRationaleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a simple layout to display the rationale
        val textView = TextView(this).apply {
            text = context.getString(R.string.permissions_rationale_text)
            textSize = 16f
            setPadding(32, 32, 32, 32)
        }
        setContentView(textView)
    }
}