package com.hstan.autoservify.ui.orders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hstan.autoservify.R
import com.hstan.autoservify.databinding.ItemOrderBinding
import com.hstan.autoservify.ui.Order
import com.hstan.autoservify.ui.main.Shops.Services.Appointment

class OrderAdapter(
    private var items: List<Any>, // can be Order or Appointment
    private val onViewClick: ((Any) -> Unit)? = null,
    private val onCancelClick: ((Any) -> Unit)? = null
) : RecyclerView.Adapter<OrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        when (val item = items[position]) {
            is Order -> bindOrder(holder, item)
            is Appointment -> bindAppointment(holder, item)
        }
    }

    private fun bindOrder(holder: OrderViewHolder, order: Order) {
        holder.binding.orderItemTitle.text = order.item?.title ?: "Item"
        holder.binding.orderQty.text = "Qty: ${order.quantity}"
        holder.binding.orderPrice.text = "Rs. ${order.item?.price ?: 0}"
        holder.binding.orderStatus.text = order.status.ifBlank { "pending" }
        holder.binding.orderDate.text = order.orderDate

        Glide.with(holder.itemView.context)
            .load(order.item?.image)
            .placeholder(R.drawable.logo)
            .error(R.drawable.logo)
            .into(holder.binding.orderItemImage)

        holder.binding.orderView.setOnClickListener { onViewClick?.invoke(order) }
        holder.binding.orderCancel.setOnClickListener { onCancelClick?.invoke(order) }
        holder.itemView.setOnClickListener { onViewClick?.invoke(order) }
    }

    private fun bindAppointment(holder: OrderViewHolder, appointment: Appointment) {
        holder.binding.orderItemTitle.text = appointment.userName.ifBlank { "Customer" }
        holder.binding.orderQty.text = "Email: ${appointment.userEmail}"
        holder.binding.orderPrice.text = "Bill: Rs. ${appointment.bill.ifBlank { "0" }}"
        holder.binding.orderStatus.text = appointment.status.ifBlank { "Pending" }
        holder.binding.orderDate.text =
            "${appointment.appointmentDate} ${appointment.appointmentTime}"

        Glide.with(holder.itemView.context)
            .load(R.drawable.logo) // Appointment has no image field
            .placeholder(R.drawable.logo)
            .into(holder.binding.orderItemImage)

        holder.binding.orderView.setOnClickListener { onViewClick?.invoke(appointment) }
        holder.binding.orderCancel.setOnClickListener { onCancelClick?.invoke(appointment) }
        holder.itemView.setOnClickListener { onViewClick?.invoke(appointment) }
    }

    fun updateData(newItems: List<Any>) {
        items = newItems
        notifyDataSetChanged()
    }
}
