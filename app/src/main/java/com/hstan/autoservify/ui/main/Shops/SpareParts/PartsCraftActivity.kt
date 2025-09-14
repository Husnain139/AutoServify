package com.hstan.autoservify.ui.main.Shops.SpareParts

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
import com.hstan.autoservify.ui.Adapters.PartsCraftAdapter
import com.hstan.autoservify.model.repositories.AuthRepository
import kotlinx.coroutines.launch

class PartsCraftActivity : AppCompatActivity() {

    private lateinit var adapter: PartsCraftAdapter
    private lateinit var viewModel: PartsCraftViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partscraft)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Initialize adapter immediately with default values
        adapter = PartsCraftAdapter(
            items = mutableListOf(),
            showEditDeleteButtons = false // Default to customer view
        )
        recyclerView.adapter = adapter
        
        // Then update based on user role
        setupAdapterBasedOnUserRole()

        // Init ViewModel
        viewModel = ViewModelProvider(this).get(PartsCraftViewModel::class.java)

        // Observe LiveData
        viewModel.partsCrafts.observe(this) { partsCrafts ->
            adapter.updateData(partsCrafts)
        }

        // Observe delete status
        lifecycleScope.launch {
            viewModel.isSuccessfullyDeleted.collect { isDeleted ->
                isDeleted?.let {
                    if (it) {
                        Toast.makeText(this@PartsCraftActivity, "Part deleted successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@PartsCraftActivity, "Failed to delete part", Toast.LENGTH_SHORT).show()
                    }
                    viewModel.clearDeleteStatus()
                }
            }
        }

        // Observe failure messages
        lifecycleScope.launch {
            viewModel.failureMessage.collect { message ->
                message?.let {
                    Toast.makeText(this@PartsCraftActivity, it, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Load data
        viewModel.loadPartsCrafts()

        // Add new spare part
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
            val addButton = findViewById<ExtendedFloatingActionButton>(R.id.add_SpareParts)
            
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
                        startActivity(Intent(this@PartsCraftActivity, Addpartscraft::class.java))
                    }
                } else {
                    addButton.visibility = android.view.View.GONE
                }
            } else {
                addButton.visibility = android.view.View.GONE
            }
        }
    }

    private fun showDeleteConfirmationDialog(partsCraft: PartsCraft) {
        AlertDialog.Builder(this)
            .setTitle("Delete Spare Part")
            .setMessage("Are you sure you want to delete '${partsCraft.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                partsCraft.id?.let { viewModel.deletePartsCraft(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}