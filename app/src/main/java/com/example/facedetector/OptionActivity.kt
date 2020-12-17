package com.example.facedetector

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class OptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_option)
        val edtServer = findViewById<EditText>(R.id.tvServer)
        val btnConfirm = findViewById<Button>(R.id.btnConfirm)
        val btnToUpload = findViewById<Button>(R.id.btnToUpload)
        val btnRecognise = findViewById<Button>(R.id.btnToRecognite)
        val btnListAttendee = findViewById<Button>(R.id.btnListAttendees)

        btnListAttendee.setOnClickListener {
            startActivity(Intent(this@OptionActivity, AttendeeActivity::class.java))
        }

        val pref = App.getPref()
        val server = pref?.getString("baseUrl", "not set")
        edtServer.setText(server)

        btnConfirm.setOnClickListener {
            val baseUrl = "http://" + edtServer.text.toString()
            with(pref!!.edit()){
                putString("baseUrl", baseUrl)
                apply()
            }
            Toast.makeText(this@OptionActivity, "Save server: $baseUrl ->Restart the app to apply", Toast.LENGTH_LONG).show()
        }

        btnRecognise.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("type", "recognite")
            startActivity(intent)
        }

        btnToUpload.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("type", "upload")
            startActivity(intent)
        }
    }
}