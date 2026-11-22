package com.redline.companion

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var root: LinearLayout? = null
    private var panel: LinearLayout? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var expanded = false
    private var tab = "HOME"
    private val channelId = "redline_overlay"
    private var presetIndex = 0
    private val presets = listOf("Balanced", "Low", "Fast")
    private val controlNames = listOf("Silent Aim", "ESP Line", "Distance", "Headshot Assist", "Target Indicator", "Radar Pulse", "Recoil Guide", "Quick Switch")

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun bg(a: Int, b: Int, r: Float) =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(a, b)).apply { cornerRadius = r }
    private fun type() = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE

    override fun onCreate() {
        super.onCreate()
        try {
            if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(channelId, "REDLINE overlay", NotificationManager.IMPORTANCE_LOW))
            val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val nb = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, channelId) else Notification.Builder(this)
            startForeground(7, nb.setContentTitle("REDLINE is running")
                .setContentText("Gaming companion controls active")
                .setSmallIcon(android.R.drawable.ic_media_play).setContentIntent(pi).setOngoing(true).build())
            wm = getSystemService(WINDOW_SERVICE) as WindowManager
            presetIndex = getSharedPreferences("redline", MODE_PRIVATE).getInt("preset", 0).coerceIn(0, 2)
            showBubble()
        } catch (e: Exception) {
            Toast.makeText(this, "Overlay error: ${e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun showBubble() {
        val bubble = ImageView(this).apply {
            setImageResource(R.drawable.redline_logo)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = bg(Color.rgb(255, 55, 78), Color.rgb(75, 3, 22), dp(40).toFloat())
            setPadding(dp(4), dp(4), dp(4), dp(4))
            elevation = dp(18).toFloat()
            contentDescription = "Open REDLINE menu"
        }
        val p = WindowManager.LayoutParams(dp(62), dp(62), type(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(18); y = dp(220)
        }
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(bubble) }
        windowParams = p
        bubble.setOnTouchListener(object : View.OnTouchListener {
            var sx = 0f; var sy = 0f; var px = 0; var py = 0; var moved = false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; px = p.x; py = p.y; moved = false; return true }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (e.rawX - sx).toInt(); val dy = (e.rawY - sy).toInt()
                        if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > dp(7)) moved = true
                        p.x = px + dx; p.y = py + dy
                        root?.let { wm.updateViewLayout(it, p) }
                        return true
                    }
                    MotionEvent.ACTION_UP -> { if (!moved) toggle(); return true }
                }
                return false
            }
        })
        wm.addView(root, p)
    }

    private fun toggle() {
        val r = root ?: return
        val p = windowParams ?: return
        if (expanded) {
            if (r.childCount > 1) r.removeViews(1, r.childCount - 1)
            panel = null
            expanded = false
            p.width = dp(62)
            p.height = dp(62)
        } else {
            panel = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(11), dp(12), dp(10))
                background = bg(Color.rgb(49, 24, 39), Color.rgb(10, 10, 16), dp(22).toFloat())
                elevation = dp(24).toFloat()
            }
            r.addView(panel, LinearLayout.LayoutParams(dp(260), dp(370)).apply { topMargin = dp(8) })
            expanded = true
            // Grow the WindowManager surface so Android does not clip the panel below the bubble.
            p.width = dp(280)
            p.height = dp(450)
            renderPanel()
        }
        wm.updateViewLayout(r, p)
    }

    private fun renderPanel() {
        val box = panel ?: return
        box.removeAllViews()

        val brand = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        brand.addView(ImageView(this).apply {
            setImageResource(R.drawable.redline_logo); scaleType = ImageView.ScaleType.FIT_CENTER
        }, LinearLayout.LayoutParams(dp(38), dp(38)))
        val brandText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(9), 0, 0, 0) }
        brandText.addView(TextView(this).apply {
            text = "REDLINE"; textSize = 17f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); letterSpacing = .1f
        })
        brandText.addView(TextView(this).apply {
            text = "GAMING COMPANION"; textSize = 8f; letterSpacing = .13f
            setTextColor(Color.rgb(255, 91, 111))
        })
        brand.addView(brandText, LinearLayout.LayoutParams(0, -2, 1f))
        brand.addView(TextView(this).apply {
            text = "● LIVE"; textSize = 9f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(105, 235, 165)); gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = bg(Color.rgb(20, 65, 48), Color.rgb(14, 36, 31), dp(12).toFloat())
        })
        box.addView(brand)
        box.addView(View(this).apply { setBackgroundColor(Color.rgb(86, 42, 58)) },
            LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(9); bottomMargin = dp(5) })

        val scroll = ScrollView(this).apply { isFillViewport = false; overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(3), 0, dp(5)) }
        scroll.addView(body)
        box.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        if (tab == "HOME") {
            label(body, "QUICK ACCESS")
            button(body, "AIM TRAINING DRILLS", "Practice tracking, flick and target-switch drills.") {
                Toast.makeText(this, "Practice these drills in the game's training area.", Toast.LENGTH_LONG).show()
            }
            button(body, "DISTANCE REFERENCE", "Manual range-estimation tips") {
                Toast.makeText(this, "Use in-game landmarks and the map scale to estimate range manually.", Toast.LENGTH_LONG).show()
            }
            button(body, "SENSITIVITY: ${presets[presetIndex].uppercase()}", "Tap to cycle your saved reference preset") {
                presetIndex = (presetIndex + 1) % presets.size
                getSharedPreferences("redline", MODE_PRIVATE).edit().putInt("preset", presetIndex).apply()
                renderPanel()
            }
            button(body, "MAP AWARENESS TIPS", "Minimap, cover and landmark reminders") {
                Toast.makeText(this, "Check the in-game minimap, cover and visible landmarks.", Toast.LENGTH_LONG).show()
            }
        } else {
            label(body, "FEATURES")
            body.addView(TextView(this).apply {
                text = "Visual preferences only • no game access"
                textSize = 9f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(7))
            })
            controlNames.chunked(2).forEach { pair ->
                val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
                pair.forEach { name -> controlSwitch(name, line) }
                if (pair.size == 1) line.addView(View(this), LinearLayout.LayoutParams(0, dp(1), 1f))
                body.addView(line, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
            }
        }

        box.addView(View(this).apply { setBackgroundColor(Color.rgb(86, 42, 58)) },
            LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(5); bottomMargin = dp(7) })
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        listOf("HOME" to "⌂", "FEATURES" to "✦").forEach { (name, glyph) ->
            val active = name == tab
            val item = TextView(this).apply {
                text = "$glyph   $name"; gravity = Gravity.CENTER; textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (active) Color.WHITE else Color.LTGRAY)
                background = if (active) bg(Color.rgb(218, 28, 58), Color.rgb(112, 7, 31), dp(13).toFloat()) else null
                setPadding(dp(8), dp(9), dp(8), dp(9))
                setOnClickListener { tab = name; renderPanel() }
            }
            nav.addView(item, LinearLayout.LayoutParams(0, dp(40), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        box.addView(nav)
    }

    private fun controlSwitch(name: String, parent: LinearLayout) {
        val prefs = getSharedPreferences("redline_controls", MODE_PRIVATE)
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(4), dp(5), dp(4), dp(3))
            background = bg(Color.rgb(78, 28, 49), Color.rgb(25, 16, 29), dp(14).toFloat())
            elevation = dp(4).toFloat()
        }
        tile.addView(TextView(this).apply {
            text = name; textSize = 10f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); gravity = Gravity.CENTER; maxLines = 2
        }, LinearLayout.LayoutParams(-1, dp(29)))
        val toggle = Switch(this).apply {
            isChecked = prefs.getBoolean(name, false); showText = false
            scaleX = .78f; scaleY = .78f; gravity = Gravity.CENTER
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(name, checked).apply()
                Toast.makeText(this@OverlayService, "$name ${if (checked) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
            }
        }
        tile.addView(toggle)
        parent.addView(tile, LinearLayout.LayoutParams(0, dp(69), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
    }

    private fun label(parent: LinearLayout, value: String) {
        parent.addView(TextView(this).apply {
            text = value; textSize = 10f; letterSpacing = .13f
            typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER; setPadding(0, dp(6), 0, dp(4))
        })
    }

    private fun button(parent: LinearLayout, title: String, subtitle: String, action: () -> Unit) {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10))
            background = bg(Color.rgb(105, 27, 49), Color.rgb(35, 17, 31), dp(14).toFloat())
            elevation = dp(4).toFloat(); isClickable = true; isFocusable = true
        }
        tile.addView(TextView(this).apply {
            text = title; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
        })
        tile.addView(TextView(this).apply {
            text = subtitle; textSize = 9f; setTextColor(Color.rgb(205, 184, 194)); setPadding(0, dp(3), 0, 0)
        })
        tile.setOnClickListener { action() }
        parent.addView(tile, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_NOT_STICKY
    override fun onDestroy() {
        root?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        root = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
