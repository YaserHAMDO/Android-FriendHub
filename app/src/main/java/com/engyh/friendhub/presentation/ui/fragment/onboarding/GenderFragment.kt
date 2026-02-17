package com.engyh.friendhub.presentation.ui.fragment.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.engyh.friendhub.R
import com.engyh.friendhub.databinding.FragmentGenderBinding
import com.engyh.friendhub.presentation.util.showSnackbar
import com.engyh.friendhub.presentation.viewmodel.SignUpViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GenderFragment : Fragment(R.layout.fragment_gender) {

    private var _binding: FragmentGenderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignUpViewModel by activityViewModels()

    val selectedDrawable = R.drawable.gender_blue_background
    val unselectedDrawable = R.drawable.gender_grey_background

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentGenderBinding.bind(view)

        binding.maleFrameLayout.setOnClickListener {
            viewModel.updateGender("Male")
            renderSelection("Male")
        }

        binding.femaleFrameLayout.setOnClickListener {
            viewModel.updateGender("Female")
            renderSelection("Female")
        }

        binding.notToSayFrameLayout.setOnClickListener {
            viewModel.updateGender("Prefer not to say")
            renderSelection("Prefer not to say")
        }

        binding.nextTextView.setOnClickListener {
            if (viewModel.data.value.gender.isBlank()) {
                showSnackbar("Please select a gender")
                return@setOnClickListener
            }
            findNavController().navigate(R.id.action_gender_to_birthdate)
        }

        renderSelection(viewModel.data.value.gender)
    }

    private fun renderSelection(gender: String?) {
        when(gender) {
            "Male" -> {
                binding.maleFrameLayout.setBackgroundResource(selectedDrawable)
                binding.femaleFrameLayout.setBackgroundResource(unselectedDrawable)
                binding.notToSayFrameLayout.setBackgroundResource(unselectedDrawable)
            }
            "Female" -> {
                binding.maleFrameLayout.setBackgroundResource(unselectedDrawable)
                binding.femaleFrameLayout.setBackgroundResource(selectedDrawable)
                binding.notToSayFrameLayout.setBackgroundResource(unselectedDrawable)
            }
            "Prefer not to say" -> {
                binding.maleFrameLayout.setBackgroundResource(unselectedDrawable)
                binding.femaleFrameLayout.setBackgroundResource(unselectedDrawable)
                binding.notToSayFrameLayout.setBackgroundResource(selectedDrawable)
            }
            else -> {
                binding.maleFrameLayout.setBackgroundResource(unselectedDrawable)
                binding.femaleFrameLayout.setBackgroundResource(unselectedDrawable)
                binding.notToSayFrameLayout.setBackgroundResource(unselectedDrawable)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

