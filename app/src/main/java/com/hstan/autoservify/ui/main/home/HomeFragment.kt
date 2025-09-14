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
import kotlinx.coroutines.launch
import com.hstan.autoservify.ui.Adapters.ShopAdapter
import com.hstan.autoservify.model.repositories.AuthRepository



class HomeFragment : Fragment() {

    lateinit var adapter: ShopAdapter
    val items=ArrayList< Shop>()
    lateinit var binding: FragmentHomeBinding
    lateinit var viewModel: HomeFragmentViewModel

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

        adapter= ShopAdapter(items)
        binding.recyclerview.adapter=adapter
        binding.recyclerview.layoutManager= LinearLayoutManager(context)

        // Check user type and show/hide add shop button
        checkUserTypeAndShowButton()

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

        binding.fabAddShop.setOnClickListener {
            startActivity(Intent(requireContext(), AddShopActivity::class.java))
        }
    }

    private fun checkUserTypeAndShowButton() {
        lifecycleScope.launch {
            val authRepository = AuthRepository()
            val currentUser = authRepository.getCurrentUser()
            
            if (currentUser != null) {
                val result = authRepository.getUserProfile(currentUser.uid)
                if (result.isSuccess) {
                    val userProfile = result.getOrThrow()
                    if (userProfile.userType == "shop_owner") {
                        binding.fabAddShop.visibility = View.VISIBLE
                    } else {
                        binding.fabAddShop.visibility = View.GONE
                    }
                } else {
                    // If no profile found, default to customer (hide button)
                    binding.fabAddShop.visibility = View.GONE
                }
            } else {
                // If not logged in, hide button
                binding.fabAddShop.visibility = View.GONE
            }
        }
    }
}