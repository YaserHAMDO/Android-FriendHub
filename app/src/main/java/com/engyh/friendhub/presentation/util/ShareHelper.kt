package com.engyh.friendhub.presentation.util

import android.content.Context
import android.content.Intent

object ShareHelper {

    fun share(text: String, context: Context) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Profile"))

    }
}
