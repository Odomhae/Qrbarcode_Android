package com.odom.barcodeqr.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.DataBindingUtil
import com.odom.barcodeqr.R
import com.odom.barcodeqr.databinding.HistoryItemBinding
import com.odom.barcodeqr.history.model.HistoryItem

class HistoryAdapter(
    val mContext: Context, val list: List<HistoryItem>) : BaseAdapter() {

    override fun getCount(): Int {
        return list.size
    }

    override fun getItem(p0: Int): Any? {
        return list[p0]
    }

    override fun getItemId(p0: Int): Long {
        return list[p0].id
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {

        val binding: HistoryItemBinding
        val holder: ViewHolder

        if (view == null) {
            binding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.history_item, parent, false)
            holder = ViewHolder(binding)
            binding.root.tag = holder
        } else {
            holder = view.tag as ViewHolder
            binding = holder.binding
        }

        val deviceItem : HistoryItem = list[position]
        holder.bind(mContext, deviceItem)

        return binding.root
    }

    private class ViewHolder(val binding: HistoryItemBinding) {
        fun bind(context: Context, historyItem: HistoryItem) {
          //  binding.tvId.text = historyItem.id.toString()
            binding.tvString.text = historyItem.qrString

            binding.tvString.setOnLongClickListener {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("label", binding.tvString.text)
                clipboard.setPrimaryClip(clip)

            //    Toast.makeText(context, "복사되었습니다!", Toast.LENGTH_SHORT).show()
                true // 이벤트 소비
            }
        }

    }

}