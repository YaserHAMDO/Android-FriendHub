package com.engyh.friendhub.presentation.ui.fragment.auth

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.engyh.friendhub.R
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.databinding.FragmentEmailSignInBinding
import com.engyh.friendhub.presentation.util.showSnackbar
import com.engyh.friendhub.presentation.viewmodel.SignInViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EmailSignInFragment : Fragment(R.layout.fragment_email_sign_in) {

    private var _binding: FragmentEmailSignInBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignInViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEmailSignInBinding.bind(view)

        binding.signInTextView.setOnClickListener {
            val email = binding.emailEditText.text?.toString().orEmpty().trim()
            val password = binding.passwordEditText.text?.toString().orEmpty()

            if (email.isBlank() || password.length < 6) {
                showSnackbar("Enter valid email and password (>=6 chars)")
                return@setOnClickListener
            }

            viewModel.signIn(email, password)
        }

        binding.SignUpTextView.setOnClickListener {
            findNavController().navigate(R.id.action_email_sign_in_to_email_sign_up)
        }

        observeSignState()
    }

    private fun observeSignState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.signInState.collect { state ->
                    when (state) {
                        is Response.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.signInTextView.isEnabled = false
                        }

                        is Response.Error -> {
                            binding.progressBar.isVisible = false
                            binding.signInTextView.isEnabled = true
                            showSnackbar(state.message)
                            viewModel.consumeSignInState()
                        }

                        is Response.Success -> {
                            binding.progressBar.isVisible = false
                            binding.signInTextView.isEnabled = true

                            if (!isAdded) return@collect

                            if (state.data) {
                                findNavController().navigate(R.id.action_email_sign_in_to_main)
                            } else {
                                findNavController().navigate(R.id.action_sign_in_to_name)
                            }

                            viewModel.consumeSignInState()
                        }

                        null -> {
                            binding.progressBar.isVisible = false
                            binding.signInTextView.isEnabled = true
                        }
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