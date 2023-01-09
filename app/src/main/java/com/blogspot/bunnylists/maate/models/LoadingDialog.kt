package com.blogspot.bunnylists.maate.models

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.blogspot.bunnylists.maate.R

class LoadingDialog(
    activity: Activity,
) {
    private var dialog: AlertDialog
    init {
        val inflater = activity.layoutInflater
        val dialogView = inflater.inflate(R.layout.loading_layout, null)
        val builder = AlertDialog.Builder(activity)
        builder.setView(dialogView)
        builder.setCancelable(false)
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun startLoading() {
        dialog.show()
    }

    fun isDismiss() {
        dialog.dismiss()
    }
}