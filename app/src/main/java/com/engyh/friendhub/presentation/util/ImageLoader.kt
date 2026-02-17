package com.engyh.friendhub.presentation.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.engyh.friendhub.R

object ImageLoader {

    fun loadUserImage(
        imageView: ImageView,
        url: String?,
        progress: ProgressBar,
    ) {

        progress.isVisible = true

        val req = Glide.with(imageView)
            .load(url)
            .circleCrop()

        req.listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable?>,
                isFirstResource: Boolean
            ): Boolean {
                progress.isVisible = false
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable?>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                progress.isVisible = false
                return false
            }
        }).into(imageView)
    }

    fun loadPostImage(
        imageView: ImageView,
        url: String?,
        progressBar: ProgressBar,
        background: ConstraintLayout,
        context: Context,
        action: () -> Unit
    ) {

        progressBar.isVisible = true

        Glide.with(imageView)
            .asBitmap()
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .dontAnimate()
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    imageView.setImageBitmap(resource)
                    progressBar.isVisible = false

                    val fallback = ContextCompat.getColor(context, R.color.color5)
                    Palette.from(resource).generate { palette ->
                        val bg = palette?.getDominantColor(fallback) ?: fallback
                        background.setBackgroundColor(bg)
                    }

                    imageView.setOnClickListener {
                        action()
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    progressBar.isVisible = false
                    imageView.setImageDrawable(errorDrawable)
                    background.setBackgroundColor(ContextCompat.getColor(context, R.color.color6))
                }
            })
    }
}
