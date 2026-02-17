package com.engyh.friendhub.presentation.ui.fragment.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.engyh.friendhub.R
import com.engyh.friendhub.databinding.FragmentLocationBinding
import com.engyh.friendhub.presentation.util.LocationHelper
import com.engyh.friendhub.presentation.viewmodel.SignUpViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LocationFragment : Fragment(R.layout.fragment_location) {

    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignUpViewModel by activityViewModels()
    private lateinit var locationHelper: LocationHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLocationBinding.bind(view)

        binding.titleTextView.text = "Welcome ${viewModel.data.value.name}"

        locationHelper = LocationHelper(
            context = requireContext(),
            caller = this,
        onLocationReady = { location ->
            binding.progressBar.visibility = View.GONE
            binding.nextTextView.visibility = View.VISIBLE
            viewModel.updateLocation(location.latitude, location.longitude)
            findNavController().navigate(R.id.action_location_to_image)
        },
        onLocationDenied = {
            binding.progressBar.visibility = View.GONE
            binding.nextTextView.visibility = View.VISIBLE
        }
        )

        binding.nextTextView.setOnClickListener {
            val (lat, lon) = viewModel.data.value.location
            val hasLocation = lat != 0.0 && lon != 0.0

            if (hasLocation) {
                findNavController().navigate(R.id.action_location_to_image)
            } else {
                binding.progressBar.visibility = View.VISIBLE
                binding.nextTextView.visibility = View.INVISIBLE
                locationHelper.checkAndRequestLocation()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationHelper.release()
        _binding = null
    }
}