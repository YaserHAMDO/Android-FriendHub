package com.engyh.friendhub.presentation.ui.fragment.edit

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.engyh.friendhub.R
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.databinding.DialogSpinnerDatepickerBinding
import com.engyh.friendhub.databinding.FragmentEditBinding
import com.engyh.friendhub.presentation.ui.fragment.other.ImageSourceFragment
import com.engyh.friendhub.presentation.util.ImageLoader
import com.engyh.friendhub.presentation.util.ImagePickerManager
import com.engyh.friendhub.presentation.util.showSnackbar
import com.engyh.friendhub.presentation.viewmodel.EditViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class EditFragment : Fragment(R.layout.fragment_edit) {

    private var _binding: FragmentEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditViewModel by viewModels()

    private var imageUri: Uri? = null
    private var selectedGender: String? = null

    private var year: Int = 0
    private var month: Int = 0
    private var dayOfMonth: Int = 0


    private lateinit var imagePickerManager: ImagePickerManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEditBinding.bind(view)

        setupClickListeners()
        setupImagePicker()
        observeUser()
        observeUpdateState()
    }

    private fun setupClickListeners() {
        binding.imageView.setOnClickListener { showImageSourceBottomSheet() }

        binding.birthdateTextView.setOnClickListener {
            showDatePickerDialog()
        }

        binding.updateTextView.setOnClickListener {
            val name = binding.editTextName.text.toString().trim()
            val about = binding.aboutEditText.text.toString().trim()

            if (name.isBlank()) {
                showSnackbar("Please enter your name.")
                return@setOnClickListener
            }
            if (about.isBlank()) {
                showSnackbar("Please enter something about you.")
                return@setOnClickListener
            }

            val gender = selectedGender
            if (gender.isNullOrBlank()) {
                showSnackbar("Please select your gender.")
                return@setOnClickListener
            }

            val birthdate = String.format("%02d-%02d-%04d", dayOfMonth, month + 1, year)

            viewModel.updateUser(
                name = name,
                about = about,
                imageUri = imageUri,
                gender = gender,
                birthdate = birthdate
            )
        }

        binding.maleFrameLayout.setOnClickListener { setGenderUi("Male") }
        binding.femaleFrameLayout.setOnClickListener { setGenderUi("Female") }
        binding.notToSayFrameLayout.setOnClickListener { setGenderUi("Prefer not to say") }
    }

    private fun observeUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.user.collect { user ->
                    if (user == null) return@collect

                    binding.editTextName.setText(user.name)
                    binding.aboutEditText.setText(user.about)
                    binding.birthdateTextView.text = user.birthdate

                    val parts = user.birthdate.split("-")
                    if (parts.size == 3) {
                        dayOfMonth = parts[0].toIntOrNull() ?: 1
                        month = (parts[1].toIntOrNull() ?: 1) - 1
                        year = parts[2].toIntOrNull() ?: 2000
                    }

                    setGenderUi(user.gender)

                    ImageLoader.loadUserImage(
                        imageView = binding.imageView,
                        url = user.imageUrl,
                        progress = binding.ImageProgressBar,
                    )

                }
            }
        }
    }

    private fun observeUpdateState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateState.collect { response ->
                    when (response) {
                        is Response.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.updateTextView.visibility = View.INVISIBLE
                            binding.nextConstraintLayout.isEnabled = false
                        }

                        is Response.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.updateTextView.visibility = View.VISIBLE
                            binding.nextConstraintLayout.isEnabled = true

                            showSnackbar("Profile updated successfully")

                            findNavController().navigateUp()
                        }

                        is Response.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.updateTextView.visibility = View.VISIBLE
                            binding.nextConstraintLayout.isEnabled = true

                            showSnackbar(response.message)

                        }

                        null -> Unit
                    }
                }
            }
        }
    }

    private fun setupImagePicker() {
        imagePickerManager = ImagePickerManager(this) { uri ->
            handleImageSelection(uri)
        }
    }

    private fun showImageSourceBottomSheet() {
        val bottomSheet = ImageSourceFragment(
            onGalleryClick = { imagePickerManager.launchGallery() },
            onCameraClick = { imagePickerManager.launchCamera() }
        )
        bottomSheet.show(parentFragmentManager, bottomSheet.tag)
    }

    private fun handleImageSelection(uri: Uri?) {
        if (uri == null) {
            showSnackbar("No image selected")
            return
        }
        imageUri = uri
        binding.imageView.setImageURI(uri)

    }

    private fun showDatePickerDialog() {
        val dialogBinding = DialogSpinnerDatepickerBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val calendar = Calendar.getInstance()

        val currentText = binding.birthdateTextView.text?.toString().orEmpty()
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        try {
            if (currentText.isNotBlank()) {
                sdf.parse(currentText)?.let { calendar.time = it }

            }
        } catch (_: Exception) {}

        dialogBinding.datePickerSpinner.init(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            null
        )

        val maxDateCalendar = Calendar.getInstance().apply { add(Calendar.YEAR, -18) }
        dialogBinding.datePickerSpinner.maxDate = maxDateCalendar.timeInMillis

        dialogBinding.okButton.setOnClickListener {
            year = dialogBinding.datePickerSpinner.year
            month = dialogBinding.datePickerSpinner.month
            dayOfMonth = dialogBinding.datePickerSpinner.dayOfMonth

            binding.birthdateTextView.text =
                String.format("%02d-%02d-%04d", dayOfMonth, month + 1, year)

            dialog.dismiss()
        }

        dialogBinding.cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setGenderUi(gender: String) {
        val selectedDrawable = R.drawable.gender_blue_background
        val unselectedDrawable = R.drawable.gender_grey_background

        selectedGender = gender

        when (gender) {
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
            else -> {
                binding.maleFrameLayout.setBackgroundResource(unselectedDrawable)
                binding.femaleFrameLayout.setBackgroundResource(unselectedDrawable)
                binding.notToSayFrameLayout.setBackgroundResource(selectedDrawable)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}