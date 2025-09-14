package com.hstan.autoservify.ui.main.Shops.Services

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.hstan.autoservify.R

class Add_Service_Activity : AppCompatActivity() {

    private val viewModel: ServiceViewModel by viewModels()
    private var isEditMode = false
    private var serviceToEdit: Service? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_service)

        val name = findViewById<EditText>(R.id.name)
        val desc = findViewById<EditText>(R.id.descript)
        val price = findViewById<EditText>(R.id.price1)
        val addBtn = findViewById<Button>(R.id.AddService_button)
        val titleText = findViewById<TextView>(R.id.textView6)

        // Check if we're in edit mode
        val serviceJson = intent.getStringExtra("service_data")
        if (serviceJson != null) {
            isEditMode = true
            serviceToEdit = Gson().fromJson(serviceJson, Service::class.java)
            
            // Update UI for edit mode
            titleText.text = "Edit Service"
            addBtn.text = "Update Service"
            
            // Pre-fill the form
            serviceToEdit?.let { service ->
                name.setText(service.name)
                desc.setText(service.description)
                price.setText(service.price.toString())
            }
        }

        addBtn.setOnClickListener {
            if (name.text.isNullOrBlank() || desc.text.isNullOrBlank() || price.text.isNullOrBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isEditMode && serviceToEdit != null) {
                // Update existing service
                val updatedService = serviceToEdit!!.copy(
                    name = name.text.toString(),
                    description = desc.text.toString(),
                    price = price.text.toString().toDoubleOrNull() ?: 0.0
                )
                viewModel.updateService(updatedService)
            } else {
                // Create new service
                val service = Service(
                    id = FirebaseFirestore.getInstance().collection("services").document().id,
                    name = name.text.toString(),
                    description = desc.text.toString(),
                    price = price.text.toString().toDoubleOrNull() ?: 0.0,
                    rating = 0.0
                )
                viewModel.addService(service)
            }
        }

        // ✅ Observe save result
        viewModel.isSuccessfullySaved.observe(this) { success ->
            if (success == true) {
                Toast.makeText(this, "Service Added", Toast.LENGTH_SHORT).show()
                finish()
            } else if (success == false) {
                Toast.makeText(this, "Failed to add service", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ Observe update result
        viewModel.isSuccessfullyUpdated.observe(this) { success ->
            if (success == true) {
                Toast.makeText(this, "Service Updated Successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else if (success == false) {
                Toast.makeText(this, "Failed to update service", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ Observe errors
        viewModel.failureMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, "Error: $it", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
