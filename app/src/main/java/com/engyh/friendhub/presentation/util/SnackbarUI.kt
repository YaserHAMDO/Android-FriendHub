package com.engyh.friendhub.presentation.util


import android.app.Activity
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

object SnackbarUI {

    fun show(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }
}


fun Fragment.showSnackbar(message: String) {
    val root = view ?: return
    SnackbarUI.show(root, message)
}

fun Activity.showSnackbar(message: String, isLong: Boolean = false) {
    val root: View = findViewById(android.R.id.content)
    Snackbar.make(root, message, if (isLong) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT).show()
}