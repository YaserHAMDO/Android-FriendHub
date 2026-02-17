package com.engyh.friendhub.di

import com.engyh.friendhub.data.repository.AuthenticationRepositoryImpl
import com.engyh.friendhub.data.repository.ChatListRepositoryImpl
import com.engyh.friendhub.data.repository.ChatRepositoryImpl
import com.engyh.friendhub.data.repository.ConnectionsRepositoryImpl
import com.engyh.friendhub.data.repository.PresenceRepositoryImpl
import com.engyh.friendhub.data.repository.UserStatusRepositoryImpl
import com.engyh.friendhub.data.repository.FriendsRepositoryImpl
import com.engyh.friendhub.data.repository.PostsRepositoryImpl
import com.engyh.friendhub.data.repository.StorageRepositoryImpl
import com.engyh.friendhub.data.repository.UserRepositoryImpl
import com.engyh.friendhub.domain.repository.AuthenticationRepository
import com.engyh.friendhub.domain.repository.ChatListRepository
import com.engyh.friendhub.domain.repository.ChatRepository
import com.engyh.friendhub.domain.repository.ConnectionsRepository
import com.engyh.friendhub.domain.repository.FriendsRepository
import com.engyh.friendhub.domain.repository.PostsRepository
import com.engyh.friendhub.domain.repository.PresenceRepository
import com.engyh.friendhub.domain.repository.StorageRepository
import com.engyh.friendhub.domain.repository.UserRepository
import com.engyh.friendhub.domain.repository.UserStatusRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton abstract fun bindAuth(impl: AuthenticationRepositoryImpl): AuthenticationRepository
    @Binds @Singleton abstract fun bindChatList(impl: ChatListRepositoryImpl): ChatListRepository
    @Binds @Singleton abstract fun bindFriends(impl: FriendsRepositoryImpl): FriendsRepository
    @Binds @Singleton abstract fun bindPosts(impl: PostsRepositoryImpl): PostsRepository
    @Binds @Singleton abstract fun bindPresence(impl: PresenceRepositoryImpl): PresenceRepository
    @Binds @Singleton abstract fun bindUserStatus(impl: UserStatusRepositoryImpl): UserStatusRepository

    @Binds @Singleton abstract fun bindStorage(impl: StorageRepositoryImpl): StorageRepository
    @Binds @Singleton abstract fun bindUser(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton abstract fun bindChat(impl: ChatRepositoryImpl): ChatRepository
    @Binds @Singleton abstract fun bindConnections(impl: ConnectionsRepositoryImpl): ConnectionsRepository
}
