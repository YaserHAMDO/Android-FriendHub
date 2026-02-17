package com.engyh.friendhub.presentation.ui.fragment.other

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.engyh.friendhub.R
import com.engyh.friendhub.databinding.FragmentImagePreviewBinding
import com.engyh.friendhub.presentation.util.showSnackbar
import java.io.IOException

class ImagePreviewFragment : Fragment(R.layout.fragment_image_preview) {

    private var _binding: FragmentImagePreviewBinding? = null
    private val binding get() = _binding!!

    private val imageUrl: String by lazy { requireArguments().getString(ARG_IMAGE_URL).orEmpty() }
    private val senderName: String by lazy { requireArguments().getString(ARG_SENDER_NAME).orEmpty() }
    private val time: String by lazy { requireArguments().getString(ARG_TIME).orEmpty() }

    private val requestWritePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                saveImageToGallery(imageUrl)
            } else {
                showSnackbar("Permission denied")
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentImagePreviewBinding.bind(view)

        binding.senderNameTextView.text = senderName
        binding.timeTextView.text = time

        Glide.with(this)
            .load(imageUrl)
            .into(binding.previewedImageView)

        binding.downloadImageView.setOnClickListener {
            if (imageUrl.isBlank()) {
                showSnackbar("No image url")

                return@setOnClickListener
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveImageToGallery(imageUrl)
            } else {
                val granted = ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

                if (granted) {
                    saveImageToGallery(imageUrl)
                } else {
                    requestWritePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun saveImageToGallery(imageUrl: String) {
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap>() {

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val resolver = requireContext().contentResolver

                    val values = ContentValues().apply {
                        put(
                            MediaStore.Images.Media.DISPLAY_NAME,
                            "downloaded_image_${System.currentTimeMillis()}.jpg"
                        )
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    }

                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri == null) {
                        showSnackbar("Failed to create media entry")

                        return
                    }

                    try {
                        resolver.openOutputStream(uri).use { out ->
                            if (out == null) throw IOException("OutputStream is null")
                            resource.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            values.clear()
                            values.put(MediaStore.Images.Media.IS_PENDING, 0)
                            resolver.update(uri, values, null, null)
                        }
                        showSnackbar("Image saved to gallery!")

                    } catch (e: Exception) {
                        showSnackbar("Failed to save: ${e.message}")

                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) = Unit
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_IMAGE_URL = "imageUrl"
        private const val ARG_SENDER_NAME = "senderName"
        private const val ARG_TIME = "time"

        fun newArgs(imageUrl: String, senderName: String, time: String) = Bundle().apply {
            putString(ARG_IMAGE_URL, imageUrl)
            putString(ARG_SENDER_NAME, senderName)
            putString(ARG_TIME, time)
        }
    }
}