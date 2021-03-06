package com.example.facedetector

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.facedetector.adapter.AttendeeAdapter
import com.example.facedetector.service.Attendee
import com.example.facedetector.service.DeleteRequest
import com.example.facedetector.service.DeviceService
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers

class AttendeeActivity : AppCompatActivity() {
    private lateinit var adapter: AttendeeAdapter
    private var service: DeviceService? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendees)

        service = App.getDeviceService()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AttendeeAdapter(this) {
            deleteAttendee(it)
        }

        service!!.getAllUsers()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { attendees -> adapter.setData(attendees) }

        recyclerView.adapter = adapter
    }

    private fun deleteAttendee(attendee: Attendee){
        service!!.delete(DeleteRequest(attendee.attendId))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                attendees -> adapter.setData(attendees)
            }
    }
}