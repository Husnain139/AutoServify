package com.hstan.autoservify.ui.main.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hstan.autoservify.databinding.FragmentOrdersBinding
import com.hstan.autoservify.ui.Order
import com.hstan.autoservify.ui.main.Shops.Services.Appointment
import com.hstan.autoservify.ui.orders.OrderAdapter
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.model.AppUser
import kotlinx.coroutines.launch

class OrdersFragment : Fragment() {

    private lateinit var binding: FragmentOrdersBinding
    private lateinit var viewModel: OrderFragmentViewModel
    private lateinit var adapter: OrderAdapter

    // one list to display both orders + appointments
    private val items = ArrayList<Any>()  // Any = Order OR Appointment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // init adapter (you will need to make OrderAdapter handle both Order & Appointment types)
        adapter = OrderAdapter(
            emptyList(),
            onViewClick = { /* navigate to detail screen if needed */ },
            onCancelClick = { item ->
                when (item) {
                    is Order -> viewModel.cancelOrder(item)
                    is Appointment -> viewModel.cancelAppointment(item)
                }
            }
        )

        binding.recyclerview.layoutManager = LinearLayoutManager(context)
        binding.recyclerview.adapter = adapter

        viewModel = ViewModelProvider(this)[OrderFragmentViewModel::class.java]

        lifecycleScope.launch {
            viewModel.failureMessage.collect { msg ->
                msg?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
            }
        }

        // observe orders
        lifecycleScope.launch {
            viewModel.orders.collect { list ->
                refreshData(list, viewModel.appointments.value)
            }
        }

        // observe appointments
        lifecycleScope.launch {
            viewModel.appointments.collect { list ->
                refreshData(viewModel.orders.value, list)
            }
        }

        // Load orders based on user role
        loadOrdersForUser()
        
        // Fallback: Also try to load all data in case user-specific loading fails
        lifecycleScope.launch {
            kotlinx.coroutines.delay(2000) // Wait 2 seconds then try loading all data
            if (items.isEmpty()) {
                println("No data loaded after 2 seconds, trying fallback")
                viewModel.readOrders()
                viewModel.readAppointments()
            }
        }
    }

    private fun refreshData(orderList: List<Order>?, appointmentList: List<Appointment>?) {
        items.clear()
        orderList?.let { 
            items.addAll(it)
            println("Orders loaded: ${it.size} items")
        }
        appointmentList?.let { 
            items.addAll(it)
            println("Appointments loaded: ${it.size} items")
        }
        println("Total items in adapter: ${items.size}")
        adapter.updateData(items)
    }

    private fun loadOrdersForUser() {
        lifecycleScope.launch {
            val authRepository = AuthRepository()
            val currentUser = authRepository.getCurrentUser()
            
            if (currentUser != null) {
                val result = authRepository.getUserProfile(currentUser.uid)
                if (result.isSuccess) {
                    val userProfile = result.getOrThrow()
                    if (userProfile.userType == "customer") {
                        // Load orders and appointments for this customer
                        viewModel.loadCustomerOrders(userProfile.uid)
                        viewModel.readAppointments() // Load all appointments for now
                    } else if (userProfile.userType == "shop_owner") {
                        // Load orders for this shop owner's shop
                        val shopId = userProfile.shopId
                        if (!shopId.isNullOrEmpty()) {
                            viewModel.loadShopOrders(shopId)
                            viewModel.readAppointments() // Load all appointments for now
                        } else {
                            // Shop owner but no shopId, load all orders/appointments
                            viewModel.readOrders()
                            viewModel.readAppointments()
                        }
                    } else {
                        // Unknown user type, load all
                        viewModel.readOrders()
                        viewModel.readAppointments()
                    }
                } else {
                    // If profile not found, try to load as customer
                    viewModel.loadCustomerOrders(currentUser.uid)
                    viewModel.readAppointments() // Load all appointments for now
                }
            } else {
                // No user logged in, load all data
                println("No user logged in, loading all data")
                viewModel.readOrders()
                viewModel.readAppointments()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible
        loadOrdersForUser()
    }
}
