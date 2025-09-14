package com.hstan.autoservify.ui.main.Shops.Services

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.gson.Gson
import com.hstan.autoservify.R
import com.hstan.autoservify.ui.Adapters.ServiceAdapter
import com.hstan.autoservify.model.repositories.AuthRepository
import kotlinx.coroutines.launch

class ServicesActivity : AppCompatActivity() {

    private lateinit var adapter: ServiceAdapter
    private lateinit var viewModel: ServiceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_services)

        val recyclerView = findViewById<RecyclerView>(R.id.services_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter immediately with default values
        adapter = ServiceAdapter(
            items = mutableListOf(),
            onItemClick = { service ->
                val intent = Intent(this, Service_Detail_Activity::class.java)
                intent.putExtra("data", Gson().toJson(service))
                startActivity(intent)
            },
            showEditDeleteButtons = false // Default to customer view
        )
        recyclerView.adapter = adapter

        // Then update based on user role
        setupAdapterBasedOnUserRole()

        viewModel = ViewModelProvider(this).get(ServiceViewModel::class.java)

        // Observe list of services
        viewModel.services.observe(this) { services ->
            services?.let { adapter.updateData(it) }
        }

        // Observe delete status
        viewModel.isSuccessfullyDeleted.observe(this) { isDeleted ->
            isDeleted?.let {
                if (it) {
                    Toast.makeText(this, "Service deleted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to delete service", Toast.LENGTH_SHORT).show()
                }
                viewModel.clearDeleteStatus()
            }
        }

        // Observe failure messages
        viewModel.failureMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.loadServices()

        // FAB → go to Add Service screen
        // Setup Add button based on user role
        setupAddButtonBasedOnUserRole()
    }

    private fun setupAdapterBasedOnUserRole() {
        lifecycleScope.launch {
            val authRepository = AuthRepository()
            val currentUser = authRepository.getCurrentUser()
            
            if (currentUser != null) {
                val result = authRepository.getUserProfile(currentUser.uid)
                val isShopOwner = if (result.isSuccess) {
                    result.getOrThrow().userType == "shop_owner"
                } else {
                    false // Default to customer if profile not found
                }
                
                // Update the existing adapter permissions without losing data
                adapter.updatePermissions(isShopOwner)
            }
        }
    }

    private fun setupAddButtonBasedOnUserRole() {
        lifecycleScope.launch {
            val authRepository = AuthRepository()
            val currentUser = authRepository.getCurrentUser()
            val addButton = findViewById<ExtendedFloatingActionButton>(R.id.add_Service)
            
            if (currentUser != null) {
                val result = authRepository.getUserProfile(currentUser.uid)
                val isShopOwner = if (result.isSuccess) {
                    result.getOrThrow().userType == "shop_owner"
                } else {
                    false
                }
                
                if (isShopOwner) {
                    addButton.visibility = android.view.View.VISIBLE
                    addButton.setOnClickListener {
                        startActivity(Intent(this@ServicesActivity, Add_Service_Activity::class.java))
                    }
                } else {
                    addButton.visibility = android.view.View.GONE
                }
            } else {
                addButton.visibility = android.view.View.GONE
            }
        }
    }

    private fun showDeleteConfirmationDialog(service: Service) {
        AlertDialog.Builder(this)
            .setTitle("Delete Service")
            .setMessage("Are you sure you want to delete '${service.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                service.id?.let { viewModel.deleteService(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
