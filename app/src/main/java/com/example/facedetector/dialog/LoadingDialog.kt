package com.example.facedetector.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.example.facedetector.R

class LoadingDialog : BaseDialog() {
    private var gravity = Gravity.CENTER
    fun show(activity: FragmentActivity?) {
        super.show(activity, TAG)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.diag_loading, container, false).apply {
            if (gravity != Gravity.CENTER) {
                findViewById<View>(R.id.progress)?.let {
                    it.layoutParams as? FrameLayout.LayoutParams
                }?.let {
                    it.gravity = this@LoadingDialog.gravity
                }
            }
        }
    }

    companion object {
        const val TAG = "LoadingDialog"

        fun newInstance() = LoadingDialog()

        fun newInstance(gravity: Int) = LoadingDialog().also {
            it.gravity = gravity
        }
    }
}