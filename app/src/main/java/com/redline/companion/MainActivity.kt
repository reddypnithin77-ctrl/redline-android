package com.redline.companion

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 36, 28, 36)
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
            setPadding(0, 20, 0, 28)
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
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(this@MainActivity, OverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
                } else {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
            }
        })
        root.addView(Button(this).apply {
            text = "Stop floating menu"
            setOnClickListener { stopService(Intent(this@MainActivity, OverlayService::class.java)) }
        })
        setContentView(root)
    }
}
