package com.engyh.friendhub.presentation.ui.fragment.other

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
import com.engyh.friendhub.databinding.FragmentUserInfoBinding
import com.engyh.friendhub.domain.model.Post
import com.engyh.friendhub.presentation.ui.adapter.PostAdapter
import com.engyh.friendhub.presentation.ui.fragment.chat.ChatFragment
import com.engyh.friendhub.presentation.util.DateTimeUtils.formatTimestamp
import com.engyh.friendhub.presentation.util.DateTimeUtils.getAgeFromBirthdateSafe
import com.engyh.friendhub.presentation.util.GeocodingHelper.getCityFromLatLng
import com.engyh.friendhub.presentation.util.ImageLoader
import com.engyh.friendhub.presentation.util.ShareHelper.share
import com.engyh.friendhub.presentation.util.showSnackbar
import com.engyh.friendhub.presentation.viewmodel.UserInfoViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserInfoFragment : Fragment(R.layout.fragment_user_info) {

    private var _binding: FragmentUserInfoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserInfoViewModel by viewModels()

    private lateinit var postAdapter: PostAdapter

    private var fromChat: String = ""
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUserInfoBinding.bind(view)

        val userId = arguments?.getString("userId")
        fromChat = arguments?.getString("fromChat").toString()
        val canChat = arguments?.getString("canChat")
        if (canChat == "false") {
            binding.chatWithTextCardView.visibility = View.GONE
        }

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        if (userId != null) {
            viewModel.loadCurrentAndTargetUserProfiles(userId)
            viewModel.fetchUserPosts(userId)
        }
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            onUserImageClick = { userId ->

            },
            onLikeClick = { postId, postOwnerId, liked ->
                viewModel.likePost(postId, postOwnerId, liked)
            },
            onCommentClick = { post ->
                showCommentsBottomSheet(post)
            },
            onImageClick = { post, imageView ->
                findNavController().navigate(
                    R.id.action_user_info_to_imagePreview,
                    ImagePreviewFragment.newArgs(
                        imageUrl = post.imageUrl,
                        senderName = post.userName,
                        time = formatTimestamp(post.timestamp)
                    )
                )
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
    private fun sharePost(post: Post) {
        val text = "*${post.userName}*:\n${post.text}"
        val sentText = if (post.imageUrl.isNotEmpty()) "$text\n\n${post.imageUrl}" else text
        share(sentText, requireContext())
    }
    private fun setupListeners() {
        binding.backImageView.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.user.collect { user ->
                        if (user != null) {
                            binding.userNameTextView.text = user.name
                            binding.aboutTextView.text = user.about
                            binding.imageProgressBar.visibility = View.VISIBLE

                            ImageLoader.loadUserImage(
                                imageView = binding.userImageView,
                                url = user.imageUrl,
                                progress = binding.imageProgressBar,
                            )


                            if (user.gender == "Prefer not to say") {
                                getCityFromLatLng(requireContext(), user.lat, user.lon) { city ->
                                    binding.userMetaTextView.text = "" + getAgeFromBirthdateSafe(user.birthdate) + " years - " + city ?: "" + getAgeFromBirthdateSafe(user.birthdate) + "years"
                                }
                            }
                            else {
                                getCityFromLatLng(requireContext(), user.lat, user.lon) { city ->
                                    binding.userMetaTextView.text = user.gender + " - " + getAgeFromBirthdateSafe(user.birthdate) + " years - " + city ?: user.gender + " - " + getAgeFromBirthdateSafe(user.birthdate) + "years"
                                }
                            }

                            binding.profileTitle.text = "User Profile"
                            binding.chatWithTextView.text = "Chat with ${user.name}"


                            binding.chatWithTextCardView.setOnClickListener {

                                if (fromChat == "true") {
                                    findNavController().navigateUp()
                                    return@setOnClickListener
                                }

                                val bundle = Bundle().apply {
                                    putString(ChatFragment.ARG_RECEIVER_ID, user.userId)
                                    putString(ChatFragment.ARG_USER_NAME, user.name)
                                    putString(ChatFragment.ARG_IMAGE_URL, user.imageUrl)
                                }
                                findNavController().navigate(R.id.chatFragment, bundle)

                            }

                        }
                    }
                }

                launch {
                    viewModel.friendsCount.collect { count ->
                        binding.friendCountTextView.text = "$count"
                    }
                }

                launch {
                    viewModel.postsCount.collect { count ->
                        binding.postCountTextView.text = "$count"
                    }
                }

                launch {
                    viewModel.distance.collect { distance ->
                        if (distance != null) {
                            if (distance < 1000) {
                                binding.distanceTextView.text = String.format("%.0f m", distance)
                            }
                            else {
                                binding.distanceTextView.text = String.format("%.1f km", distance / 1000)
                            }
                        }
                    }
                }

                launch {
                    viewModel.postsState.collect { response ->
                        when (response) {
                            is Response.Success -> {
                                postAdapter.submitList(response.data)
                            }
                            is Response.Error -> {
                                showSnackbar("Failed to fetch posts: ${response.message}")
                            }
                            else -> {}
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
