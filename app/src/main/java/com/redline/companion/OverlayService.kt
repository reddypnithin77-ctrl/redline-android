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
    private var expanded=false
    private var tab="HOME"
    private val channelId="redline_overlay"
    private var presetIndex=0
    private val presets=listOf("Balanced","Low","Fast")
    private val controlNames=listOf("Silent Aim","ESP Line","Distance","Headshot Assist","Target Indicator","Radar Pulse","Recoil Guide","Quick Switch")
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun bg(a:Int,b:Int,r:Float)=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(a,b)).apply{cornerRadius=r}
    private fun type()=if(Build.VERSION.SDK_INT>=26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE

    override fun onCreate(){
        super.onCreate()
        try{
            if(Build.VERSION.SDK_INT>=26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(channelId,"REDLINE overlay",NotificationManager.IMPORTANCE_LOW))
            val pi=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val nb=if(Build.VERSION.SDK_INT>=26) Notification.Builder(this,channelId) else Notification.Builder(this)
            startForeground(7,nb.setContentTitle("REDLINE is running").setContentText("Gaming companion controls active").setSmallIcon(android.R.drawable.ic_media_play).setContentIntent(pi).setOngoing(true).build())
            wm=getSystemService(WINDOW_SERVICE) as WindowManager
            val prefs=getSharedPreferences("redline",MODE_PRIVATE)
            presetIndex=prefs.getInt("preset",0).coerceIn(0,2)
            showBubble()
        }catch(e:Exception){Toast.makeText(this,"Overlay error: ${e.javaClass.simpleName}",Toast.LENGTH_LONG).show();stopSelf()}
    }

    private fun showBubble(){
        val bubble=TextView(this).apply{text="♛\nR";textSize=18f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=bg(Color.rgb(255,48,68),Color.rgb(125,5,28),dp(50).toFloat());elevation=dp(18).toFloat()}
        val p=WindowManager.LayoutParams(dp(280),WindowManager.LayoutParams.WRAP_CONTENT,type(),WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START;x=dp(18);y=dp(220)}
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;addView(bubble)}
        windowParams=p
        bubble.setOnTouchListener(object:View.OnTouchListener{
            var sx=0f;var sy=0f;var px=0;var py=0;var moved=false
            override fun onTouch(v:View,e:MotionEvent):Boolean{when(e.action){
                MotionEvent.ACTION_DOWN->{sx=e.rawX;sy=e.rawY;px=p.x;py=p.y;moved=false;return true}
                MotionEvent.ACTION_MOVE->{val dx=(e.rawX-sx).toInt();val dy=(e.rawY-sy).toInt();if(kotlin.math.abs(dx)+kotlin.math.abs(dy)>dp(7))moved=true;p.x=px+dx;p.y=py+dy;root?.let{wm.updateViewLayout(it,p)};return true}
                MotionEvent.ACTION_UP->{if(!moved)toggle();return true}
            };return false}
        })
        wm.addView(root,p)
    }

    private fun toggle(){
        val r=root?:return
        val p=windowParams?:return
        if(expanded){if(r.childCount>1)r.removeViews(1,r.childCount-1);panel=null;expanded=false}
        else{
            panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(12),dp(12),dp(10));background=bg(Color.rgb(42,34,44),Color.rgb(13,13,19),dp(20).toFloat());elevation=dp(20).toFloat()}
            r.addView(panel,LinearLayout.LayoutParams(dp(260),-2).apply{topMargin=dp(8)})
            expanded=true;renderPanel()
        }
        wm.updateViewLayout(r,p)
    }

    private fun renderPanel(){
        val box=panel?:return
        box.removeAllViews()
        box.addView(TextView(this).apply{text="REDLINE";textSize=19f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.rgb(255,55,75));gravity=Gravity.CENTER;letterSpacing=.12f})
        box.addView(TextView(this).apply{text="COMPANION  •  ANDROID 15";textSize=9f;setTextColor(Color.LTGRAY);gravity=Gravity.CENTER;setPadding(0,dp(3),0,dp(10))})
        when(tab){
            "HOME"->{
                label("QUICK ACCESS")
                button("AIM TRAINING DRILLS"){Toast.makeText(this,"Practice tracking, flick and target-switch drills in the game's training area.",Toast.LENGTH_LONG).show()}
                button("DISTANCE REFERENCE"){Toast.makeText(this,"Use in-game landmarks and the map scale to estimate range manually.",Toast.LENGTH_LONG).show()}
                button("SENSITIVITY: ${presets[presetIndex]}"){presetIndex=(presetIndex+1)%presets.size;getSharedPreferences("redline",MODE_PRIVATE).edit().putInt("preset",presetIndex).apply();renderPanel()}
                button("MAP AWARENESS TIPS"){Toast.makeText(this,"Check the in-game minimap, cover and visible landmarks.",Toast.LENGTH_LONG).show()}
            }
            "FEATURES"->{
                label("CONTROL PANEL")
                label("VISUAL SWITCHES")
                controlNames.forEach { controlSwitch(it) }
                box.addView(TextView(this).apply{text="Visual settings only • REDLINE does not connect to or modify game data";textSize=9f;gravity=Gravity.CENTER;textAlignment=View.TEXT_ALIGNMENT_CENTER;setTextColor(Color.LTGRAY);setPadding(dp(4),dp(10),dp(4),dp(4))})
            }
            else->{
                label("SUPPORT")
                button("OVERLAY PERMISSION HELP"){Toast.makeText(this,"Enable Display over other apps for REDLINE in Android settings.",Toast.LENGTH_LONG).show()}
                button("HOW TO USE"){Toast.makeText(this,"Drag the R bubble to move it. Tap once to expand or collapse.",Toast.LENGTH_LONG).show()}
                button("COMPANION MODE"){Toast.makeText(this,"REDLINE works independently and does not read or change game data.",Toast.LENGTH_LONG).show()}
            }
        }
        val divider=View(this).apply{setBackgroundColor(Color.rgb(75,45,57))}
        box.addView(divider,LinearLayout.LayoutParams(-1,dp(1)).apply{topMargin=dp(8);bottomMargin=dp(7)})
        val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        listOf("HOME","FEATURES","SUPPORT").forEach{t->
            val active=t==tab
            val item=TextView(this).apply{text=when(t){"HOME"->"⌂";"FEATURES"->"✦";else->"?"}+"\n"+t;gravity=Gravity.CENTER;textSize=9f;typeface=Typeface.DEFAULT_BOLD;setTextColor(if(active)Color.WHITE else Color.LTGRAY);if(active)background=bg(Color.rgb(205,22,49),Color.rgb(110,8,30),dp(10).toFloat());setPadding(dp(6),dp(8),dp(6),dp(8));minHeight=dp(48);setOnClickListener{tab=t;renderPanel()}}
            nav.addView(item,LinearLayout.LayoutParams(0,dp(42),1f).apply{setMargins(dp(2),0,dp(2),0)})
        }
        box.addView(nav)
    }

    private fun controlSwitch(name:String){
        val prefs=getSharedPreferences("redline_controls",MODE_PRIVATE)
        val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),dp(4),dp(8),dp(4));background=bg(Color.rgb(45,27,39),Color.rgb(27,19,29),dp(12).toFloat())}
        val title=TextView(this).apply{text=name;textSize=12f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)}
        row.addView(title,LinearLayout.LayoutParams(0,dp(42),1f))
        val toggle=Switch(this).apply{isChecked=prefs.getBoolean(name,false);showText=false;scaleX=.9f;scaleY=.9f;setOnCheckedChangeListener{_,checked->prefs.edit().putBoolean(name,checked).apply();Toast.makeText(this@OverlayService,"$name ${if(checked)"ON" else "OFF"}",Toast.LENGTH_SHORT).show()}}
        row.addView(toggle)
        panel?.addView(row,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
    }

    private fun label(s:String){panel?.addView(TextView(this).apply{text=s;textSize=10f;letterSpacing=.12f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.LTGRAY);gravity=Gravity.CENTER;setPadding(0,dp(8),0,dp(5))})}
    private fun button(s:String,action:()->Unit){panel?.addView(TextView(this).apply{text=s;textSize=12f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=bg(Color.rgb(105,24,46),Color.rgb(38,18,31),dp(14).toFloat());elevation=dp(7).toFloat();setPadding(dp(10),dp(15),dp(10),dp(15));minHeight=dp(50);setOnClickListener{animate().scaleX(.97f).scaleY(.97f).setDuration(70).withEndAction{animate().scaleX(1f).scaleY(1f).setDuration(80).start();action()}.start()}},LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6)})}

    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int)=START_NOT_STICKY
    override fun onDestroy(){root?.let{try{wm.removeView(it)}catch(_:Exception){}};root=null;super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
}