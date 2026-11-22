package com.redline.companion

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.security.MessageDigest

class MainActivity : Activity() {
    // Compare a SHA-256 digest so the access password is not stored as plain text.
    private val accessPasswordHash = "e000eb1cce3bdb4f8aad29d14380616d2d82a7d57212345659f05fd7aa16bc24"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLogin()
    }

    private fun showLogin() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(36), dp(28), dp(36))
            setBackgroundColor(Color.rgb(12, 12, 14))
        }
        root.addView(TextView(this).apply {
            text = "REDLINE"
            textSize = 34f
            setTextColor(Color.rgb(235, 35, 45))
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "SECURE ACCESS"
            textSize = 13f
            letterSpacing = 0.18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(28))
        })
        val passwordInput = EditText(this).apply {
            hint = "Enter access password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(210, 35, 48))
        }
        root.addView(passwordInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)))
        root.addView(Button(this).apply {
            text = "LOGIN"
            setOnClickListener {
                val digest = MessageDigest.getInstance("SHA-256")
                    .digest(passwordInput.text.toString().toByteArray(Charsets.UTF_8))
                    .joinToString("") { "%02x".format(it) }
                if (digest == accessPasswordHash) {
                    Toast.makeText(this@MainActivity, "Login successful", Toast.LENGTH_SHORT).show()
                    showDashboard()
                } else {
                    passwordInput.text.clear()
                    passwordInput.error = "Incorrect password"
                }
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)).apply { topMargin = dp(18) })
        root.addView(TextView(this).apply {
            text = "Authorized access only"
            textSize = 12f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, 0)
        })
        setContentView(root)
    }

    private fun showDashboard() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(36), dp(28), dp(36))
            setBackgroundColor(Color.rgb(12, 12, 14))
        }
        root.addView(TextView(this).apply {
            text = "REDLINE"
            textSize = 32f
            setTextColor(Color.rgb(235, 35, 45))
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Android 15 • Gaming companion overlay\n\nFloating shortcuts and session tools. No game injection or game-memory access."
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(28))
        })
        root.addView(Button(this).apply {
            text = "1. Allow display over other apps"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        })
        root.addView(Button(this).apply {
            text = "2. Start REDLINE floating menu"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                    Toast.makeText(this@MainActivity, "Allow REDLINE to display over other apps, then tap Start again.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                try {
                    val intent = Intent(this@MainActivity, OverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
                    Toast.makeText(this@MainActivity, "Starting REDLINE floating controls…", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Could not start overlay: ${e.javaClass.simpleName} — ${e.message ?: "check app permissions"}", Toast.LENGTH_LONG).show()
                }
            }
        })
        root.addView(Button(this).apply {
            text = "Stop floating menu"
            setOnClickListener { stopService(Intent(this@MainActivity, OverlayService::class.java)) }
        })
        setContentView(root)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
