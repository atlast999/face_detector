package com.example.facedetector.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.facedetector.App
import com.example.facedetector.R
import com.example.facedetector.service.Attendee
import com.example.facedetector.service.CommandRequest
import com.example.facedetector.service.DeviceService
import com.example.facedetector.timehelper.TimeOutTimer
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.util.concurrent.TimeUnit

class ResultDialog: BaseDialog() {

    private var deviceService: DeviceService? = null

    private lateinit var attendee: Attendee
    private val timer = object : TimeOutTimer(2, TimeUnit.SECONDS) {
        override fun onTimeoutCompleted(): Int {
            if (attendee.name != null){
                deviceService!!.sendCommand(CommandRequest("0"))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { _ ->
                        dismissAllowingStateLoss()
                    }
            } else{
                dismissAllowingStateLoss()
            }
            return NextAction.DISPOSE
        }

    }

    fun show(activity: FragmentActivity?) {
        super.show(activity, TAG)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.diag_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deviceService = App.getDeviceService()
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvId = view.findViewById<TextView>(R.id.tvId)
        val tvRes = view.findViewById<TextView>(R.id.tvRes)
        if (attendee.name == null){
            tvRes.setTextColor(resources.getColor(R.color.red))
            tvRes.text = "GET OUT"
            tvName.text = "You don't belong here"
            tvId.text = ""
            timer.start()
        } else {
            tvRes.setTextColor(resources.getColor(R.color.green))
            tvRes.text = "COME IN"
            tvName.text = attendee.name
            tvId.text = attendee.attendId.toString()
            deviceService!!.sendCommand(CommandRequest("1"))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ ->
                    timer.start()
                }
        }
        val imageView = view.findViewById<ImageView>(R.id.imageAttendee)
        val dir = File(context?.filesDir, "mydir")
        val myImage = File(dir, "myImage.jpeg")
        Glide.with(this)
                        .load(myImage)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(imageView!!)

    }
    companion object {
        const val TAG = "ResultDialog"

        fun newInstance(attendee: Attendee) = ResultDialog().also {
            it.attendee = attendee
        }
    }
}