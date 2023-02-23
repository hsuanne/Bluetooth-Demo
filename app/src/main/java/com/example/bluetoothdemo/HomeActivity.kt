package com.example.bluetoothdemo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothdemo.ble.BLEActivity
import com.example.bluetoothdemo.main.MainActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val bluetoothBtn: Button = findViewById(R.id.bluetoothBtn)
        val bleBtn: Button = findViewById(R.id.bleBtn)
        
        bluetoothBtn.setOnClickListener { 
            navigateToMainActivity()
        }

        bleBtn.setOnClickListener {
            navigateToBleActivity()
        }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun navigateToBleActivity() {
        startActivity(Intent(this, BLEActivity::class.java))
    }
}