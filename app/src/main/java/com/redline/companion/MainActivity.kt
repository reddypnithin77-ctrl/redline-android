package com.redline.companion

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.security.MessageDigest

class MainActivity : Activity() {
    private val accessPasswordHash = "e000eb1cce3bdb4f8aad29d14380616d2d82a7d57212345659f05fd7aa16bc24"
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout
    private var currentTab = "HOME"
    private var overlayEnabled = false
    private var homePowerButton: TextView? = null
    private val red = Color.rgb(244, 35,  sixty())
    private val bgColor = Color.rgb(9, 9, 13)
    private val panelColor = Color.rgb(25, 25, 33)

    private fun sixty() = 60
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun gradient(c1: Int, c2: Int, radius: Float): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c1, c2)).apply { cornerRadius = radius }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); showLogin() }

    private fun showLogin() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(28), dp(30), dp(28), dp(30)); background = gradient(Color.rgb(30,8,14), bgColor, 0f) }
        root.addView(TextView(this).apply { text="REDLINE"; textSize=36f; typeface=Typeface.DEFAULT_BOLD; setTextColor(red); gravity=Gravity.CENTER })
        root.addView(TextView(this).apply { text="GAMING COMPANION  •  ANDROID 15"; textSize=11f; letterSpacing=.18f; setTextColor(Color.LTGRAY); gravity=Gravity.CENTER; setPadding(0,dp(8),0,dp(30)) })
        val field = EditText(this).apply { hint="Enter access password"; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setPadding(dp(16),dp(10),dp(16),dp(10)); background=gradient(panelColor,Color.rgb(15,15,20),dp(16).toFloat()) }
        root.addView(field, LinearLayout.LayoutParams(-1,dp(56)))
        root.addView(actionButton("UNLOCK REDLINE") {
            val hash=MessageDigest.getInstance("SHA-256").digest(field.text.toString().toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            if(hash==accessPasswordHash) showDashboard() else { field.text.clear(); field.error="Incorrect password" }
        }, LinearLayout.LayoutParams(-1,dp(54)).apply { topMargin=dp(18) })
        root.addView(TextView(this).apply { text="Authorized access only"; setTextColor(Color.GRAY); textSize=12f; gravity=Gravity.CENTER; setPadding(0,dp(24),0,0) })
        setContentView(root)
    }

    private fun showDashboard() {
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(bgColor) }
        val header=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(22),dp(20),dp(16)); background=gradient(Color.rgb(55,9,20),Color.rgb(16,12,18),dp(22).toFloat()); elevation=dp(8).toFloat() }
        val brandRow=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        brandRow.addView(TextView(this).apply { text="R"; textSize=30f; typeface=Typeface.create("sans-serif-black",Typeface.BOLD); gravity=Gravity.CENTER; setTextColor(Color.WHITE); background=gradient(Color.rgb(255,45,68),Color.rgb(100,4,25),dp(50).toFloat()); elevation=dp(12).toFloat() },LinearLayout.LayoutParams(dp(58),dp(58)))
        val brandText=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(12),0,0,0) }
        brandText.addView(TextView(this).apply { text="REDLINE"; textSize=24f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); letterSpacing=.08f })
        brandText.addView(TextView(this).apply { text="GAMING COMPANION"; textSize=10f; letterSpacing=.16f; setTextColor(red) })
        brandRow.addView(brandText)
        header.addView(brandRow)
        header.addView(TextView(this).apply { text="●  READY TO RUN"; textSize=11f; letterSpacing=.14f; setTextColor(red); setPadding(0,dp(6),0,0) })
        root.addView(header,LinearLayout.LayoutParams(-1,dp(112)).apply { setMargins(dp(14),dp(14),dp(14),dp(8)) })
        content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(18),dp(8),dp(18),dp(12)) }
        root.addView(content,LinearLayout.LayoutParams(-1,0,1f))
        nav=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER; setPadding(dp(8),dp(8),dp(8),dp(12)); background=gradient(Color.rgb(28,20,27),Color.rgb(12,12,17),dp(22).toFloat()); elevation=dp(18).toFloat() }
        root.addView(nav,LinearLayout.LayoutParams(-1,dp(76)).apply { setMargins(dp(12),dp(4),dp(12),dp(12)) })
        renderTab()
        setContentView(root)
    }

    private fun renderTab() {
        content.removeAllViews(); nav.removeAllViews()
        val tabs=listOf("HOME" to "⌂","SUPPORT" to "?") 
        tabs.forEach { (name, glyph) ->
            val active=currentTab==name
            val b=TextView(this).apply { text="$glyph\n$name"; gravity=Gravity.CENTER; textSize=if(active)12f else 10f; typeface=if(active)Typeface.DEFAULT_BOLD else Typeface.DEFAULT; setTextColor(if(active) Color.WHITE else Color.GRAY); background=if(active) gradient(Color.rgb(210,22,48),Color.rgb(105,8,27),dp(16).toFloat()) else null; setPadding(dp(10),dp(10),dp(10),dp(10)); minHeight=dp(54); setOnClickListener { currentTab=name; renderTab() } }
            nav.addView(b,LinearLayout.LayoutParams(0,-1,1f).apply { setMargins(dp(4),0,dp(4),0) })
        }
        when(currentTab) {
            "HOME" -> {
                val wrap=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(dp(8),dp(12),dp(8),dp(12)) }
                val glow=TextView(this).apply {
                    text="⏻"
                    textSize=58f
                    typeface=Typeface.DEFAULT_BOLD
                    gravity=Gravity.CENTER
                    setTextColor(Color.WHITE)
                    background=gradient(Color.rgb(255,55,78),Color.rgb(105,3,27),dp(100).toFloat())
                    elevation=dp(22).toFloat()
                    isClickable=true
                    isFocusable=true
                    setOnClickListener {
                        if(!overlayEnabled) {
                            if(!Settings.canDrawOverlays(this@MainActivity)) {
                                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))
                                Toast.makeText(this@MainActivity,"Allow Display over other apps, then tap the power button again.",Toast.LENGTH_LONG).show()
                            } else {
                                val i=Intent(this@MainActivity,OverlayService::class.java)
                                if(Build.VERSION.SDK_INT>=26) startForegroundService(i) else startService(i)
                                overlayEnabled=true
                                updatePowerButton()
                            }
                        } else {
                            stopService(Intent(this@MainActivity,OverlayService::class.java))
                            overlayEnabled=false
                            updatePowerButton()
                        }
                    }
                }
                homePowerButton=glow
                wrap.addView(glow,LinearLayout.LayoutParams(dp(190),dp(190)))
                content.addView(wrap,LinearLayout.LayoutParams(-1,-1))
            }
            "FEATURES" -> {
                heading("FEATURES","Useful tools • no game modification")
                infoCard("SESSION TIMER","Start / pause / reset","Track your training session from the floating panel.") { startOverlay() }
                infoCard("SENSITIVITY NOTES","Balanced • Low • Fast","Save a personal reference preset; it does not alter game settings.") { startOverlay() }
                infoCard("HUD CHECKLIST","Comfort and layout tips","Review simple practice and control-layout reminders.") { startOverlay() }
                infoCard("FLOATING SHORTCUT","Draggable red R bubble","Open the compact overlay while using other apps.") { startOverlay() }
            }
            else -> {
                heading("SUPPORT","Need a hand?")
                infoCard("PERMISSIONS","Overlay permission help","Allow REDLINE to display over other apps in Android settings.") { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName"))) }
                infoCard("HOW TO USE","1. Allow overlay  2. Start floating menu","Tap the red R bubble to expand or collapse the mini panel.") { startOverlay() }
                infoCard("PROJECT SOURCE","GitHub repository","Open the REDLINE Android project.") { startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/reddypnithin77-ctrl/redline-android"))) }
                infoCard("SAFETY","Companion utility only","No ESP, aimbot, exploits, injection, or game-memory access.") {}
            }
        }
    }

    private fun updatePowerButton() {
        homePowerButton?.apply {
            text=if(overlayEnabled) "⏻\nON" else "⏻"
            background=if(overlayEnabled) gradient(Color.rgb(35,205,125),Color.rgb(5,95,61),dp(100).toFloat()) else gradient(Color.rgb(255,55,78),Color.rgb(105,3,27),dp(100).toFloat())
        }
    }
    override fun onResume() { super.onResume(); if(::content.isInitialized && currentTab=="HOME" && !overlayEnabled && Settings.canDrawOverlays(this)) { /* ready for the next tap */ } }

    private fun heading(title:String, subtitle:String) {
        content.addView(TextView(this).apply { text=title; textSize=21f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(0,dp(12),0,dp(4)) })
        content.addView(TextView(this).apply { text=subtitle; textSize=12f; setTextColor(Color.GRAY); setPadding(0,0,0,dp(16)) })
    }
    private fun infoCard(title:String, subtitle:String, detail:String, action:()->Unit) {
        val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(18),dp(17),dp(18),dp(17)); background=gradient(Color.rgb(38,35,45),Color.rgb(19,19,26),dp(18).toFloat()); elevation=dp(7).toFloat(); isClickable=true; isFocusable=true }
        card.addView(TextView(this).apply { text=title; textSize=11f; letterSpacing=.14f; typeface=Typeface.DEFAULT_BOLD; setTextColor(red) })
        card.addView(TextView(this).apply { text=subtitle; textSize=17f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(0,dp(7),0,dp(4)) })
        card.addView(TextView(this).apply { text=detail; textSize=12f; setTextColor(Color.LTGRAY) })
        card.setOnClickListener { card.animate().scaleX(.975f).scaleY(.975f).setDuration(75).withEndAction { card.animate().scaleX(1f).scaleY(1f).setDuration(100).start(); action() }.start() }
        content.addView(card,LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin=dp(12) })
    }
    private fun actionButton(label:String, action:()->Unit)=Button(this).apply { text=label; isAllCaps=false; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); background=gradient(Color.rgb(255,45,67),Color.rgb(155,8,34),dp(16).toFloat()); elevation=dp(8).toFloat(); setOnClickListener { action() } }
    private fun startOverlay() {
        if(!Settings.canDrawOverlays(this)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))
        else { val i=Intent(this,OverlayService::class.java); if(Build.VERSION.SDK_INT>=26) startForegroundService(i) else startService(i) }
    }
}
