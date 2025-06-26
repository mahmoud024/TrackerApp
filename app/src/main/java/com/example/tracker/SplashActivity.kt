package com.example.tracker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView

class SplashActivity : AppCompatActivity() {

    private val splashDuration = 5000L // مدة السلاش سكرين بالميلي ثانية (2 ثواني)

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.logoImage)
        val animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
        logo.startAnimation(animation)


        // بعد 2 ثانية، انتقل للـ MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()  // انهي SplashActivity حتى لا يعود المستخدم لها بالرجوع
        }, splashDuration)
    }
}
