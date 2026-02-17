package com.engyh.friendhub.presentation.ui.fragment.onboarding

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.engyh.friendhub.R
import com.engyh.friendhub.databinding.FragmentNameBinding
import com.engyh.friendhub.presentation.util.CustomDialog
import com.engyh.friendhub.presentation.util.showSnackbar
import com.engyh.friendhub.presentation.viewmodel.SignUpViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NameFragment : Fragment(R.layout.fragment_name) {

    private var _binding: FragmentNameBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignUpViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNameBinding.bind(view)

        binding.nameEditText.setText(viewModel.data.value.name)

        binding.nameEditText.doAfterTextChanged {
            viewModel.updateUserName(it?.toString().orEmpty())
        }

        binding.nextTextView.setOnClickListener {
            val name = binding.nameEditText.text?.toString().orEmpty().trim()
            if (name.isBlank()) {
                showSnackbar("Enter a display name")
                binding.nameEditText.requestFocus()
                return@setOnClickListener
            }
            viewModel.updateUserName(name)
            findNavController().navigate(R.id.action_name_to_location)
        }

        setupOnBackPressedCallback()
    }

    private fun setupOnBackPressedCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showCancelRegistrationDialog()
                }
            }
        )
    }

    private fun showCancelRegistrationDialog() {
        val tag = "cancel_registration_dialog"
        val key = "cancel_registration"

        if (parentFragmentManager.findFragmentByTag(tag) != null) return

        parentFragmentManager.setFragmentResultListener(
            key,
            viewLifecycleOwner
        ) { _, bundle ->
            val confirmed = bundle.getBoolean(CustomDialog.KEY_CONFIRMED, false)
            if (!confirmed) return@setFragmentResultListener

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.logout()
                if (isAdded) findNavController().navigate(R.id.action_name_to_sign_in)
            }
        }

        CustomDialog.newInstance(
            requestKey = key,
            title = "Cancel Registration",
            message = "Are you sure you want to cancel registration?",
            positiveText = "Yes",
            negativeText = "No"
        ).show(parentFragmentManager, tag)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
