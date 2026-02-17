package com.engyh.friendhub.presentation.ui.fragment.other

import android.os.Bundle
import android.view.View
import com.engyh.friendhub.R
import com.engyh.friendhub.databinding.FragmentImageSourceBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ImageSourceFragment(
    private val onGalleryClick: () -> Unit,
    private val onCameraClick: () -> Unit
) : BottomSheetDialogFragment(R.layout.fragment_image_source) {

    private var _binding: FragmentImageSourceBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentImageSourceBinding.bind(view)

        binding.galleryLinearLayout.setOnClickListener {
            onGalleryClick()
            dismiss()
        }

        binding.cameraLinearLayout.setOnClickListener {
            onCameraClick()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}