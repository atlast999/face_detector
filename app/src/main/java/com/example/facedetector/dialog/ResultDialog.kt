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
import com.example.facedetector.R
import com.example.facedetector.service.Attendee
import com.example.facedetector.timehelper.TimeOutTimer
import java.io.File
import java.util.concurrent.TimeUnit

class ResultDialog: BaseDialog() {

    private lateinit var attendee: Attendee
    private val timer = object : TimeOutTimer(2, TimeUnit.SECONDS) {
        override fun onTimeoutCompleted(): Int {
            dismissAllowingStateLoss()
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
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvId = view.findViewById<TextView>(R.id.tvId)
        if (attendee.name == null){
            tvName.text = "NOT RECOGNIZED"
            tvId.text = ""
        } else {
            tvName.text = attendee.name
            tvId.text = attendee.attendId.toString()
        }
        val imageView = view.findViewById<ImageView>(R.id.imageAttendee)
        val dir = File(context?.filesDir, "mydir")
        val myImage = File(dir, "myImage.jpeg")
        Glide.with(this)
                        .load(myImage)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(imageView!!)
        timer.start()
    }
    companion object {
        const val TAG = "ResultDialog"

        fun newInstance(attendee: Attendee) = ResultDialog().also {
            it.attendee = attendee
        }
    }
}