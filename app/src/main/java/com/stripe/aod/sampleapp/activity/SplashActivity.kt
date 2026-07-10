package com.stripe.aod.sampleapp.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Use a themed windowBackground for the splash image to avoid setContentView overhead.
    lifecycleScope.launch {
      // show splash for ~2 seconds
      delay(2000)
      // Navigate to main / onboarding
      startActivity(Intent(this@SplashActivity, MainActivity::class.java))
      finish()
    }
  }
}
