package com.odom.barcodeqr.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.odom.barcodeqr.R
import com.odom.barcodeqr.adapter.HistoryModernAdapter
import com.odom.barcodeqr.databinding.FragmentHistoryBinding
import com.odom.barcodeqr.history.HistoryViewModel
import com.odom.barcodeqr.history.model.HistoryItem

class HistoryFragment : Fragment() {

    private lateinit var viewModel: HistoryViewModel
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var modernAdapter: HistoryModernAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[HistoryViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearch()
        setupClearButton()
        
        observeHistory()
    }

    private fun setupRecyclerView() {
        modernAdapter = HistoryModernAdapter(
            context = requireContext(),
            historyList = emptyList(),
            onItemClick = { historyItem ->
                // Handle item click - could navigate to generate with this content
                //navigateToGenerate(historyItem.qrString)
            },
            onCopyClick = { historyItem ->
                // Copy is handled in adapter
            },
            onRegenerateClick = { historyItem ->
                navigateToGenerate(historyItem.qrString)
            },
            onDeleteClick = { historyItem ->
                deleteHistoryItem(historyItem)
            }
        )

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = modernAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                modernAdapter.filter(query)
                
                // Show/hide clear button
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text.clear()
        }
    }

    private fun setupClearButton() {
        binding.btnClearHistory.setOnClickListener {
            clearAllHistory()
        }
    }

    private fun observeHistory() {
        lifecycleScope.launchWhenStarted {
            viewModel.listHistory.collect { list ->
                updateUI(list)
            }
        }
    }

    private fun updateUI(historyList: List<HistoryItem>) {
        if (historyList.isEmpty()) {
            showEmptyState()
        } else {
            showHistoryList(historyList)
        }
    }

    private fun showEmptyState() {
        binding.rvHistory.visibility = View.GONE
        binding.llEmptyState.visibility = View.VISIBLE
    }

    private fun showHistoryList(historyList: List<HistoryItem>) {
        binding.rvHistory.visibility = View.VISIBLE
        binding.llEmptyState.visibility = View.GONE
        modernAdapter.updateList(historyList)
    }

    private fun navigateToGenerate(content: String) {
        // Navigate to GenerateFragment with the content
        val bundle = Bundle().apply {
            putString("qr_content", content)
        }
        findNavController().navigate(R.id.navigation_generate, bundle)
    }

    private fun deleteHistoryItem(historyItem: HistoryItem) {
        viewModel.deleteHistory(historyItem)
    }

    private fun clearAllHistory() {
        viewModel.clearAllHistory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}