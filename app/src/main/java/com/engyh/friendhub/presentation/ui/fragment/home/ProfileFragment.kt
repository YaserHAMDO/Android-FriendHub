package com.engyh.friendhub.presentation.ui.fragment.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.engyh.friendhub.R
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.databinding.FragmentProfileBinding
import com.engyh.friendhub.domain.model.Post
import com.engyh.friendhub.presentation.ui.adapter.PostAdapter
import com.engyh.friendhub.presentation.ui.fragment.other.CommentFragment
import com.engyh.friendhub.presentation.ui.fragment.other.ImagePreviewFragment
import com.engyh.friendhub.presentation.util.DateTimeUtils.formatTimestamp
import com.engyh.friendhub.presentation.util.DateTimeUtils.getAgeFromBirthdateSafe
import com.engyh.friendhub.presentation.util.GeocodingHelper.getCityFromLatLng
import com.engyh.friendhub.presentation.util.ImageLoader
import com.engyh.friendhub.presentation.util.ShareHelper.share
import com.engyh.friendhub.presentation.util.showSnackbar
import com.engyh.friendhub.presentation.viewmodel.ProfileUiState
import com.engyh.friendhub.presentation.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var postAdapter: PostAdapter

    private var uniqueId: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        setupRecyclerView()
        setupListeners()
        setupObservers()

        viewModel.start()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            onUserImageClick = {},
            onLikeClick = { postId, ownerId, liked ->
                viewModel.likePost(postId, ownerId, liked)
            },
            onCommentClick = { post ->
                showCommentsBottomSheet(post)
            },
            onImageClick = { post, _ ->
                navigateToImagePreview(post)
            },
            onShareClick = { post ->
                sharePost(post)
            }
        )

        binding.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
        }
    }

    private fun setupListeners() {
        binding.editProfile.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_edit)
        }

        binding.shareProfile.setOnClickListener {
            if (uniqueId.isNotBlank()) {
                share(uniqueId, requireActivity())
            }
        }

        binding.settingsImageView.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_settings)
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiState.collect { response ->
                        when (response) {
                            is Response.Loading -> {
                            }

                            is Response.Error -> {
                                showSnackbar(response.message)
                            }

                            is Response.Success -> {
                                renderUi(response.data)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderUi(state: ProfileUiState) {
        val user = state.user ?: return

        binding.userNameTextView.text = user.name
        binding.aboutTextView.text = user.about

        binding.friendCountTextView.text = state.friendsCount.toString()
        binding.postCountTextView.text = state.postsCount.toString()
        binding.distanceTextView.text = "0 km"

        uniqueId = user.uniqueId

        ImageLoader.loadUserImage(
            imageView = binding.userImageView,
            url = user.imageUrl,
            progress = binding.imageProgressBar
        )

        val age = getAgeFromBirthdateSafe(user.birthdate)
        val genderPart = if (user.gender == "Prefer not to say") null else user.gender

        getCityFromLatLng(requireContext(), user.lat, user.lon) { city ->
            val parts = mutableListOf<String>()
            if (!genderPart.isNullOrBlank()) parts.add(genderPart)
            if (age != null) parts.add("$age years")
            if (!city.isNullOrBlank()) parts.add(city)

            binding.userMetaTextView.text = parts.joinToString(" - ")
        }

        postAdapter.submitList(state.posts)
    }

    private fun navigateToImagePreview(post: Post) {
        findNavController().navigate(
            R.id.action_main_to_imagePreview,
            ImagePreviewFragment.newArgs(
                imageUrl = post.imageUrl,
                senderName = post.userName,
                time = formatTimestamp(post.timestamp)
            )
        )
    }

    private fun showCommentsBottomSheet(post: Post) {
        val sheet = CommentFragment.newInstance(
            postId = post.postId,
            postOwnerId = post.userId,
            userName = post.userName,
            userImageUrl = post.userImageUrl,
            postText = post.text,
            postImageUrl = post.imageUrl,
            timestamp = formatTimestamp(post.timestamp)
        )
        sheet.show(childFragmentManager, "comments_sheet")
    }

    private fun sharePost(post: Post) {
        val text = "*${post.userName}*:\n${post.text}"
        val sentText = if (post.imageUrl.isNotEmpty()) "$text\n\n${post.imageUrl}" else text
        share(sentText, requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}