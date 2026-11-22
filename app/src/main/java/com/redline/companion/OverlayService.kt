package com.redline.companion

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.widget.*
import android.graphics.drawable.GradientDrawable

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var root: LinearLayout? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var expanded = false
    private val channelId = "redline_overlay"
    private val handler = Handler(Looper.getMainLooper())
    private var elapsedSeconds = 0L
    private var timerRunning = false
    private var timerLabel: TextView? = null
    private val presets = listOf("Balanced", "Low recoil practice", "Fast swipe")
    private var presetIndex = 0
    private val timerTick = object : Runnable {
        override fun run() {
            if (!timerRunning) return
            elapsedSeconds++
            updateTimerLabel()
            handler.postDelayed(this, 1000L)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun formatTime(total: Long): String {
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }
    private fun updateTimerLabel() {
        timerLabel?.text = "SESSION  " + formatTime(elapsedSeconds) + if (timerRunning) "  • RUNNING" else "  • PAUSED"
    }

    override fun onCreate() {
        super.onCreate()
        try {
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
            presetIndex = getSharedPreferences("redline", MODE_PRIVATE).getInt("preset", 0).coerceIn(0, presets.lastIndex)
            showBubble()
        } catch (e: Exception) {
            Toast.makeText(this, "REDLINE overlay error: ${e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun bg(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private fun styledButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(Color.WHITE)
        backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(75, 25, 32))
        setOnClickListener { action() }
    }

    private fun showBubble() {
        val bubble = TextView(this).apply {
            text = "R"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = bg(Color.rgb(190, 18, 35), dp(50).toFloat())
            elevation = dp(12).toFloat()
        }
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(24)
            y = dp(220)
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(bubble, LinearLayout.LayoutParams(dp(58), dp(58)))
        }
        windowParams = params
        bubble.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f
            var downY = 0f
            var startX = 0
            var startY = 0
            var moved = false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = e.rawX; downY = e.rawY
                        startX = params.x; startY = params.y
                        moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (e.rawX - downX).toInt()
                        val dy = (e.rawY - downY).toInt()
                        if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > dp(8)) moved = true
                        params.x = startX + dx
                        params.y = startY + dy
                        root?.let { wm.updateViewLayout(it, params) }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) toggleMenu()
                        return true
                    }
                }
                return false
            }
        })
        wm.addView(root, params)
    }

    private fun toggleMenu() {
        val current = root ?: return
        val params = windowParams ?: return
        if (expanded) {
            if (current.childCount > 1) current.removeViews(1, current.childCount - 1)
            expanded = false
        } else {
            val panel = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = bg(Color.rgb(22, 22, 26), dp(18).toFloat())
                elevation = dp(14).toFloat()
            }
            panel.addView(TextView(this).apply {
                text = "REDLINE"
                textSize = 16f
                setTextColor(Color.rgb(255, 55, 65))
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(6))
            })
            timerLabel = TextView(this).apply {
                textSize = 13f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, dp(5), 0, dp(5))
            }
            updateTimerLabel()
            panel.addView(timerLabel)
            panel.addView(styledButton(if (timerRunning) "Pause session timer" else "Start / resume timer") {
                if (timerRunning) {
                    timerRunning = false
                    handler.removeCallbacks(timerTick)
                } else {
                    timerRunning = true
                    handler.postDelayed(timerTick, 1000L)
                }
                updateTimerLabel()
                refreshTimerButton(panel)
            })
            panel.addView(styledButton("Reset session timer") {
                timerRunning = false
                handler.removeCallbacks(timerTick)
                elapsedSeconds = 0L
                updateTimerLabel()
                refreshTimerButton(panel)
            })
            panel.addView(styledButton("Sensitivity preset: ${presets[presetIndex]}") {
                presetIndex = (presetIndex + 1) % presets.size
                getSharedPreferences("redline", MODE_PRIVATE).edit().putInt("preset", presetIndex).apply()
                Toast.makeText(this, "Saved preset: ${presets[presetIndex]}. This is a reference only; it does not change game settings.", Toast.LENGTH_LONG).show()
                refreshPresetButton(panel)
            })
            panel.addView(styledButton("HUD & practice guide") {
                Toast.makeText(this, "HUD tip: keep fire and aim controls comfortably reachable; adjust sensitivity in the game's own settings and test changes in training mode.", Toast.LENGTH_LONG).show()
            })
            panel.addView(styledButton("Close menu") { toggleMenu() })
            current.addView(panel, LinearLayout.LayoutParams(dp(230), WindowManager.LayoutParams.WRAP_CONTENT))
            expanded = true
        }
        try {
            wm.updateViewLayout(current, params)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not resize REDLINE menu: ${e.javaClass.simpleName}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshTimerButton(panel: LinearLayout) {
        // Timer state is visible in the status line; reopening the menu refreshes the action label.
        updateTimerLabel()
    }
    private fun refreshPresetButton(panel: LinearLayout) {
        // The chosen preset is persisted and shown in a confirmation toast.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onDestroy() {
        timerRunning = false
        handler.removeCallbacks(timerTick)
        root?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        root = null
        windowParams = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
