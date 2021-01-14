package com.example.facedetector

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.facedetector.service.LoginRequest
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val edtUsername = findViewById<EditText>(R.id.edt_username)
        val edtPassword = findViewById<EditText>(R.id.edt_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val btnSetting = findViewById<Button>(R.id.btn_setting)


        val edtDeviceServer = findViewById<EditText>(R.id.edtDeviceServer)
        val btnDeviceServer = findViewById<Button>(R.id.btnDeviceServer)

        val pref = App.getPref()
        val deviceServer = pref?.getString("deviceUrl", "not set")
        edtDeviceServer.setText(deviceServer)
        btnDeviceServer.setOnClickListener {
            val deviceUrl = "http://" + edtDeviceServer.text.toString()
            with(pref!!.edit()){
                putString("deviceUrl", deviceUrl)
                apply()
            }
            Toast.makeText(this@LoginActivity, "Save device server: $deviceUrl ->Restart the app to apply", Toast.LENGTH_LONG).show()
        }

        btnSetting.setOnClickListener {
            startActivity(Intent(this@LoginActivity, OptionActivity::class.java))
        }

        val deviceService = App.getDeviceService()
        if (deviceServer != null){
            btnLogin.setOnClickListener {
                deviceService!!.login(LoginRequest(edtUsername.text.toString(), edtPassword.text.toString()))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {response ->
                        if (response.code == "200"){
                            startActivity(Intent(this@LoginActivity, FunctionActivity::class.java))
                        } else {
                            Toast.makeText(this@LoginActivity, response.message, Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

    }
}