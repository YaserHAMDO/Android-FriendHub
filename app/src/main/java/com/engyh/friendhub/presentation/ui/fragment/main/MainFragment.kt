package com.engyh.friendhub.presentation.ui.fragment.main

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.engyh.friendhub.R
import com.engyh.friendhub.databinding.FragmentMainBinding
import com.engyh.friendhub.presentation.ui.adapter.ViewPagerAdapter
import com.engyh.friendhub.presentation.viewmodel.AppBootstrapViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainFragment : Fragment(R.layout.fragment_main) {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val bootstrapViewModel: AppBootstrapViewModel by activityViewModels()

    private var pageCallback: ViewPager2.OnPageChangeCallback? = null
    private var navigatedAway = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMainBinding.bind(view)

        binding.viewPager.adapter = ViewPagerAdapter(this)

        pageCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = setBottomBar(position)
        }

        pageCallback?.let(binding.viewPager::registerOnPageChangeCallback)

        binding.icon1LinearLayout.setOnClickListener { binding.viewPager.setCurrentItem(0, false) }
        binding.icon2LinearLayout.setOnClickListener { binding.viewPager.setCurrentItem(1, false) }
        binding.icon3LinearLayout.setOnClickListener { binding.viewPager.setCurrentItem(2, false) }
        binding.icon4LinearLayout.setOnClickListener { binding.viewPager.setCurrentItem(3, false) }

        bootstrapViewModel.start()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bootstrapViewModel.state.collect { state ->
                    when (state) {
                        AppBootstrapViewModel.BootstrapState.Loading -> Unit
                        AppBootstrapViewModel.BootstrapState.Ready -> Unit
                        AppBootstrapViewModel.BootstrapState.LoggedOut -> {
                            if (!navigatedAway) {
                                navigatedAway = true
                                findNavController().navigate(R.id.action_main_to_sign_in)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setBottomBar(index: Int) {
        val context = requireContext()
        val typefaceMedium = ResourcesCompat.getFont(context, R.font.roboto_medium)
        val typefaceLight = ResourcesCompat.getFont(context, R.font.roboto_light)
        val selectedColor = ContextCompat.getColor(context, R.color.color5)
        val unselectedColor = ContextCompat.getColor(context, R.color.black)

        binding.icon1ImageView.setImageResource(R.drawable.non_colored_chat_icon)
        binding.icon1TextView.setTextColor(unselectedColor)
        binding.icon1TextView.textSize = 9f
        binding.icon1TextView.typeface = typefaceLight

        binding.icon2ImageView.setImageResource(R.drawable.non_colored_post_icon)
        binding.icon2TextView.setTextColor(unselectedColor)
        binding.icon2TextView.textSize = 9f
        binding.icon2TextView.typeface = typefaceLight

        binding.icon3ImageView.setImageResource(R.drawable.non_colored_friends_icon)
        binding.icon3TextView.setTextColor(unselectedColor)
        binding.icon3TextView.textSize = 9f
        binding.icon3TextView.typeface = typefaceLight

        binding.icon4ImageView.setImageResource(R.drawable.non_colored_profile_icon)
        binding.icon4TextView.setTextColor(unselectedColor)
        binding.icon4TextView.textSize = 9f
        binding.icon4TextView.typeface = typefaceLight

        when (index) {
            0 -> {
                binding.icon1ImageView.setImageResource(R.drawable.colored_chat_icon)
                binding.icon1TextView.setTextColor(selectedColor)
                binding.icon1TextView.textSize = 11f
                binding.icon1TextView.typeface = typefaceMedium
            }
            1 -> {
                binding.icon2ImageView.setImageResource(R.drawable.colored_post_icon)
                binding.icon2TextView.setTextColor(selectedColor)
                binding.icon2TextView.textSize = 11f
                binding.icon2TextView.typeface = typefaceMedium
            }
            2 -> {
                binding.icon3ImageView.setImageResource(R.drawable.colored_friend_icon)
                binding.icon3TextView.setTextColor(selectedColor)
                binding.icon3TextView.textSize = 11f
                binding.icon3TextView.typeface = typefaceMedium
            }
            3 -> {
                binding.icon4ImageView.setImageResource(R.drawable.colored_profile_icon)
                binding.icon4TextView.setTextColor(selectedColor)
                binding.icon4TextView.textSize = 11f
                binding.icon4TextView.typeface = typefaceMedium
            }
        }
    }

    override fun onDestroyView() {
        pageCallback?.let { binding.viewPager.unregisterOnPageChangeCallback(it) }
        pageCallback = null
        _binding = null
        super.onDestroyView()
    }
}