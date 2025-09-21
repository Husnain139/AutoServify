package com.hstan.autoservify.ui.main.home

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.hstan.autoservify.R
import com.hstan.autoservify.ui.auth.LoginActivity
import com.hstan.autoservify.ui.main.ViewModels.MainViewModel
import com.hstan.autoservify.ui.main.Shops.Services.ServicesActivity
import com.hstan.autoservify.ui.main.Shops.SpareParts.PartsCraftActivity
import com.hstan.autoservify.model.repositories.AuthRepository
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val viewModel: MainViewModel by viewModels()
    private var currentUserType: String = "customer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        val imageView = findViewById<ImageView>(R.id.drawer_icon)
        imageView.setOnClickListener {
            if (drawer.isDrawerVisible(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START)
            } else {
                drawer.openDrawer(GravityCompat.START)
            }
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            ?: return

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setupWithNavController(navHostFragment.navController)
        
        // Setup custom navigation for shopkeepers
        setupShopkeeperNavigation(bottomNavigationView)
        
        // Get user type
        checkUserType()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        when (item.itemId) {
            R.id.home -> {
                navigateToFragment(R.id.item_home)
            }
            R.id.orders -> {
                navigateToFragment(R.id.item_cart)
            }
            R.id.profile -> {
                navigateToFragment(R.id.item_profile)
            }
            R.id.logout -> {
                // Show logout confirmation dialog
                showLogoutConfirmationDialog()
            }
            else -> {
                Toast.makeText(this, "Invalid option", Toast.LENGTH_SHORT).show()
            }
        }
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun navigateToFragment(fragmentId: Int) {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(fragmentId)
    }

    private fun showLogoutConfirmationDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Are you sure you want to log out?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                // Perform logout action
                lifecycleScope.launch {
                    viewModel.logout()
                    // After logging out, navigate to LoginActivity
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish() // Finish the current activity to prevent going back
                }
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Dismiss the dialog if the user chooses "No"
            }
        val alert = dialogBuilder.create()
        alert.show()
    }

    private fun checkUserType() {
        lifecycleScope.launch {
            val authRepository = AuthRepository()
            val currentUser = authRepository.getCurrentUser()
            
            if (currentUser != null) {
                val result = authRepository.getUserProfile(currentUser.uid)
                if (result.isSuccess) {
                    val userProfile = result.getOrThrow()
                    currentUserType = userProfile.userType
                }
            }
        }
    }

    private fun setupShopkeeperNavigation(bottomNavigationView: BottomNavigationView) {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.item_home -> {
                    // Home navigation (works for both)
                    val navHostFragment = supportFragmentManager
                        .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    navHostFragment.navController.navigate(R.id.item_home)
                    true
                }
                R.id.item_search -> {
                    if (currentUserType == "shop_owner") {
                        // For shopkeepers: Search -> PartsCraft (Spare Parts)
                        startActivity(Intent(this, PartsCraftActivity::class.java))
                    } else {
                        // For customers: Search -> Search Fragment
                        val navHostFragment = supportFragmentManager
                            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                        navHostFragment.navController.navigate(R.id.item_search)
                    }
                    true
                }
                R.id.item_cart -> {
                    if (currentUserType == "shop_owner") {
                        // For shopkeepers: Orders -> Services
                        startActivity(Intent(this, ServicesActivity::class.java))
                    } else {
                        // For customers: Orders -> Orders Fragment  
                        val navHostFragment = supportFragmentManager
                            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                        navHostFragment.navController.navigate(R.id.item_cart)
                    }
                    true
                }
                R.id.item_profile -> {
                    // Profile navigation (works for both - keep as is)
                    val navHostFragment = supportFragmentManager
                        .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    navHostFragment.navController.navigate(R.id.item_profile)
                    true
                }
                else -> false
            }
        }
    }
}
