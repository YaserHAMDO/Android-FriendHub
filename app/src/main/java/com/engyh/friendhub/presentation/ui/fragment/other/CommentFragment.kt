package com.engyh.friendhub.presentation.ui.fragment.other

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.engyh.friendhub.R
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.databinding.CommentFragmentBinding
import com.engyh.friendhub.presentation.ui.adapter.CommentAdapter
import com.engyh.friendhub.presentation.util.ImageLoader
import com.engyh.friendhub.presentation.util.showSnackbar
import com.engyh.friendhub.presentation.viewmodel.PostsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CommentFragment : BottomSheetDialogFragment(R.layout.comment_fragment) {

    private var _binding: CommentFragmentBinding? = null
    private val binding get() = _binding!!

    private val postsViewModel: PostsViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    private val commentsAdapter = CommentAdapter()

    private var commentsJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = CommentFragmentBinding.bind(view)

        val args = requireArguments()

        val postId = args.getString(ARG_POST_ID).orEmpty()
        val postOwnerId = args.getString(ARG_POST_OWNER_ID).orEmpty()
        val userName = args.getString(ARG_USER_NAME).orEmpty()
        val userImageUrl = args.getString(ARG_USER_IMAGE).orEmpty()
        val postText = args.getString(ARG_POST_TEXT).orEmpty()
        val postImageUrl = args.getString(ARG_POST_IMAGE).orEmpty()
        val timestamp = args.getString(ARG_TIMESTAMP).orEmpty()

        setupRecycler()

        binding.userName.text = userName
        binding.timeTextView.text = timestamp
        binding.postTextView.text = postText

        ImageLoader.loadUserImage(
            imageView = binding.userImage,
            url = userImageUrl,
            progress = binding.imageProgressBar)

        if (postImageUrl.isNotBlank()) {
            binding.postBackgroundConstraintLayout.isVisible = true

            ImageLoader.loadPostImage(
                imageView = binding.postImageView,
                url = postImageUrl,
                progressBar = binding.progressBar,
                binding.postBackgroundConstraintLayout,
                requireContext(),
                action = {  }
            )
        } else {
            binding.postBackgroundConstraintLayout.isVisible = false

        }

        postsViewModel.getComments(postId, postOwnerId)
        collectComments()

        binding.sendImageView.setOnClickListener {
            val text = binding.editTextName.text?.toString()?.trim().orEmpty()
            if (text.isNotBlank()) {
                postsViewModel.addComment(postId, postOwnerId, text)
                binding.editTextName.text?.clear()
            }
        }
    }

    private fun setupRecycler() {
        binding.commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.commentsRecyclerView.adapter = commentsAdapter
    }

    private fun collectComments() {
        commentsJob?.cancel()
        commentsJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postsViewModel.commentsState.collect { response ->
                    when (response) {
                        is Response.Success -> {
                            commentsAdapter.submitList(response.data)
                            if (response.data.isNotEmpty()) {
                                binding.commentsRecyclerView.smoothScrollToPosition(response.data.size - 1)
                            }
                        }
                        is Response.Error -> showSnackbar("Failed to load comments: ${response.message}")
                        else -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        commentsJob?.cancel()
        postsViewModel.clearCommentsState()
        _binding = null
    }

    companion object {
        private const val ARG_POST_ID = "arg_post_id"
        private const val ARG_POST_OWNER_ID = "arg_post_owner_id"
        private const val ARG_USER_NAME = "arg_user_name"
        private const val ARG_USER_IMAGE = "arg_user_image"
        private const val ARG_POST_TEXT = "arg_post_text"
        private const val ARG_POST_IMAGE = "arg_post_image"
        private const val ARG_TIMESTAMP = "arg_timestamp"

        fun newInstance(
            postId: String,
            postOwnerId: String,
            userName: String,
            userImageUrl: String,
            postText: String,
            postImageUrl: String,
            timestamp: String
        ): CommentFragment {
            return CommentFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_POST_ID, postId)
                    putString(ARG_POST_OWNER_ID, postOwnerId)
                    putString(ARG_USER_NAME, userName)
                    putString(ARG_USER_IMAGE, userImageUrl)
                    putString(ARG_POST_TEXT, postText)
                    putString(ARG_POST_IMAGE, postImageUrl)
                    putString(ARG_TIMESTAMP, timestamp)
                }
            }
        }
    }
}
