package com.engyh.friendhub.presentation.util

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImagePickerManager(
    private val fragment: Fragment,
    private val onImageSelected: (Uri?) -> Unit
) {

    private var cameraImageUri: Uri? = null

    private val pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest> =
        fragment.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            onImageSelected(uri)
        }

    private val takePictureLauncher: ActivityResultLauncher<Uri> =
        fragment.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                onImageSelected(cameraImageUri)
            } else {
                onImageSelected(null)
            }
        }

    private val requestCameraPermissionLauncher: ActivityResultLauncher<String> =
        fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCameraInternal()
            } else {
                fragment.showSnackbar("Camera permission denied")
            }
        }

    fun launchGallery() {
        pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun launchCamera() {
        when {
            ContextCompat.checkSelfPermission(
                fragment.requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCameraInternal()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }


    private fun launchCameraInternal() {
        cameraImageUri = createCameraOutputUri(fragment.requireContext())
        val uri = cameraImageUri ?: run {
            fragment.showSnackbar("Unable to create camera Uri")
            return
        }
        takePictureLauncher.launch(uri)
    }

    private fun createCameraOutputUri(context: Context): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val name = "IMG_${timeStamp}.jpg"

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/FriendHub")
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } else {
            try {
                val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                val file = File.createTempFile("IMG_", ".jpg", dir)
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
