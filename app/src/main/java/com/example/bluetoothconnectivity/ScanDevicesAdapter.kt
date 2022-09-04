package com.example.bluetoothconnectivity

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothconnectivity.databinding.ItemScanDeviceBinding

class ScanDevicesAdapter(val listener: RecyclerViewItemOnClickListener) : RecyclerView.Adapter<ScanDevicesAdapter.ScanResultVh>() {
    companion object{
        private const val TAG: String = "ScanDevicesAdapter"
    }
    private var onItemClick: ((ScanResult) -> Unit)? = null
    private var itemsList: MutableList<ScanResult> = arrayListOf()

    @SuppressLint("MissingPermission")
    fun setItems(mutableList: MutableList<ScanResult>) {
        if (mutableList != itemsList) {
            mutableList.forEach{ data ->
                if (data.device.name != null) itemsList.add(data)
                //mutableList.add()
            }
            //itemsList = mutableList
            notifyDataSetChanged()
        }
    }

    @SuppressLint("MissingPermission")
    fun addSingleItem(item: ScanResult) {
        /**
         * In this particular case the data coming in may be duplicate. So check that only unique
         * elements are admitted: the device Id + device name should create a unique pair.
         * removeIf requires API level 24, so using removeAll here. But feel free to use any of
         * a number of options. Remove the previous element so to keep the latest timestamp
         */
        itemsList.removeAll {
            it.device.name == item.device.name && it.device.address == item.device.address
        }
        if (item.device.name != null) itemsList.add(item)
        notifyDataSetChanged()
    }

    private fun getItem(position: Int): ScanResult? = if (itemsList.isEmpty()) null else itemsList[position]

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ScanDevicesAdapter.ScanResultVh {
        val binding = ItemScanDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScanResultVh(binding/*,onClickListener*/)
    }

    override fun onBindViewHolder(holder: ScanDevicesAdapter.ScanResultVh, position: Int) {
        Log.e(TAG, "onBindViewHolder: called for position $position")
        holder.bind(getItem(position))
    }

    override fun getItemCount() = itemsList.size

    inner class ScanResultVh(private val binding: ItemScanDeviceBinding/*, private val onClickListener: ((device: ScanResult) -> Unit)*/) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("MissingPermission")
        fun bind(item: ScanResult?) {
            item?.let {
                binding.deviceName.text = it.device.name
                binding.deviceAddress.text = it.device.address
                //binding.lastSeen.text = it.timestampNanos.toString()
                binding.root.setOnClickListener {
                    listener.onViewClick(item)
                   // onItemClick?.invoke(itemsList[adapterPosition])
                }
            }

        }
    }

    interface RecyclerViewItemOnClickListener {
        fun onViewClick(position: ScanResult)
    }

}