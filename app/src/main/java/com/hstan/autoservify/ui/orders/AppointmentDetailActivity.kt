package com.hstan.autoservify.ui.orders

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.hstan.autoservify.databinding.ActivityAppointmentDetailBinding
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.model.repositories.OrderRepository
import com.hstan.autoservify.model.repositories.ServiceRepository
import com.hstan.autoservify.ui.main.Shops.Services.Appointment
import com.hstan.autoservify.ui.main.ViewModels.Order
import kotlinx.coroutines.launch

class AppointmentDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppointmentDetailBinding
    private lateinit var appointment: Appointment
    private val orderRepository = OrderRepository()
    private val authRepository = AuthRepository()
    private val serviceRepository = ServiceRepository()
    private lateinit var sparePartsAdapter: AppointmentSparePartsAdapter
    private val spareParts = mutableListOf<Order>()
    private var servicePrice: Double = 0.0

    companion object {
        const val EXTRA_APPOINTMENT = "extra_appointment"
        const val REQUEST_ADD_PARTS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppointmentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set title and back button
        title = "Appointment Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get appointment from intent
        val appointmentJson = intent.getStringExtra(EXTRA_APPOINTMENT)
        if (appointmentJson == null) {
            Toast.makeText(this, "Error: No appointment data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        appointment = Gson().fromJson(appointmentJson, Appointment::class.java)

        setupUI()
        setupRecyclerView()
        loadServicePrice()
        loadSpareParts()
        checkUserType()

        binding.createOrderButton.setOnClickListener {
            openAddPartsActivity()
        }
    }

    private fun setupUI() {
        binding.apply {
            serviceNameText.text = appointment.serviceName
            appointmentDateText.text = appointment.appointmentDate
            appointmentTimeText.text = appointment.appointmentTime
            customerNameText.text = appointment.userName
            
            // Set status with color
            statusText.text = appointment.status
            when (appointment.status.lowercase()) {
                "pending" -> {
                    statusText.setBackgroundColor(Color.parseColor("#FF9800"))
                    statusText.setTextColor(Color.WHITE)
                }
                "confirmed" -> {
                    statusText.setBackgroundColor(Color.parseColor("#4CAF50"))
                    statusText.setTextColor(Color.WHITE)
                }
                "completed" -> {
                    statusText.setBackgroundColor(Color.parseColor("#2196F3"))
                    statusText.setTextColor(Color.WHITE)
                }
                else -> {
                    statusText.setBackgroundColor(Color.parseColor("#9E9E9E"))
                    statusText.setTextColor(Color.WHITE)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        sparePartsAdapter = AppointmentSparePartsAdapter(spareParts)
        binding.sparePartsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AppointmentDetailActivity)
            adapter = sparePartsAdapter
        }
    }

    private fun loadServicePrice() {
        lifecycleScope.launch {
            try {
                // Try to get price from appointment.bill first
                if (appointment.bill.isNotEmpty()) {
                    servicePrice = appointment.bill.toDoubleOrNull() ?: 0.0
                    updatePrices()
                } else {
                    // Otherwise fetch from service
                    val service = serviceRepository.getServiceById(appointment.serviceId)
                    if (service != null) {
                        servicePrice = service.price
                    } else {
                        // Default to 0 if service not found
                        servicePrice = 0.0
                    }
                    updatePrices()
                }
            } catch (e: Exception) {
                println("Error loading service price: ${e.message}")
                servicePrice = 0.0
                updatePrices()
            }
        }
    }

    private fun loadSpareParts() {
        lifecycleScope.launch {
            try {
                // Use appointment.id as the bookingId
                val bookingId = appointment.id.ifEmpty { appointment.appointmentId }
                orderRepository.getOrdersByBookingId(bookingId).collect { orders ->
                    spareParts.clear()
                    spareParts.addAll(orders)
                    sparePartsAdapter.notifyDataSetChanged()
                    
                    // Show/hide empty state
                    if (orders.isEmpty()) {
                        binding.sparePartsRecyclerView.visibility = View.GONE
                        binding.noPartsText.visibility = View.VISIBLE
                    } else {
                        binding.sparePartsRecyclerView.visibility = View.VISIBLE
                        binding.noPartsText.visibility = View.GONE
                    }
                    
                    updatePrices()
                }
            } catch (e: Exception) {
                println("Error loading spare parts: ${e.message}")
                Toast.makeText(this@AppointmentDetailActivity, "Error loading spare parts", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePrices() {
        val sparePartsTotal = spareParts.sumOf { 
            (it.item?.price ?: 0) * it.quantity.toDouble()
        }
        val totalAmount = servicePrice + sparePartsTotal

        binding.apply {
            serviceAmountText.text = "Rs. ${String.format("%.2f", servicePrice)}"
            sparePartsAmountText.text = "Rs. ${String.format("%.2f", sparePartsTotal)}"
            totalAmountText.text = "Rs. ${String.format("%.2f", totalAmount)}"
        }
    }

    private fun checkUserType() {
        lifecycleScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    val result = authRepository.getUserProfile(currentUser.uid)
                    result.onSuccess { userProfile ->
                        if (userProfile.userType == "shop_owner") {
                            binding.createOrderButton.visibility = View.VISIBLE
                        } else {
                            binding.createOrderButton.visibility = View.GONE
                        }
                    }.onFailure {
                        binding.createOrderButton.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                println("Error checking user type: ${e.message}")
                binding.createOrderButton.visibility = View.GONE
            }
        }
    }

    private fun openAddPartsActivity() {
        val intent = Intent(this, AddAppointmentPartsActivity::class.java)
        intent.putExtra("appointment", Gson().toJson(appointment))
        startActivityForResult(intent, REQUEST_ADD_PARTS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_PARTS && resultCode == RESULT_OK) {
            // Refresh the spare parts list
            loadSpareParts()
            Toast.makeText(this, "Spare parts added successfully", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

