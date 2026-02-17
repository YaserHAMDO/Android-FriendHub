package com.engyh.friendhub.presentation.ui.fragment.onboarding

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.engyh.friendhub.R
import com.engyh.friendhub.databinding.FragmentImageBinding
import com.engyh.friendhub.presentation.ui.fragment.other.ImageSourceFragment
import com.engyh.friendhub.presentation.util.ImagePickerManager
import com.engyh.friendhub.presentation.util.showSnackbar
import com.engyh.friendhub.presentation.viewmodel.SignUpViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ImageFragment : Fragment(R.layout.fragment_image) {

    private var _binding: FragmentImageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignUpViewModel by activityViewModels()
    private lateinit var imagePickerManager: ImagePickerManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentImageBinding.bind(view)

        setupImagePicker()

        binding.imageView.setOnClickListener { showImageSourceBottomSheet() }

        binding.nextTextView.setOnClickListener {
            if (viewModel.data.value.image.isNullOrBlank()) {
                showSnackbar("Please select an image")
                return@setOnClickListener
            }
            findNavController().navigate(R.id.action_image_to_gender)
        }

        viewModel.data.value.image?.let { uriStr ->
            binding.imageView.setImageURI(uriStr.toUri())
        }
    }

    private fun setupImagePicker() {
        imagePickerManager = ImagePickerManager(this) { uri ->
            handleImageSelection(uri)
        }
    }

    private fun showImageSourceBottomSheet() {
        val bottomSheet = ImageSourceFragment(
            onGalleryClick = { imagePickerManager.launchGallery() },
            onCameraClick = { imagePickerManager.launchCamera() }
        )
        bottomSheet.show(parentFragmentManager, bottomSheet.tag)
    }

    private fun handleImageSelection(uri: Uri?) {
        if (uri == null) {
            showSnackbar("No image selected")
            return
        }
        binding.imageView.setImageURI(uri)
        viewModel.updateImage(uri)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
