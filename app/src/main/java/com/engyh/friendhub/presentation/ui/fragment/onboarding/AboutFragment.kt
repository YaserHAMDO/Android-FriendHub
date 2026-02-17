package com.engyh.friendhub.presentation.ui.fragment.onboarding

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.engyh.friendhub.R
import com.engyh.friendhub.databinding.FragmentAboutBinding
import com.engyh.friendhub.presentation.util.showSnackbar
import com.engyh.friendhub.presentation.viewmodel.SignUpViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AboutFragment : Fragment(R.layout.fragment_about) {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignUpViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentAboutBinding.bind(view)

        binding.aboutEditText.setText(viewModel.data.value.about)

        binding.aboutEditText.doAfterTextChanged {
            viewModel.updateAbout(it?.toString().orEmpty())
        }

        binding.nextTextView.setOnClickListener {
            val about = binding.aboutEditText.text?.toString().orEmpty().trim()
            if (about.isBlank()) {
                showSnackbar("Please write a short bio")
                binding.aboutEditText.requestFocus()
                return@setOnClickListener
            }

            viewModel.updateAbout(about)
            viewModel.clearError()
            viewModel.signUpUser()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progressBar.visibility = if (state.loading) View.VISIBLE else View.GONE
                    binding.nextTextView.visibility = if (state.loading) View.INVISIBLE else View.VISIBLE

                    state.error?.let { showSnackbar(it) }

                    if (state.success) {
                        viewModel.resetState()
                        findNavController().navigate(R.id.action_about_to_main)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}