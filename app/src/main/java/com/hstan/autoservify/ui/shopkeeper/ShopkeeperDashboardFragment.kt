package com.hstan.autoservify.ui.shopkeeper

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.hstan.autoservify.R
import com.hstan.autoservify.ui.main.Shops.AddShopActivity
import com.hstan.autoservify.ui.main.Shops.Shop
import kotlinx.coroutines.launch
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.model.repositories.ShopRepository
import com.hstan.autoservify.model.repositories.OrderRepository
import com.hstan.autoservify.model.repositories.AppointmentRepository
import com.hstan.autoservify.ui.Adapters.DashboardOrderAdapter
import com.hstan.autoservify.ui.Adapters.DashboardAppointmentAdapter
import com.hstan.autoservify.ui.main.ViewModels.Order
import com.hstan.autoservify.ui.main.Shops.Services.Appointment
import com.hstan.autoservify.databinding.FragmentShopkeeperDashboardBinding

class ShopkeeperDashboardFragment : Fragment() {

    lateinit var dashboardOrderAdapter: DashboardOrderAdapter
    lateinit var dashboardAppointmentAdapter: DashboardAppointmentAdapter
    lateinit var binding: FragmentShopkeeperDashboardBinding
    
    private var currentShop: Shop? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentShopkeeperDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize adapters
        dashboardOrderAdapter = DashboardOrderAdapter(emptyList())
        dashboardAppointmentAdapter = DashboardAppointmentAdapter(emptyList())

        // Setup RecyclerViews for orders and appointments
        binding.recentOrdersRecyclerview.adapter = dashboardOrderAdapter
        binding.recentOrdersRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        
        binding.recentAppointmentsRecyclerview.adapter = dashboardAppointmentAdapter
        binding.recentAppointmentsRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // Setup click listeners
        setupClickListeners()

        // Load shopkeeper data
        loadShopkeeperData()
    }

    private fun setupClickListeners() {
        binding.viewAllOrdersText.setOnClickListener {
            val intent = Intent(requireContext(), com.hstan.autoservify.ui.shopkeeper.OrdersActivity::class.java)
            startActivity(intent)
        }

        binding.viewAllAppointmentsText.setOnClickListener {
            val intent = Intent(requireContext(), com.hstan.autoservify.ui.shopkeeper.AppointmentsActivity::class.java)
            startActivity(intent)
        }

        binding.editShopButton.setOnClickListener {
            currentShop?.let { shop ->
                val intent = Intent(requireContext(), AddShopActivity::class.java)
                intent.putExtra("shopData", com.google.gson.Gson().toJson(shop))
                startActivity(intent)
            }
        }
    }

    private fun loadShopkeeperData() {
        lifecycleScope.launch {
            val authRepository = AuthRepository()
            val currentUser = authRepository.getCurrentUser()
            
            if (currentUser != null) {
                val result = authRepository.getUserProfile(currentUser.uid)
                if (result.isSuccess) {
                    val userProfile = result.getOrThrow()
                    // Update shopkeeper name
                    updateShopkeeperName(userProfile.name ?: userProfile.email ?: "Shopkeeper")
                    
                    if (userProfile.userType == "shop_owner") {
                        userProfile.shopId?.let { shopId ->
                            loadShopData(shopId)
                            loadDashboardStats(shopId)
                            loadRecentOrders(shopId)
                            loadRecentAppointments(shopId)
                        }
                    }
                }
            }
        }
    }

    private fun loadShopData(shopId: String) {
        lifecycleScope.launch {
            val shopRepository = ShopRepository()
            shopRepository.getShops().collect { shops ->
                val userShop = shops.find { it.id == shopId }
                userShop?.let { shop ->
                    currentShop = shop
                    updateShopInfo(shop)
                }
            }
        }
    }

    private fun updateShopInfo(shop: Shop) {
        binding.shopNameText.text = shop.title
        binding.shopLocationText.text = shop.address
        // You can load shop image here if needed
        // Glide.with(this).load(shop.imageUrl).into(binding.shopImage)
    }
    
    private fun updateShopkeeperName(userName: String) {
        binding.shopkeeperNameText.text = userName.ifEmpty { "Shopkeeper" }
    }

    private fun loadDashboardStats(shopId: String) {
        lifecycleScope.launch {
            try {
                val orderRepository = OrderRepository()
                val appointmentRepository = AppointmentRepository()
                
                // Get today's date for filtering
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date())
                
                // Load orders and calculate statistics
                orderRepository.getShopOrders(shopId).collect { orders ->
                    // Today's sales (orders placed today)
                    val todaySales = orders.count { order ->
                        order.orderDate.contains(today)
                    }
                    
                    // Orders pending (orders with "pending" or "Order Placed" status)
                    val pendingOrders = orders.count { order ->
                        order.status.contains("pending", ignoreCase = true) || 
                        order.status.contains("Order Placed", ignoreCase = true)
                    }
                    
                    // Update UI
                    binding.salesCount.text = todaySales.toString()
                    binding.ordersPendingCount.text = pendingOrders.toString()
                }
                
                // Load appointments and calculate statistics
                appointmentRepository.getShopAppointments(shopId).collect { appointments ->
                    // Total appointments for this shop
                    val totalAppointments = appointments.size
                    
                    // Debug logging
                    println("ShopkeeperDashboard: Total appointments for shop $shopId: $totalAppointments")
                    appointments.forEach { appointment ->
                        println("ShopkeeperDashboard: Appointment - Service: ${appointment.serviceName}, Status: ${appointment.status}, ShopId: ${appointment.shopId}")
                    }
                    
                    // Update UI with total appointments
                    binding.appointmentsPendingCount.text = totalAppointments.toString()
                }
                
                // For now, set a default review score (you can implement actual review system later)
                binding.averageReview.text = "4.8"
                
            } catch (e: Exception) {
                // Handle error, set default values
                binding.salesCount.text = "0"
                binding.ordersPendingCount.text = "0"
                binding.appointmentsPendingCount.text = "0"
                binding.averageReview.text = "4.8"
                println("Error loading dashboard stats: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun loadRecentOrders(shopId: String) {
        lifecycleScope.launch {
            val orderRepository = OrderRepository()
            orderRepository.getShopOrders(shopId).collect { orders ->
                val recentOrders = orders.take(3)
                dashboardOrderAdapter.updateData(recentOrders)
            }
        }
    }

    private fun loadRecentAppointments(shopId: String) {
        lifecycleScope.launch {
            val appointmentRepository = AppointmentRepository()
            appointmentRepository.getRecentShopAppointments(shopId, 3).collect { appointments ->
                dashboardAppointmentAdapter.updateData(appointments)
            }
        }
    }
}
