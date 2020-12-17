package com.example.facedetector

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class FunctionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_function)

        val btnController = findViewById<Button>(R.id.btnController)
        btnController.setOnClickListener {
            startActivity(Intent(this@FunctionActivity, DeviceActivity::class.java))
        }

        val btnRecognition = findViewById<Button>(R.id.btnRecognition)
        btnRecognition.setOnClickListener {
            startActivity(Intent(this@FunctionActivity, OptionActivity::class.java))
        }

    }
}