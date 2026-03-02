package com.odom.barcodeqr

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.gms.ads.MobileAds
import com.odom.barcodeqr.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.flow


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var backPressedTime: Long = 0
    private lateinit var reviewManager: com.google.android.play.core.review.ReviewManager
    private var reviewInfo: com.google.android.play.core.review.ReviewInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar!!.hide()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_scan, R.id.navigation_generate , R.id.navigation_history
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        
        // Initialize AdMob
        MobileAds.initialize(this) {}
        
        // Initialize in-app review
        initializeInAppReview()

        // Setup back press handler
        setupBackPressedHandler()
    }
    
    private fun setupBackPressedHandler() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val navController = findNavController(R.id.nav_host_fragment_activity_main)
                val currentFragment = navController.currentDestination?.id
                
                // Check if we're at top level fragments
                if (currentFragment in listOf(R.id.navigation_scan, R.id.navigation_generate, R.id.navigation_history)) {
                    if (System.currentTimeMillis() - backPressedTime < 2000) {
                        // Second back press within 2 seconds - exit app
                        finish()
                    } else {
                        // First back press - show toast and potentially show review
                        backPressedTime = System.currentTimeMillis()
                        Toast.makeText(this@MainActivity, getString(R.string.msg_back_press_to_exit), Toast.LENGTH_SHORT).show()
                        
                        // Show in-app review (random chance to show)
                        //showInAppReview()
                        initializeInAppReview()
                    }
                } else {
                    // Navigate back in fragment hierarchy
                    navController.popBackStack()
                }
            }
        }
        
        onBackPressedDispatcher.addCallback(this, callback)
    }
    
    private fun initializeInAppReview() {
        reviewManager = ReviewManagerFactory.create(this)
        val requestReviewFlow = reviewManager.requestReviewFlow()
        requestReviewFlow.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reviewInfo = task.result

                val reviewInfo = task.result
                (this as? Activity)?.let { activity ->
                    reviewManager.launchReviewFlow(activity, reviewInfo)
                }

            }
        }
    }

}