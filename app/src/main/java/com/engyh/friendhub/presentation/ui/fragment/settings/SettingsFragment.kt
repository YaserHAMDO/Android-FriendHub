package com.engyh.friendhub.presentation.ui.fragment.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.engyh.friendhub.R
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.databinding.FragmentSettingsBinding
import com.engyh.friendhub.presentation.util.showSnackbar
import com.engyh.friendhub.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        binding.logoutLinearLayout.setOnClickListener {
            viewModel.logout()
            showSnackbar("Logged out")
            navigateToSignInClearBackstack()
        }

        binding.deleteAccountLinearLayout.setOnClickListener {
            viewModel.deleteAccount()
        }

        observeDeleteAccount()
    }

    private fun observeDeleteAccount() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.deleteState.collect { response ->
                    when (response) {
                        is Response.Loading -> {
                        }
                        is Response.Success -> {
                            showSnackbar("Account deleted")
                            viewModel.logout()
                            viewModel.resetDeleteState()
                            navigateToSignInClearBackstack()
                        }
                        is Response.Error -> {
                            showSnackbar("Failed to delete account: ${response.message}")
                            viewModel.resetDeleteState()
                        }
                        null -> Unit
                    }
                }
            }
        }
    }

    private fun navigateToSignInClearBackstack() {
        findNavController().navigate(
            R.id.signInFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}