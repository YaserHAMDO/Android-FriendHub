package com.engyh.friendhub.presentation.ui.adapter

import android.media.MediaPlayer

class ChatAudioPlayer {

    private var player: MediaPlayer? = null
    private var currentKey: String? = null

    fun toggle(key: String, url: String, onFinished: () -> Unit): String? {
        if (currentKey == key && player?.isPlaying == true) {
            stop()
            return null
        }
        stop()
        currentKey = key

        player = MediaPlayer().apply {
            setDataSource(url)
            prepare()
            start()
            setOnCompletionListener {
                stop()
                onFinished()
            }
        }

        return currentKey
    }

    fun stop() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        currentKey = null
    }

    fun release() = stop()
}
