package com.engyh.friendhub.presentation.ui.fragment.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.engyh.friendhub.R
import com.engyh.friendhub.databinding.FragmentChatBinding
import com.engyh.friendhub.domain.model.Message
import com.engyh.friendhub.presentation.ui.adapter.ChatAdapter
import com.engyh.friendhub.presentation.ui.adapter.ChatAudioPlayer
import com.engyh.friendhub.presentation.ui.fragment.other.ImagePreviewFragment
import com.engyh.friendhub.presentation.ui.fragment.other.ImageSourceFragment
import com.engyh.friendhub.presentation.util.ChatAudioRecorder
import com.engyh.friendhub.presentation.util.ImageLoader
import com.engyh.friendhub.presentation.util.ImagePickerManager
import com.engyh.friendhub.presentation.util.showSnackbar
import com.engyh.friendhub.presentation.viewmodel.ChatViewModel
import com.vanniktech.emoji.EmojiPopup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ChatFragment : Fragment(R.layout.fragment_chat) {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()

    private var receiverId: String = ""
    private var otherUserName: String = ""
    private var otherUserImageUrl: String = ""

    private var chatAdapter: ChatAdapter? = null

    private var pickedBitmap: Uri? = null

    private var audioRecorder: ChatAudioRecorder? = null

    private var recordTimer: CountDownTimer? = null
    private var recordSeconds: Long = 0L

    private val audioPlayer by lazy { ChatAudioPlayer() }

    private var emojiPopup: EmojiPopup? = null

    private lateinit var imagePickerManager: ImagePickerManager

    private var typingJob: Job? = null
    private val TYPING_IDLE_MS = 1200L

    private var firstScroll = true
    private var lastListSize = 0

    private var keyboardListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    private val requestRecordPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) showSnackbar("Record audio permission denied")
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChatBinding.bind(view)

        if (!readArgs()) return
        setupHeader()
        setupRecorder()
        setupRecycler()
        setupEmoji()
        setupImagePicker()
        setupImageUi()
        setupsendImageViewUi()
        setupScrollDownButton()
        setupKeyboardVisibility()
        observeUi()

        viewModel.initChat(receiverId)
    }

    private fun getArgData() {
        receiverId = requireArguments().getString(ARG_RECEIVER_ID).orEmpty()
        otherUserName = requireArguments().getString(ARG_USER_NAME).orEmpty()
        otherUserImageUrl = requireArguments().getString(ARG_IMAGE_URL).orEmpty()

        if (receiverId.isBlank()) {
            findNavController().navigateUp()
            return
        }
    }

    private fun readArgs(): Boolean {
        receiverId = requireArguments().getString(ARG_RECEIVER_ID).orEmpty()
        otherUserName = requireArguments().getString(ARG_USER_NAME).orEmpty()
        otherUserImageUrl = requireArguments().getString(ARG_IMAGE_URL).orEmpty()

        if (receiverId.isBlank()) {
            findNavController().navigateUp()
            return false
        }
        return true
    }

    private fun setupHeader() {
        binding.userNameTextView.text = otherUserName

        if (otherUserImageUrl.isNotBlank()) {
            ImageLoader.loadUserImage(
                imageView = binding.userImageView,
                url = otherUserImageUrl,
                progress = binding.ImageProgressBar,
            )
        }

        binding.userImageView.setOnClickListener {
            val bundle = Bundle().apply {
                putString("userId", receiverId)
                putString("canChat", "true")
                putString("fromChat", "true")
            }
            findNavController().navigate(R.id.action_chat_to_userInfoFragment, bundle)
        }

        binding.menuImageView.setOnClickListener { showPopupMenu(it) }
    }

    private fun setupRecorder() {
        audioRecorder = ChatAudioRecorder(requireContext().applicationContext)
    }

    private fun setupRecycler() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
            setHasFixedSize(true)
        }
    }

    private fun setupEmoji() {
        emojiPopup = EmojiPopup(binding.root, binding.messageEditText)

        binding.emojisImageView.setOnClickListener {
            val popup = emojiPopup ?: return@setOnClickListener
            if (popup.isShowing) {
                popup.dismiss()
                binding.emojisImageView.setImageResource(R.drawable.baseline_insert_emoticon_24)
            } else {
                popup.toggle()
                binding.emojisImageView.setImageResource(R.drawable.twotone_keyboard_24)
            }
        }
    }

    private fun setupImagePicker() {
        imagePickerManager = ImagePickerManager(this) { uri ->
            if (uri == null) {
                showSnackbar("No image selected")
            } else {
                showImagePreview(uri)
            }
        }
    }

    private fun setupImageUi() {
        binding.cameraImageView.setOnClickListener { showImageSourceBottomSheet() }
        binding.cancelImageView.setOnClickListener { hideImagePreview() }

        binding.sendPreviewedImageView.setOnClickListener {
            val bmp = pickedBitmap ?: return@setOnClickListener
            val caption = binding.imageTextEditText.text?.toString().orEmpty()

            binding.imageProgressBar.isVisible = true
            binding.textMessageLinearLayout.isVisible = false

            viewModel.sendImageMessage(receiverId, bmp, caption)

            binding.imageTextEditText.text?.clear()
            hideImagePreview()
        }
    }

    private fun showImageSourceBottomSheet() {
        val bottomSheet = ImageSourceFragment(
            onGalleryClick = { imagePickerManager.launchGallery() },
            onCameraClick = { imagePickerManager.launchCamera() }
        )
        bottomSheet.show(parentFragmentManager, bottomSheet.tag)
    }

    private fun showImagePreview(uri: Uri) {
        try {
            val input = requireContext().contentResolver.openInputStream(uri) ?: return
            val bmp = BitmapFactory.decodeStream(input)
            pickedBitmap = uri

            binding.previewLinearLayout.isVisible = true
            binding.previewImageView.setImageURI(uri)
        } catch (e: Exception) {
            Log.e("ChatFragment", "Image decode failed", e)
            showSnackbar("Failed to load image")
        }
    }

    private fun hideImagePreview() {
        pickedBitmap = null
        binding.previewLinearLayout.isVisible = false
        binding.imageProgressBar.isVisible = false
        binding.textMessageLinearLayout.isVisible = true
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupsendImageViewUi() {
        binding.messageEditText.doAfterTextChanged { editable ->
            val hasText = !editable?.toString().isNullOrBlank()

            binding.sendImageView.setImageResource(
                if (hasText) R.drawable.baseline_send_24
                else R.drawable.baseline_keyboard_voice_24
            )

            if (hasText) {
                viewModel.setTyping(true)
                typingJob?.cancel()
                typingJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(TYPING_IDLE_MS)
                    viewModel.setTyping(false)
                }
            } else {
                typingJob?.cancel()
                viewModel.setTyping(false)
            }
        }

        binding.sendImageView.setOnTouchListener { _, event ->
            val textNow = binding.messageEditText.text?.toString().orEmpty().trim()
            val voiceMode = textNow.isBlank()

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (voiceMode) startRecordingFlow()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!voiceMode) sendText()
                }
            }
            true
        }

        binding.sendVoiceImageView.setOnClickListener { finishAndSendRecording() }
        binding.trashImageView.setOnClickListener { cancelRecording() }
    }

    private fun sendText() {
        val text = binding.messageEditText.text?.toString().orEmpty().trim()
        if (text.isBlank()) return

        viewModel.sendMessage(receiverId, text)
        binding.messageEditText.setText("")
        viewModel.setTyping(false)
    }

    private fun startRecordingFlow() {
        if (!hasRecordPermission()) {
            requestRecordPermission.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        binding.recodeConstraintLayout.isVisible = true
        binding.lottieAnimationView.playAnimation()

        recordSeconds = 0
        startRecordTimer()

        runCatching { audioRecorder?.start() }.onFailure { e ->
            Log.e("ChatFragment", "startRecording failed", e)
            showSnackbar("Recording failed")
            cancelRecordingUiOnly()
        }
    }

    private fun finishAndSendRecording() {
        stopRecordTimer()

        val result = audioRecorder?.stop()
        binding.recodeConstraintLayout.isVisible = false
        binding.lottieAnimationView.cancelAnimation()

        if (result == null) {
            showSnackbar("Recording failed")
            return
        }

        if (result.durationMs < 1000) {
            showSnackbar("Voice too short!")
            runCatching { result.file.delete() }
            return
        }

        binding.messageProgressBar.isVisible = true
        binding.sendImageView.isVisible = false

        viewModel.sendVoiceMessage(receiverId, Uri.fromFile(result.file), result.durationMs)

        binding.messageProgressBar.isVisible = false
        binding.sendImageView.isVisible = true

        runCatching { result.file.delete() }
    }

    private fun cancelRecording() {
        stopRecordTimer()
        audioRecorder?.cancel()
        cancelRecordingUiOnly()
    }

    private fun cancelRecordingUiOnly() {
        binding.recodeConstraintLayout.isVisible = false
        binding.lottieAnimationView.cancelAnimation()
    }

    private fun startRecordTimer() {
        recordTimer?.cancel()
        recordTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                recordSeconds++
                val m = recordSeconds / 60
                val s = recordSeconds % 60
                binding.recodeCounterTextView.text = String.format("%02d:%02d", m, s)
            }
            override fun onFinish() = Unit
        }.start()
    }

    private fun stopRecordTimer() {
        recordTimer?.cancel()
        recordTimer = null
    }

    private fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    private fun setupScrollDownButton() {
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val lastVisible = lm.findLastVisibleItemPosition()
                val total = chatAdapter?.itemCount ?: 0
                binding.arrowDownFrameLayout.isVisible = total > 5 && lastVisible < total - 4
            }
        })

        binding.arrowDownFrameLayout.setOnClickListener {
            val total = chatAdapter?.itemCount ?: 0
            if (total > 0) binding.recyclerView.smoothScrollToPosition(total - 1)
        }
    }

    private fun setupKeyboardVisibility() {
        observeKeyboardVisibility(binding.root) { isKeyboardVisible ->
            if (isKeyboardVisible) {
                val total = chatAdapter?.itemCount ?: 0
                if (total > 0) binding.recyclerView.scrollToPosition(total - 1)
            }
        }
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.currentUserId.collect { uid ->
                        val id = uid ?: return@collect
                        setupAdapter(id)
                    }
                }

                launch {
                    viewModel.messages.collect { list ->
                        val adapter = chatAdapter ?: return@collect

                        val lm = binding.recyclerView.layoutManager as? LinearLayoutManager
                        val lastVisible = lm?.findLastVisibleItemPosition() ?: -1
                        val isNearBottom = lastVisible >= (lastListSize - 3)

                        adapter.submitList(list) {
                            if (list.isNotEmpty() && (firstScroll || isNearBottom)) {
                                firstScroll = false
                                binding.recyclerView.scrollToPosition(list.lastIndex)
                            }
                            lastListSize = list.size
                        }
                    }
                }

                launch {
                    viewModel.typingStatus.collect { isTyping ->
                        binding.typingTextView?.isVisible = isTyping
                    }
                }

                launch {
                    viewModel.uploading.collect { uploading ->
                        binding.messageProgressBar.isVisible = uploading
                        binding.sendImageView.isVisible = !uploading
                    }
                }

                launch {
                    viewModel.friendOnlineState(receiverId).collect { isOnline ->
                        binding.isOnlineView.isVisible = isOnline
                    }
                }
            }
        }
    }

    private fun setupAdapter(currentUserId: String) {
        if (chatAdapter != null) return

        val adapter = ChatAdapter(
            currentUserId = currentUserId,
            listener = object : ChatAdapter.Listener {

                override fun onAudioToggle(message: Message) {
                    val url = message.audioUrl
                    if (url.isNullOrBlank()) return

                    val nowPlaying = audioPlayer.toggle(
                        key = message.messageId,
                        url = url,
                        onFinished = { chatAdapter?.setPlayingMessage(null) }
                    )
                    chatAdapter?.setPlayingMessage(nowPlaying)
                }

                override fun onImageClick(message: Message) {
                    val senderName = if (message.senderId == currentUserId) "You" else otherUserName
                    val time = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        .format(Date(message.time))

                    findNavController().navigate(
                        R.id.action_chat_to_imagePreview,
                        ImagePreviewFragment.newArgs(
                            imageUrl = message.imageUrl,
                            senderName = senderName,
                            time = time
                        )
                    )
                }
            }
        )

        chatAdapter = adapter
        binding.recyclerView.adapter = adapter
    }

    private fun showPopupMenu(anchor: View) {
        val popupMenu = PopupMenu(requireContext(), anchor)
        popupMenu.inflate(R.menu.chat_menu)

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_clear_chat -> {
                    viewModel.clearChatForMe()
                    true
                }
                R.id.action_block_user -> { showSnackbar("Block user"); true }
                R.id.action_mute -> { showSnackbar("Mute"); true }
                R.id.action_report -> { showSnackbar("Report"); true }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun observeKeyboardVisibility(rootView: View, onChanged: (Boolean) -> Unit) {
        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            private var last = false

            override fun onGlobalLayout() {
                val rect = Rect()
                rootView.getWindowVisibleDisplayFrame(rect)

                val screenHeight = rootView.rootView.height
                val keypadHeight = screenHeight - rect.height()
                val isVisible = keypadHeight > 200

                if (isVisible != last) {
                    last = isVisible
                    onChanged(isVisible)
                }
            }
        }

        keyboardListener = listener
        rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    override fun onPause() {
        super.onPause()
        viewModel.setTyping(false)
        audioPlayer.stop()
        hideImagePreview()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        stopRecordTimer()
        audioRecorder?.release()
        audioRecorder = null
        audioPlayer.release()

        emojiPopup?.dismiss()
        emojiPopup = null

        keyboardListener?.let { l ->
            binding.root.viewTreeObserver.removeOnGlobalLayoutListener(l)
        }
        keyboardListener = null

        chatAdapter = null
        _binding = null
    }

    companion object {
        const val ARG_RECEIVER_ID = "receiverId"
        const val ARG_USER_NAME = "userName"
        const val ARG_IMAGE_URL = "imageUrl"
    }
}