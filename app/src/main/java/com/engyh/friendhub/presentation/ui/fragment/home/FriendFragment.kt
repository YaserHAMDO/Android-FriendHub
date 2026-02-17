package com.engyh.friendhub.presentation.ui.fragment.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
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
import com.engyh.friendhub.databinding.DialogSearchUsersBinding
import com.engyh.friendhub.databinding.FragmentFriendBinding
import com.engyh.friendhub.presentation.ui.adapter.UserAdapter
import com.engyh.friendhub.presentation.util.showSnackbar
import com.engyh.friendhub.presentation.viewmodel.FriendsUiState
import com.engyh.friendhub.presentation.viewmodel.FriendsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FriendFragment : Fragment(R.layout.fragment_friend) {

    private var _binding: FragmentFriendBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FriendsViewModel by viewModels()

    private lateinit var requestsAdapter: UserAdapter
    private lateinit var friendsAdapter: UserAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFriendBinding.bind(view)

        setupRecyclerViews()
        setupListeners()
        setupObservers()

        viewModel.start()
    }

    private fun setupRecyclerViews() {
        requestsAdapter = UserAdapter(
            onItemClick = { user ->
                val bundle = Bundle().apply {
                    putString("userId", user.userId)
                    putString("canChat", "false")
                    putString("fromChat", "false")
                }
                findNavController().navigate(R.id.action_main_to_userInfo, bundle)
            },
            onUserClick = { user -> viewModel.acceptRequest(user) },
            onUserRejectClick = { user -> viewModel.rejectRequest(user) },
            onSendRequestClick = { },
            type = 1
        )
        binding.requestsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.requestsRecyclerView.adapter = requestsAdapter

        friendsAdapter = UserAdapter(
            onItemClick = { user ->
                val bundle = Bundle().apply {
                    putString("userId", user.userId)
                    putString("fromChat", "false")
                }
                findNavController().navigate(R.id.action_main_to_userInfo, bundle)
            },
            onUserClick = { },
            onUserRejectClick = { },
            onSendRequestClick = { },
            type = 2
        )
        binding.friendsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.friendsRecyclerView.adapter = friendsAdapter
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is Response.Loading -> {
                                binding.progressBar.isVisible = true
                                binding.contentLinearLayout.isVisible = false
                            }

                            is Response.Success -> {
                                binding.progressBar.isVisible = false
                                binding.contentLinearLayout.isVisible = true

                                val ui: FriendsUiState = state.data

                                requestsAdapter.submitList(ui.requests)
                                binding.requestsTitleTextView.isVisible = ui.requests.isNotEmpty()
                                binding.requestsRecyclerView.isVisible = ui.requests.isNotEmpty()

                                friendsAdapter.submitList(ui.friends)
                                binding.noFriendsTextView.isVisible = ui.friends.isEmpty()
                                binding.friendsRecyclerView.isVisible = ui.friends.isNotEmpty()
                            }

                            is Response.Error -> {
                                binding.progressBar.isVisible = false
                                binding.contentLinearLayout.isVisible = true
                                binding.noFriendsTextView.isVisible = true
                                binding.noFriendsTextView.text = state.message
                            }
                        }
                    }
                }

                launch {
                    viewModel.operationEvents.collect { event ->
                        when (event) {
                            is Response.Loading -> {
                            }
                            is Response.Success -> {
                            }
                            is Response.Error -> {
                                showSnackbar(event.message)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.fab.setOnClickListener { showSearchDialog() }
    }

    private fun showSearchDialog() {
        val dialogBinding = DialogSearchUsersBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val searchAdapter = UserAdapter(
            onItemClick = { },
            onUserClick = { },
            onUserRejectClick = { },
            onSendRequestClick = { user ->
                viewModel.sendRequest(user)
                dialog.dismiss()
            },
            type = 0
        )

        dialogBinding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }

        dialogBinding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.trim()?.takeIf { it.isNotBlank() }?.let { viewModel.searchUsers(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean = false
        })

        var searchJob: Job? = null
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchState.collect { state ->
                    when (state) {
                        is Response.Loading -> {
                        }
                        is Response.Success -> {
                            searchAdapter.submitList(state.data)
                        }
                        is Response.Error -> {
                            showSnackbar(state.message)
                        }
                    }
                }
            }
        }

        dialog.setOnDismissListener { searchJob?.cancel() }
        dialogBinding.closeButton.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}