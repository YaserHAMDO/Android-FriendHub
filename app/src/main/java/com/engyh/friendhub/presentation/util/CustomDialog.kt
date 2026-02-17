package com.engyh.friendhub.presentation.util

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.engyh.friendhub.databinding.CustomDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CustomDialog : DialogFragment() {

    private var _binding: CustomDialogBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = CustomDialogBinding.inflate(LayoutInflater.from(requireContext()))

        val requestKey = requireArguments().getString(ARG_REQUEST_KEY)
            ?: error("ConfirmDialogFragment requires requestKey")

        val title = requireArguments().getString(ARG_TITLE).orEmpty()
        val message = requireArguments().getString(ARG_MESSAGE).orEmpty()
        val positiveText = requireArguments().getString(ARG_POSITIVE).orEmpty().ifBlank { "Yes" }
        val negativeText = requireArguments().getString(ARG_NEGATIVE).orEmpty().ifBlank { "Cancel" }

        binding.titleTextView.text = title
        binding.messageTextView.text = message
        binding.positiveTextView.text = positiveText
        binding.negativeTextView.text = negativeText

        binding.negativeTextView.setOnClickListener {
            parentFragmentManager.setFragmentResult(requestKey, bundleOf(KEY_CONFIRMED to false))
            dismiss()
        }

        binding.positiveTextView.setOnClickListener {
            parentFragmentManager.setFragmentResult(requestKey, bundleOf(KEY_CONFIRMED to true))
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val KEY_CONFIRMED = "confirmed"

        private const val ARG_REQUEST_KEY = "arg_request_key"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_MESSAGE = "arg_message"
        private const val ARG_POSITIVE = "arg_positive"
        private const val ARG_NEGATIVE = "arg_negative"

        fun newInstance(
            requestKey: String,
            title: String,
            message: String,
            positiveText: String = "Yes",
            negativeText: String = "Cancel"
        ): CustomDialog {
            return CustomDialog().apply {
                arguments = bundleOf(
                    ARG_REQUEST_KEY to requestKey,
                    ARG_TITLE to title,
                    ARG_MESSAGE to message,
                    ARG_POSITIVE to positiveText,
                    ARG_NEGATIVE to negativeText
                )
            }
        }
    }
}
