package com.engyh.friendhub.presentation.ui.fragment.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.engyh.friendhub.R
import com.engyh.friendhub.databinding.FragmentEmailSignUpBinding
import com.engyh.friendhub.presentation.util.showSnackbar
import com.engyh.friendhub.presentation.viewmodel.SignUpViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmailSignUpFragment : Fragment(R.layout.fragment_email_sign_up) {

    private var _binding: FragmentEmailSignUpBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignUpViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEmailSignUpBinding.bind(view)

        binding.signInTextView.setOnClickListener {
            findNavController().navigate(R.id.action_email_sign_up_to_email_sign_in)
        }

        binding.signUpTextView.setOnClickListener {
            val email = binding.emailEditText.text?.toString().orEmpty().trim()
            val password = binding.passwordEditText.text?.toString().orEmpty()

            if (email.isBlank()) {
                showSnackbar("Enter a valid email")
                return@setOnClickListener
            }
            if (password.length < 6) {
                showSnackbar("Password must be at least 6 characters")
                return@setOnClickListener
            }

            viewModel.updateEmail(email)
            viewModel.updatePassword(password)

            findNavController().navigate(R.id.action_email_sign_up_to_name)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}