package com.hstan.autoservify.ui.main.home

import android.os.Bundle
import android.content.Intent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager

import com.hstan.autoservify.R
import com.hstan.autoservify.databinding.FragmentHomeBinding
import com.hstan.autoservify.ui.main.Shops.AddShopActivity
import com.hstan.autoservify.ui.main.Shops.Shop
import com.hstan.autoservify.ui.main.Shops.Services.ServicesActivity
import com.hstan.autoservify.ui.main.Shops.SpareParts.PartsCraftActivity
import kotlinx.coroutines.launch
import com.hstan.autoservify.ui.Adapters.ShopAdapter
import com.hstan.autoservify.ui.Adapters.DashboardServiceAdapter
import com.hstan.autoservify.ui.Adapters.DashboardSparePartAdapter
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.model.repositories.ShopRepository
import com.hstan.autoservify.model.repositories.ServiceRepository
import com.hstan.autoservify.model.repositories.PartsCraftRepository



class HomeFragment : Fragment() {

    lateinit var adapter: ShopAdapter
    lateinit var serviceAdapter: DashboardServiceAdapter
    lateinit var sparePartAdapter: DashboardSparePartAdapter
    val items = ArrayList<Shop>()
    lateinit var binding: FragmentHomeBinding
    lateinit var viewModel: HomeFragmentViewModel
    
    private var currentUserType: String = "customer"
    private var currentShop: Shop? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = HomeFragmentViewModel()

        // Initialize adapters
        adapter = ShopAdapter(items)
        serviceAdapter = DashboardServiceAdapter(emptyList())
        sparePartAdapter = DashboardSparePartAdapter(emptyList())

        // Setup RecyclerViews
        binding.recyclerview.adapter = adapter
        binding.recyclerview.layoutManager = LinearLayoutManager(context)
        
        binding.servicesRecyclerView.adapter = serviceAdapter
        binding.servicesRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        
        binding.sparePartsRecyclerView.adapter = sparePartAdapter
        binding.sparePartsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // Check user type and setup appropriate view
        checkUserTypeAndSetupView()

        // Setup click listeners
        setupClickListeners()

        // Observe data changes
        observeDataChanges()
    }

    private fun checkUserTypeAndSetupView() {
        lifecycleScope.launch {
            val authRepository = AuthRepository()
            val currentUser = authRepository.getCurrentUser()
            
            if (currentUser != null) {
                val result = authRepository.getUserProfile(currentUser.uid)
                if (result.isSuccess) {
                    val userProfile = result.getOrThrow()
                    currentUserType = userProfile.userType
                    
                    when (userProfile.userType) {
                        "shop_owner" -> {
                            showShopkeeperView()
                            loadShopkeeperData(userProfile)
                        }
                        else -> {
                            showCustomerView()
                        }
                    }
                } else {
                    // If no profile found, default to customer
                    showCustomerView()
                }
            } else {
                // If not logged in, show customer view
                showCustomerView()
            }
        }
    }

    private fun showCustomerView() {
        binding.customerView.visibility = View.VISIBLE
        binding.shopkeeperView.visibility = View.GONE
        
        // Show/hide FAB based on user type
        if (currentUserType == "shop_owner") {
            binding.fabAddShop.visibility = View.VISIBLE
        } else {
            binding.fabAddShop.visibility = View.GONE
        }
    }

    private fun showShopkeeperView() {
        binding.customerView.visibility = View.GONE
        binding.shopkeeperView.visibility = View.VISIBLE
    }

    private fun loadShopkeeperData(userProfile: com.hstan.autoservify.model.AppUser) {
        userProfile.shopId?.let { shopId ->
            lifecycleScope.launch {
                // Load shop details
                val shopRepository = ShopRepository()
                shopRepository.getShops().collect { shops ->
                    val userShop = shops.find { it.id == shopId }
                    userShop?.let { shop ->
                        currentShop = shop
                        updateShopInfo(shop)
                        loadDashboardStats(shopId)
                        loadRecentServices(shopId)
                        loadRecentSpareParts(shopId)
                    }
                }
            }
        }
    }

    private fun updateShopInfo(shop: Shop) {
        binding.shopNameText.text = shop.title
        binding.shopLocationText.text = shop.address
        // You can load shop image here if needed
    }

    private fun loadDashboardStats(shopId: String) {
        // TODO: Implement actual stats loading
        // For now, using placeholder data
        binding.salesCountText.text = "75k"
        binding.ordersCountText.text = "7"
        binding.reviewRatingText.text = "4.5★"
    }

    private fun loadRecentServices(shopId: String) {
        lifecycleScope.launch {
            try {
                val serviceRepository = ServiceRepository()
                val services = serviceRepository.getServicesByShopId(shopId)
                // Show latest 3 services
                val recentServices = services.take(3)
                serviceAdapter.updateData(recentServices)
            } catch (e: Exception) {
                serviceAdapter.updateData(emptyList())
            }
        }
    }

    private fun loadRecentSpareParts(shopId: String) {
        lifecycleScope.launch {
            try {
                val partsCraftRepository = PartsCraftRepository()
                partsCraftRepository.getPartsCraftsByShopId(shopId).collect { parts ->
                    // Show latest 3 spare parts
                    val recentParts = parts.take(3)
                    sparePartAdapter.updateData(recentParts)
                }
            } catch (e: Exception) {
                sparePartAdapter.updateData(emptyList())
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddShop.setOnClickListener {
            startActivity(Intent(requireContext(), AddShopActivity::class.java))
        }

        binding.editShopButton.setOnClickListener {
            currentShop?.let { shop ->
                val intent = Intent(requireContext(), AddShopActivity::class.java)
                // You can pass shop data for editing
                startActivity(intent)
            }
        }

        binding.viewAllServicesText.setOnClickListener {
            val intent = Intent(requireContext(), ServicesActivity::class.java)
            intent.putExtra("shopOwnerMode", true)
            startActivity(intent)
        }

        binding.viewAllPartsText.setOnClickListener {
            val intent = Intent(requireContext(), PartsCraftActivity::class.java)
            intent.putExtra("shopOwnerMode", true)
            startActivity(intent)
        }
    }

    private fun observeDataChanges() {
        lifecycleScope.launch {
            viewModel.failureMessage.collect {
                it?.let {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.data.collect {
                it?.let {
                    items.clear()
                    items.addAll(it)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }
}