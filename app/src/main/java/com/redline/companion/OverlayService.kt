package com.redline.companion

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import android.graphics.drawable.GradientDrawable

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var root: LinearLayout? = null
    private var expanded = false
    private val channelId = "redline_overlay"

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "REDLINE overlay", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val pending = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, channelId) else Notification.Builder(this)
        startForeground(7, builder.setContentTitle("REDLINE is running")
            .setContentText("Floating companion controls are active")
            .setSmallIcon(android.R.drawable.ic_media_play).setContentIntent(pending).setOngoing(true).build())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    private fun bg(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color); cornerRadius = radius
    }

    private fun showBubble() {
        val bubble = TextView(this).apply {
            text = "R"; textSize = 20f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); background = bg(Color.rgb(190, 18, 35), 100f)
            elevation = 12f
        }
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(58, 58, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 220 }
        bubble.setOnTouchListener(object : View.OnTouchListener {
            var downX=0f; var downY=0f; var startX=0; var startY=0; var moved=false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { downX=e.rawX; downY=e.rawY; startX=params.x; startY=params.y; moved=false; return true }
                    MotionEvent.ACTION_MOVE -> {
                        val dx=(e.rawX-downX).toInt(); val dy=(e.rawY-downY).toInt()
                        if (kotlin.math.abs(dx)+kotlin.math.abs(dy)>8) moved=true
                        params.x=startX+dx; params.y=startY+dy; wm.updateViewLayout(root, params); return true
                    }
                    MotionEvent.ACTION_UP -> { if (!moved) toggleMenu(); return true }
                }
                return false
            }
        })
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(bubble, LinearLayout.LayoutParams(58, 58))
        }
        wm.addView(root, params)
    }

    private fun toggleMenu() {
        val current = root ?: return
        if (expanded) {
            if (current.childCount > 1) current.removeViews(1, current.childCount - 1)
            expanded = false
            return
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(12, 10, 12, 10)
            background = bg(Color.rgb(22, 22, 26), 18f); elevation = 14f
        }
        panel.addView(TextView(this).apply {
            text="REDLINE"; textSize=16f; setTextColor(Color.rgb(255,55,65)); gravity=Gravity.CENTER
        })
        listOf("Session timer: ready", "Quick notes", "Aim practice: offline", "Close menu").forEach { label ->
            panel.addView(Button(this).apply {
                text=label; isAllCaps=false; setTextColor(Color.WHITE)
                backgroundTintList=android.content.res.ColorStateList.valueOf(Color.rgb(65,25,30))
                setOnClickListener {
                    if (label=="Close menu") toggleMenu()
                    else Toast.makeText(this@OverlayService, label, Toast.LENGTH_SHORT).show()
                }
            })
        }
        current.addView(panel)
        expanded = true
    }

    override fun onDestroy() {
        root?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        root = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
