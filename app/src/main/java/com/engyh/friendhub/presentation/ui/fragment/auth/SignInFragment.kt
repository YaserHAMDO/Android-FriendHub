package com.engyh.friendhub.presentation.ui.fragment.auth

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.engyh.friendhub.R
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.databinding.FragmentSignInBinding
import com.engyh.friendhub.presentation.util.GoogleSignInHelper
import com.engyh.friendhub.presentation.util.showSnackbar
import com.engyh.friendhub.presentation.viewmodel.SignInViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignInFragment : Fragment(R.layout.fragment_sign_in) {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignInViewModel by viewModels()

    private lateinit var googleHelper: GoogleSignInHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSignInBinding.bind(view)

        googleHelper = GoogleSignInHelper(requireContext())

        setupClicks()
        observeSignInState()
        setupOnBackPressedCallback()
    }

    private fun setupClicks() {
        binding.googleConstraintLayout.setOnClickListener {
            if (binding.googleProgressBar.isVisible) return@setOnClickListener
            startGoogleSignIn()
        }

        binding.emailConstraintLayout.setOnClickListener {
            findNavController().navigate(R.id.action_sign_in_to_email)
        }
    }

    private fun observeSignInState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.signInState.collect { state ->
                    when (state) {
                        is Response.Loading -> renderLoading(true)

                        is Response.Error -> {
                            renderLoading(false)
                            showSnackbar(state.message)
                            viewModel.consumeSignInState()
                        }

                        is Response.Success -> {
                            renderLoading(false)

                            if (!isAdded) return@collect

                            if (state.data) {
                                findNavController().navigate(R.id.action_sign_in_to_main)
                            } else {
                                findNavController().navigate(R.id.action_sign_in_to_name)
                            }

                            viewModel.consumeSignInState()
                        }

                        null -> Unit
                    }
                }
            }
        }
    }

    private fun startGoogleSignIn() {
        renderLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val result = googleHelper.signIn()
            result.fold(
                onSuccess = { firebaseCred ->
                    viewModel.signInWithCredential(firebaseCred)
                },
                onFailure = {
                    renderLoading(false)
                    showSnackbar("Sign-in cancelled")
                }
            )
        }
    }

    private fun renderLoading(loading: Boolean) {
        binding.googleConstraintLayout.isEnabled = !loading
        binding.emailConstraintLayout.isEnabled = !loading
        binding.googleConstraintLayout.isVisible = !loading
        binding.googleProgressBar.isVisible = loading
    }

    private fun setupOnBackPressedCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}