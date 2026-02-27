package com.odom.barcodeqr.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.odom.barcodeqr.R
import com.odom.barcodeqr.history.model.HistoryItem
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HistoryModernAdapter(
    private val context: Context,
    private var historyList: List<HistoryItem>,
    private val onItemClick: (HistoryItem) -> Unit,
    private val onCopyClick: (HistoryItem) -> Unit,
    private val onRegenerateClick: (HistoryItem) -> Unit,
    private val onDeleteClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryModernAdapter.ViewHolder>() {

    private var filteredList: List<HistoryItem> = historyList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.history_item_modern, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val historyItem = filteredList[position]
        holder.bind(historyItem)
    }

    override fun getItemCount(): Int = filteredList.size

    fun updateList(newList: List<HistoryItem>) {
        historyList = newList
        filteredList = newList
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredList = if (query.isEmpty()) {
            historyList
        } else {
            historyList.filter { 
                it.qrString.contains(query, ignoreCase = true) 
            }
        }
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val ivQRPreview: ImageView = itemView.findViewById(R.id.ivQRPreview)
        private val btnCopy: ImageView = itemView.findViewById(R.id.btnCopy)
        private val btnRegenerate: ImageView = itemView.findViewById(R.id.btnRegenerate)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)

        fun bind(historyItem: HistoryItem) {
            // Set content
            tvContent.text = historyItem.qrString
            tvTimestamp.text = getRelativeTime(historyItem.createdAt)

            // Set click listeners
            itemView.setOnClickListener { onItemClick(historyItem) }
            
            btnCopy.setOnClickListener { 
                copyToClipboard(historyItem.qrString)
                onCopyClick(historyItem)
            }
            
            btnRegenerate.setOnClickListener { onRegenerateClick(historyItem) }
            btnDelete.setOnClickListener { onDeleteClick(historyItem) }

            // Long press to copy (keeping existing functionality)
            itemView.setOnLongClickListener {
                copyToClipboard(historyItem.qrString)
                true
            }
        }

        private fun copyToClipboard(text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("QR Code", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "클립보드에 복사되었습니다", Toast.LENGTH_SHORT).show()
        }

        private fun getRelativeTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            val returnTime = when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "방금 전"
                diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}분 전"
                diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}시간 전"
                diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}일 전"
                else -> {
                    val dateFormat = SimpleDateFormat("MM월 dd일", Locale.getDefault())
                    dateFormat.format(Date(timestamp))
                }
            }

            return returnTime
        }
    }
}
