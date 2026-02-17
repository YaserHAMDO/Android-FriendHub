package com.engyh.friendhub.presentation.ui.fragment.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.engyh.friendhub.R
import com.engyh.friendhub.databinding.FragmentBirthdateBinding
import com.engyh.friendhub.presentation.util.showSnackbar
import com.engyh.friendhub.presentation.viewmodel.SignUpViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class BirthdateFragment : Fragment(R.layout.fragment_birthdate) {

    private var _binding: FragmentBirthdateBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignUpViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentBirthdateBinding.bind(view)

        configDatePicker()

        binding.nextTextView.setOnClickListener {
            if (viewModel.data.value.birthdate.isEmpty()) {
                showSnackbar("Please select your birthdate")
                return@setOnClickListener
            }
            findNavController().navigate(R.id.action_birthdate_to_about)
        }
    }

    private fun configDatePicker() {
        val c = Calendar.getInstance().apply { add(Calendar.YEAR, -18) }
        binding.datePicker.maxDate = c.timeInMillis

        val (year, month0, day) = parseBirthdateOrDefault(viewModel.data.value.birthdate)

        binding.datePicker.init(year, month0, day) { _, y, m, d ->
            viewModel.updateBirthdate(y, m + 1, d)
        }
    }

    private fun parseBirthdateOrDefault(birthdate: String): Triple<Int, Int, Int> {
        val parts = birthdate.split("-")
        if (parts.size == 3) {
            val day = parts[0].toIntOrNull()
            val month = parts[1].toIntOrNull()
            val year = parts[2].toIntOrNull()
            if (day != null && month != null && year != null) {
                return Triple(year, month - 1, day)
            }
        }
        return Triple(1990, 0, 1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}