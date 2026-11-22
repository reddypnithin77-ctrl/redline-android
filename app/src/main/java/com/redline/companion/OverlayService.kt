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
    private var panelScroll: ScrollView? = null
    private var pendingScrollY = 0
    private val presets = listOf("Balanced", "Low", "Fast")
    private val controls = listOf(
        "Aim Drill", "Line-of-Sight Trainer",
        "Distance Practice", "Recoil Coach",
        "Sensitivity Lab", "Reaction Trainer",
        "Sound Awareness", "HUD Planner",
        "Movement Trainer", "Crosshair Placement",
        "Map Knowledge", "Session Analytics"
    )

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
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = bg(Color.rgb(34, 19, 30), Color.rgb(8, 9, 14), dp(24).toFloat())
                elevation = dp(24).toFloat()
            }
            r.addView(panel, LinearLayout.LayoutParams(dp(292), dp(500)).apply { topMargin = dp(8) })
            expanded = true
            p.width = dp(310)
            p.height = dp(570)
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
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        val brandText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        brandText.addView(TextView(this).apply {
            text = "REDLINE"; textSize = 19f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); letterSpacing = .08f
        })
        brandText.addView(TextView(this).apply {
            text = "TACTICAL TRAINING COMPANION"; textSize = 8f; letterSpacing = .12f
            setTextColor(Color.rgb(255, 91, 111))
        })
        brand.addView(brandText, LinearLayout.LayoutParams(0, -2, 1f))
        val close = TextView(this).apply {
            text = "×"; textSize = 25f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); background = bg(Color.rgb(75, 27, 43), Color.rgb(35, 15, 27), dp(12).toFloat())
            setOnClickListener { toggle() }
        }
        brand.addView(close, LinearLayout.LayoutParams(dp(38), dp(38)))
        box.addView(brand)

        val status = TextView(this).apply {
            text = "●  COMPANION ACTIVE     •     PRESET: ${presets[presetIndex].uppercase()}"
            textSize = 8f; letterSpacing = .04f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(110, 235, 170))
            setPadding(dp(3), dp(9), dp(3), dp(9))
        }
        box.addView(status)

        // Tabs are deliberately placed at the top, always visible without scrolling.
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            background = bg(Color.rgb(23, 20, 28), Color.rgb(15, 13, 20), dp(15).toFloat())
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        listOf("HOME" to "⌂", "FEATURES" to "✦").forEach { (name, glyph) ->
            val active = tab == name
            val item = TextView(this).apply {
                text = "$glyph   $name"; gravity = Gravity.CENTER; textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (active) Color.WHITE else Color.rgb(165, 155, 166))
                background = if (active) bg(Color.rgb(225, 35, 65), Color.rgb(113, 8, 33), dp(12).toFloat()) else null
                setPadding(dp(8), dp(12), dp(8), dp(12))
                setOnClickListener { tab = name; renderPanel() }
            }
            nav.addView(item, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        box.addView(nav)

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            clipToPadding = false
            setPadding(0, dp(8), 0, dp(4))
        }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        panelScroll = scroll
        box.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        scroll.post { scroll.scrollTo(0, pendingScrollY) }

        if (tab == "HOME") {
            sectionLabel(body, "COMMAND CENTER")
            val hero = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(15), dp(15), dp(15), dp(15))
                background = bg(Color.rgb(91, 20, 43), Color.rgb(29, 17, 31), dp(18).toFloat())
            }
            hero.addView(TextView(this).apply {
                text = "READY FOR YOUR NEXT SESSION"; textSize = 13f
                typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
            })
            hero.addView(TextView(this).apply {
                text = "Training tools, quick references and personal setup notes in one place."
                textSize = 10f; setTextColor(Color.rgb(218, 194, 204)); setPadding(0, dp(6), 0, 0)
            })
            body.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
            actionCard(body, "SENSITIVITY PRESET", presets[presetIndex].uppercase(), "Tap to cycle Balanced / Low / Fast") {
                presetIndex = (presetIndex + 1) % presets.size
                getSharedPreferences("redline", MODE_PRIVATE).edit().putInt("preset", presetIndex).apply()
                renderPanel()
            }
            actionCard(body, "PRACTICE ROUTINE", "Aim • tracking • movement", "Open a simple practice checklist") {
                Toast.makeText(this, "Practice tracking, flicks and movement in the training area.", Toast.LENGTH_LONG).show()
            }
            actionCard(body, "SESSION NOTES", "Keep your setup consistent", "Use Quick Notes in Features to track changes") {
                tab = "FEATURES"; renderPanel()
            }
        } else {
            sectionLabel(body, "PERSONAL TOOLKIT")
            body.addView(TextView(this).apply {
                text = "Tap a tile or switch to enable its companion setting. No enemy tracking, game injection or game-data access."
                textSize = 9f; setTextColor(Color.rgb(183, 172, 185))
                setPadding(dp(2), 0, dp(2), dp(8))
            })
            controls.chunked(2).forEach { pair ->
                val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
                pair.forEach { controlTile(it, line) }
                body.addView(line, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            }
        }
        box.addView(TextView(this).apply {
            text = "REDLINE  •  COMPANION MODE ONLY"
            textSize = 8f; letterSpacing = .1f; gravity = Gravity.CENTER
            setTextColor(Color.rgb(130, 112, 126)); setPadding(0, dp(8), 0, 0)
        })
    }

    private fun controlTile(name: String, parent: LinearLayout) {
        val prefs = getSharedPreferences("redline_controls", MODE_PRIVATE)
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(8))
            background = bg(Color.rgb(54, 28, 44), Color.rgb(22, 17, 27), dp(15).toFloat())
            isClickable = true; isFocusable = true
        }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(TextView(this).apply {
            text = "✦"; textSize = 14f; setTextColor(Color.rgb(255, 75, 100))
        }, LinearLayout.LayoutParams(dp(20), -2))
        head.addView(TextView(this).apply {
            text = name; textSize = 10f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); maxLines = 2
        }, LinearLayout.LayoutParams(0, dp(34), 1f))
        tile.addView(head)
        val enabled = prefs.getBoolean(name, false)
        tile.addView(TextView(this).apply {
            text = if (enabled) "●  ENABLED" else "○  DISABLED"
            textSize = 8f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (enabled) Color.rgb(104, 235, 160) else Color.rgb(160, 148, 160))
            setPadding(dp(20), dp(4), 0, 0)
        })
        tile.setOnClickListener {
            val next = !prefs.getBoolean(name, false)
            pendingScrollY = panelScroll?.scrollY ?: 0
            prefs.edit().putBoolean(name, next).apply()
            Toast.makeText(this, "$name ${if (next) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
            renderPanel()
        }
        parent.addView(tile, LinearLayout.LayoutParams(0, dp(88), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
    }

    private fun sectionLabel(parent: LinearLayout, value: String) {
        parent.addView(TextView(this).apply {
            text = value; textSize = 9f; letterSpacing = .16f
            typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(255, 105, 125))
            setPadding(dp(2), dp(3), 0, dp(8))
        })
    }

    private fun actionCard(parent: LinearLayout, title: String, subtitle: String, detail: String, action: () -> Unit) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(11), dp(13), dp(11))
            background = bg(Color.rgb(54, 28, 44), Color.rgb(22, 17, 27), dp(15).toFloat())
            isClickable = true; isFocusable = true
        }
        card.addView(TextView(this).apply {
            text = title; textSize = 8f; letterSpacing = .12f
            typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(255, 105, 125))
        })
        card.addView(TextView(this).apply {
            text = subtitle; textSize = 12f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); setPadding(0, dp(4), 0, dp(3))
        })
        card.addView(TextView(this).apply {
            text = detail; textSize = 9f; setTextColor(Color.rgb(190, 178, 190))
        })
        card.setOnClickListener {
            card.animate().scaleX(.98f).scaleY(.98f).setDuration(70).withEndAction {
                card.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                action()
            }.start()
        }
        parent.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_NOT_STICKY
    override fun onDestroy() {
        root?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        root = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
