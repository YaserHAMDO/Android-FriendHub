package com.engyh.friendhub

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FriendHubApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        EmojiManager.install(GoogleEmojiProvider())
    }
}