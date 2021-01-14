package com.example.facedetector

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.example.facedetector.service.CommandRequest
import com.example.facedetector.service.DeviceService
import com.example.facedetector.timehelper.TimeOutTimer
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_device.*
import java.util.*
import java.util.concurrent.TimeUnit


class DeviceActivity : AppCompatActivity() {
    private val REQ_CODE_SPEECH_INPUT = 100
    private lateinit var imageView: ImageView
    private var deviceService: DeviceService? = null
    private val deviceState = MutableLiveData(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)

        deviceState.observe(this){
            if (it){
                loadImage(R.drawable.light_on)
                btnTurn.text = "Turn off"
            } else {
                loadImage(R.drawable.light_off)
                btnTurn.text = "Turn on"
            }
        }

        val tvTemper = findViewById<TextView>(R.id.edtTemper)
        val tvHumi = findViewById<TextView>(R.id.edtHumi)

        val btnTurn = findViewById<Button>(R.id.btnTurn)
        imageView = findViewById(R.id.imageView)

        deviceService = App.getDeviceService()

        deviceService!!.sensors()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { listSensors ->
                val device = listSensors[0]
                tvTemper.text = device.temperature
                tvHumi.text = device.humidity
            }

        btnTurn.setOnClickListener {
            val command = if(deviceState.value!!) "0" else "1"
            deviceService!!.sendCommand(CommandRequest(command))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { response ->
                    deviceState.value = response.status == "1"
                }
        }

        val timer = object : TimeOutTimer(6, TimeUnit.SECONDS) {
            override fun onTimeoutCompleted(): Int {
                deviceService!!.sensors()
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

        val btnVoice = findViewById<Button>(R.id.btnVoice)
        btnVoice.setOnClickListener {
            promptSpeechInput()
        }
    }
    private fun promptSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(
            RecognizerIntent.EXTRA_PROMPT, "Say something..."
        )
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT)
        } catch (a: ActivityNotFoundException) {
            Toast.makeText(
                applicationContext, "NOT SUPPORTED",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loadImage(drawable: Int){
        Glide.with(this)
            .load(drawable)
            .into(imageView)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_CODE_SPEECH_INPUT -> {
                if (resultCode == RESULT_OK && null != data) {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)?.toLowerCase(
                        Locale.ROOT)
                    if (result != null){
                        if (result.contains("on")){
                            deviceService!!.sendCommand(CommandRequest("1"))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { response ->
                                    deviceState.value = response.status == "1"
                                }
                        }
                        if (result.contains("off")){
                            deviceService!!.sendCommand(CommandRequest("0"))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { response ->
                                    deviceState.value = response.status == "1"
                                }
                        }
                    }
                }
            }
        }
    }
}