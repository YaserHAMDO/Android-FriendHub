package com.engyh.friendhub.presentation.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.engyh.friendhub.presentation.ui.fragment.home.ChatListFragment
import com.engyh.friendhub.presentation.ui.fragment.home.FriendFragment
import com.engyh.friendhub.presentation.ui.fragment.home.PostFragment
import com.engyh.friendhub.presentation.ui.fragment.home.ProfileFragment

class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ChatListFragment()
            1 -> PostFragment()
            2 -> FriendFragment()
            3 -> ProfileFragment()
            else -> ChatListFragment()
        }
    }
}