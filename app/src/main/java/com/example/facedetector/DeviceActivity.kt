package com.example.facedetector

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.example.facedetector.service.CommandRequest
import com.example.facedetector.timehelper.TimeOutTimer
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class DeviceActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)

        val deviceState = MutableLiveData(false)
        deviceState.observe(this){
            if (it){
                loadImage(R.drawable.light_on)
            } else {
                loadImage(R.drawable.light_off)
            }
        }

        val tvTemper = findViewById<TextView>(R.id.edtTemper)
        val tvHumi = findViewById<TextView>(R.id.edtHumi)
        val edtServer = findViewById<EditText>(R.id.edtServer)
        val btnSetServer = findViewById<Button>(R.id.btnSetServer)
        val btnTurn = findViewById<Button>(R.id.btnTurn)
        imageView = findViewById(R.id.imageView)

        val pref = App.getPref()
        val server = pref?.getString("deviceUrl", "not set")
        edtServer.setText(server)
        btnSetServer.setOnClickListener {
            val deviceUrl = "http://" + edtServer.text.toString()
            with(pref!!.edit()){
                putString("deviceUrl", deviceUrl)
                apply()
            }
            Toast.makeText(this@DeviceActivity, "Save server: $deviceUrl ->Restart the app to apply", Toast.LENGTH_LONG).show()
        }

        val deviceService = App.getDeviceService()

        btnTurn.setOnClickListener {
            val command = if(deviceState.value!!) "0" else "1"
            deviceService.sendCommand(CommandRequest(command))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    deviceState.value = !deviceState.value!!
                }
        }

        val timer = object : TimeOutTimer(1, TimeUnit.SECONDS) {
            override fun onTimeoutCompleted(): Int {
                deviceService.sensors()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { listSensors ->
                        val device = listSensors[0]
                        tvTemper.text = device.temperature
                        tvHumi.text = device.humidity
                    }
                return NextAction.REPEAT
            }

        }
        timer.start()
    }

    private fun loadImage(drawable : Int){
        Glide.with(this)
            .load(drawable)
            .into(imageView)
    }
}