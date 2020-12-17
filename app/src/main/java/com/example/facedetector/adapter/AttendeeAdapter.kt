package com.example.facedetector.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.facedetector.R
import com.example.facedetector.service.Attendee

class AttendeeAdapter(val context: Context, val deleteClick: (Attendee) -> Unit): RecyclerView.Adapter<AttendeeAdapter.ViewHolder>() {

    private val data = mutableListOf<Attendee>()

    fun setData(list: List<Attendee>){
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_list_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val attendee = data[position]
        holder.tvName.text = attendee.name
        holder.tvId.text = attendee.attendId.toString()
        holder.tvDelete.setOnClickListener {
            deleteClick(attendee)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvId: TextView = itemView.findViewById(R.id.tvId)
        val tvDelete: TextView = itemView.findViewById(R.id.tvDelete)
    }
}