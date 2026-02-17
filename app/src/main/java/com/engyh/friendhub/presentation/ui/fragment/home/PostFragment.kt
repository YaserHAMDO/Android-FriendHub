package com.engyh.friendhub.presentation.ui.fragment.home

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.engyh.friendhub.R
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.databinding.DialogAddPostBinding
import com.engyh.friendhub.databinding.FragmentPostBinding
import com.engyh.friendhub.domain.model.Post
import com.engyh.friendhub.presentation.ui.adapter.PostAdapter
import com.engyh.friendhub.presentation.ui.fragment.other.CommentFragment
import com.engyh.friendhub.presentation.ui.fragment.other.ImagePreviewFragment
import com.engyh.friendhub.presentation.util.DateTimeUtils.formatTimestamp
import com.engyh.friendhub.presentation.util.ShareHelper.share
import com.engyh.friendhub.presentation.util.showSnackbar
import com.engyh.friendhub.presentation.viewmodel.PostsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PostFragment : Fragment(R.layout.fragment_post) {

    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostsViewModel by viewModels()

    private lateinit var postAdapter: PostAdapter

    private var selectedImageUri: Uri? = null
    private var dialogPostImage: ImageView? = null

    private var pendingScroll = false

    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                dialogPostImage?.setImageURI(uri)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPostBinding.bind(view)

        setupRecyclerView()
        setupListeners()
        setupObservers()

        viewModel.start()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            onUserImageClick = { userId ->
                pendingScroll = true
                val bundle = Bundle().apply {
                    putString("userId", userId)
                    putString("canChat", "true")
                    putString("fromChat", "false")
                }
                findNavController().navigate(R.id.action_main_to_userInfo, bundle)
            },
            onLikeClick = { postId, ownerId, liked ->
                pendingScroll = true
                viewModel.likePost(postId, ownerId, liked)
            },
            onCommentClick = { post ->
                pendingScroll = true
                showCommentsBottomSheet(post)
            },
            onImageClick = { post, _ ->
                pendingScroll = true
                navigateToImagePreview(post)
            },
            onShareClick = { post ->
                pendingScroll = true
                sharePost(post)
            }
        )

        binding.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        binding.fab.setOnClickListener { showAddPostDialog() }

        binding.swipeRefreshLayout.setOnRefreshListener {
            pendingScroll = false
            viewModel.refreshPosts()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.postsState.collect { response ->
                        binding.swipeRefreshLayout.isRefreshing = response is Response.Loading

                        when (response) {
                            is Response.Loading -> {
                            }

                            is Response.Success -> {
                                val posts = response.data
                                postAdapter.submitList(posts)

                                binding.emptyPostsText.isVisible = posts.isEmpty()

                                if (!pendingScroll && posts.isNotEmpty()) {
                                    binding.postsRecyclerView.scrollToPosition(0)
                                }
                            }

                            is Response.Error -> {
                                binding.emptyPostsText.isVisible = true
                                showSnackbar("Failed to fetch posts: ${response.message}")
                            }
                        }
                    }
                }

                launch {
                    viewModel.createPostState.collect { response ->
                        if (response == null) return@collect

                        when (response) {
                            is Response.Success -> {
                                showSnackbar("Post created successfully!")
                                viewModel.resetCreatePostState()
                                pendingScroll = false
                                binding.postsRecyclerView.scrollToPosition(0)
                            }

                            is Response.Error -> {
                                showSnackbar("Failed to create post: ${response.message}")
                                viewModel.resetCreatePostState()
                            }

                            is Response.Loading -> Unit
                        }
                    }
                }
            }
        }
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

    private fun showAddPostDialog() {
        val dialogBinding = DialogAddPostBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogPostImage = dialogBinding.postImage
        selectedImageUri = null

        dialogBinding.postImage.setOnClickListener { getContent.launch("image/*") }

        dialogBinding.publishButton.setOnClickListener {
            val text = dialogBinding.postEditText.text.toString().trim()
            if (text.isEmpty() && selectedImageUri == null) {
                showSnackbar("Please enter text or select an image")
                return@setOnClickListener
            }

            viewModel.createPost(text, selectedImageUri)
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        dialogPostImage = null
    }
}