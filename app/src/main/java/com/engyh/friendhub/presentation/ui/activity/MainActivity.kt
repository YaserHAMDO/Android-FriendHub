package com.engyh.friendhub.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.engyh.friendhub.R
import com.engyh.friendhub.databinding.ActivityMainBinding
import com.engyh.friendhub.presentation.viewmodel.PresenceViewModel
import com.engyh.friendhub.presentation.viewmodel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    
    private val presenceViewModel: PresenceViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val splashViewModel: SplashViewModel by viewModels()

        splashViewModel.load()

        splashScreen.setKeepOnScreenCondition {
            !splashViewModel.isReady.value
        }

        lifecycleScope.launch { presenceViewModel.start() }

        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_START -> presenceViewModel.onAppForeground()
                    androidx.lifecycle.Lifecycle.Event.ON_STOP -> presenceViewModel.onAppBackground()
                    else -> Unit
                }
            }
        )


        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.backImageView.setOnClickListener {
            navController.navigateUp()
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateTopBar(destination.id)
        }

        lifecycleScope.launch {
            splashViewModel.startDestination.collect { startDest ->
                if (startDest == null) return@collect

                val graph = navController.navInflater.inflate(R.navigation.nav_graph)
                graph.setStartDestination(startDest)
                navController.graph = graph

                handleIntent(intent)

                this.cancel()
            }
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getStringExtra("NAVIGATE_TO") == "EDIT_PROFILE") {
            val userId = intent.getStringExtra("USER_ID")
            val bundle = bundleOf("userId" to userId)
            navController.navigate(R.id.editFragment, bundle)
        }
    }

    private fun updateTopBar(destinationId: Int) {
        val onBoardingFragments = setOf(
            R.id.nameFragment,
            R.id.locationFragment,
            R.id.imageFragment,
            R.id.genderFragment,
            R.id.birthdateFragment,
            R.id.aboutFragment
        )

        if (destinationId in onBoardingFragments) {
            binding.topBar.visibility = View.VISIBLE
            updateProgressBar(destinationId)
        } else {
            binding.topBar.visibility = View.GONE
        }
    }

    private fun updateProgressBar(destinationId: Int) {
        resetProgress()

        when (destinationId) {
            R.id.nameFragment -> {
                setActive(binding.progressName)
            }
            R.id.locationFragment -> {
                setCompleted(binding.progressName)
                setActive(binding.progressLocation)
            }
            R.id.imageFragment -> {
                setCompleted(binding.progressName)
                setCompleted(binding.progressLocation)
                setActive(binding.progressImage)
            }
            R.id.genderFragment -> {
                setCompleted(binding.progressName)
                setCompleted(binding.progressLocation)
                setCompleted(binding.progressImage)
                setActive(binding.progressGender)
            }
            R.id.birthdateFragment -> {
                setCompleted(binding.progressName)
                setCompleted(binding.progressLocation)
                setCompleted(binding.progressImage)
                setCompleted(binding.progressGender)
                setActive(binding.progressBirthdate)
            }
            R.id.aboutFragment -> {
                setCompleted(binding.progressName)
                setCompleted(binding.progressLocation)
                setCompleted(binding.progressImage)
                setCompleted(binding.progressGender)
                setCompleted(binding.progressBirthdate)
                setActive(binding.progressAbout)
            }
        }
    }

    private fun resetProgress() {
        val inactiveDrawable = R.drawable.tab_gray_background
        binding.progressName.setBackgroundResource(inactiveDrawable)
        binding.progressLocation.setBackgroundResource(inactiveDrawable)
        binding.progressImage.setBackgroundResource(inactiveDrawable)
        binding.progressGender.setBackgroundResource(inactiveDrawable)
        binding.progressBirthdate.setBackgroundResource(inactiveDrawable)
        binding.progressAbout.setBackgroundResource(inactiveDrawable)
    }

    private fun setActive(view: View) {
        view.setBackgroundResource(R.drawable.tab_background)
    }

    private fun setCompleted(view: View) {
        view.setBackgroundResource(R.drawable.tab_background)
    }

}
