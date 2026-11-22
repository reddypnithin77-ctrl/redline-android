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
        header.addView(TextView(this).apply { text="REDLINE  /  COMPANION"; textSize=23f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        header.addView(TextView(this).apply { text="●  READY TO RUN"; textSize=11f; letterSpacing=.14f; setTextColor(red); setPadding(0,dp(6),0,0) })
        root.addView(header,LinearLayout.LayoutParams(-1,dp(104)).apply { setMargins(dp(14),dp(14),dp(14),dp(8)) })
        content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(18),dp(8),dp(18),dp(12)) }
        root.addView(content,LinearLayout.LayoutParams(-1,0,1f))
        nav=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER; setPadding(dp(8),dp(8),dp(8),dp(12)); background=gradient(Color.rgb(28,20,27),Color.rgb(12,12,17),dp(22).toFloat()); elevation=dp(18).toFloat() }
        root.addView(nav,LinearLayout.LayoutParams(-1,dp(76)).apply { setMargins(dp(12),dp(4),dp(12),dp(12)) })
        renderTab()
        setContentView(root)
    }

    private fun renderTab() {
        content.removeAllViews(); nav.removeAllViews()
        val tabs=listOf("HOME" to "⌂","FEATURES" to "✦","SUPPORT" to "?") 
        tabs.forEach { (name, glyph) ->
            val active=currentTab==name
            val b=TextView(this).apply { text="$glyph\n$name"; gravity=Gravity.CENTER; textSize=if(active)12f else 10f; typeface=if(active)Typeface.DEFAULT_BOLD else Typeface.DEFAULT; setTextColor(if(active) Color.WHITE else Color.GRAY); background=if(active) gradient(Color.rgb(210,22,48),Color.rgb(105,8,27),dp(16).toFloat()) else null; setPadding(dp(10),dp(7),dp(10),dp(7)); setOnClickListener { currentTab=name; renderTab() } }
            nav.addView(b,LinearLayout.LayoutParams(0,-1,1f).apply { setMargins(dp(4),0,dp(4),0) })
        }
        when(currentTab) {
            "HOME" -> {
                heading("CONTROL CENTER","Your companion dashboard")
                infoCard("FLOATING WINDOW","Open REDLINE controls over other apps","Start the draggable R bubble and access session tools.") {
                    if(!Settings.canDrawOverlays(this)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))
                    else { val i=Intent(this,OverlayService::class.java); if(Build.VERSION.SDK_INT>=26) startForegroundService(i) else startService(i); Toast.makeText(this,"Floating controls starting",Toast.LENGTH_SHORT).show() }
                }
                infoCard("SESSION TRACKER","Track your practice time","Start, pause and reset a simple session timer.") { currentTab="FEATURES"; renderTab() }
                infoCard("QUICK STATUS","Standalone companion mode","REDLINE does not inject into or modify any game.") {}
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

    private fun heading(title:String, subtitle:String) {
        content.addView(TextView(this).apply { text=title; textSize=21f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(0,dp(12),0,dp(4)) })
        content.addView(TextView(this).apply { text=subtitle; textSize=12f; setTextColor(Color.GRAY); setPadding(0,0,0,dp(16)) })
    }
    private fun infoCard(title:String, subtitle:String, detail:String, action:()->Unit) {
        val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(16),dp(14),dp(16),dp(14)); background=gradient(Color.rgb(38,35,45),Color.rgb(19,19,26),dp(18).toFloat()); elevation=dp(7).toFloat(); isClickable=true; isFocusable=true }
        card.addView(TextView(this).apply { text=title; textSize=10f; letterSpacing=.14f; typeface=Typeface.DEFAULT_BOLD; setTextColor(red) })
        card.addView(TextView(this).apply { text=subtitle; textSize=16f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(0,dp(7),0,dp(4)) })
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
