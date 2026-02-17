package com.engyh.friendhub.presentation.ui.fragment.home

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.engyh.friendhub.R
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.databinding.FragmentChatListBinding
import com.engyh.friendhub.presentation.ui.adapter.ChatListAdapter
import com.engyh.friendhub.presentation.ui.fragment.chat.ChatFragment
import com.engyh.friendhub.presentation.viewmodel.AppBootstrapViewModel
import com.engyh.friendhub.presentation.viewmodel.ChatListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatListFragment : Fragment(R.layout.fragment_chat_list) {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatListViewModel by viewModels()
    private val bootstrapViewModel: AppBootstrapViewModel by activityViewModels()

    private lateinit var chatListAdapter: ChatListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChatListBinding.bind(view)

        setupRecyclerViews()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    bootstrapViewModel.state.collect { state ->
                        if (state == AppBootstrapViewModel.BootstrapState.Ready) {
                            viewModel.start()
                        }
                    }
                }

                launch {
                    viewModel.chatsState.collect { state ->
                        when (state) {
                            is Response.Loading -> {
                                binding.progressBar.isVisible = true
                                binding.recyclerView.isVisible = false
                                binding.emptyChatsTextView.isVisible = false
                            }
                            is Response.Success -> {
                                binding.progressBar.isVisible = false
                                val chats = state.data
                                binding.emptyChatsTextView.isVisible = chats.isEmpty()
                                binding.recyclerView.isVisible = chats.isNotEmpty()
                                chatListAdapter.submitList(chats)
                            }
                            is Response.Error -> {
                                binding.progressBar.isVisible = false
                                binding.recyclerView.isVisible = false
                                binding.emptyChatsTextView.isVisible = true
                                binding.emptyChatsTextView.text = state.message
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerViews() {
        chatListAdapter = ChatListAdapter(
            onItemClick = { user ->
                val bundle = Bundle().apply {
                    putString(ChatFragment.ARG_RECEIVER_ID, user.userId)
                    putString(ChatFragment.ARG_USER_NAME, user.name)
                    putString(ChatFragment.ARG_IMAGE_URL, user.imageUrl)
                }
                findNavController().navigate(R.id.chatFragment, bundle)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = chatListAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}