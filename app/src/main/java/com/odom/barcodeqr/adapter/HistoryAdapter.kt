//package com.odom.barcodeqr.adapter
//
//import android.content.Context
//import android.widget.BaseAdapter
//import com.odom.barcodeqr.history.model.HistoryItem
//
//class HistoryAdapter(
//    val mContext: Context,
//    val list: List<HistoryItem>,
//) : BaseAdapter() {
//
//    override fun getCount(): Int {
//        return list.size
//    }
//
//    override fun getItem(p0: Int): Any? {
//        return null
//    }
//
//    override fun getItemId(p0: Int): Long {
//        return 0
//    }
//
//    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
//
//        val binding: DeviceItemBinding
//        val holder: ViewHolder
//
//        if (view == null) {
//            binding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.device_item, parent, false)
//            holder = ViewHolder(binding)
//            binding.root.tag = holder
//        } else {
//            holder = view.tag as ViewHolder
//            binding = holder.binding
//        }
//
//        val deviceItem : DeviceGridItem = list[position]
//        if (deviceItem != null) {
//            holder.bind(deviceItem)
//        }
//
//        return binding.root
//    }
//
//    private class ViewHolder(val binding: DeviceItemBinding) {
//        fun bind(
//            deviceItem: DeviceGridItem) {
//            binding.tvDeviceName.text = deviceItem.deviceName
//            binding.ivDevice.setImageDrawable(deviceItem.deviceImg)
//        }
//
//    }
//
//}