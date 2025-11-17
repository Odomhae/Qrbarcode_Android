package com.odom.barcodeqr.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.odom.barcodeqr.adapter.HistoryAdapter
import com.odom.barcodeqr.databinding.FragmentHistoryBinding
import com.odom.barcodeqr.history.HistoryViewModel

class HistoryFragment : Fragment() {

  //  private val viewModel: HistoryViewModel by viewModels()

    private lateinit var viewModel: HistoryViewModel


    private var _binding: FragmentHistoryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // AndroidViewModel은 Application을 필요로 하므로 requireActivity().application을 전달
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[HistoryViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenStarted {
            viewModel.listHistory.collect { list ->
                // 여기서 list는 실제 DB에서 가져온 List<HistoryItem>
                Log.d("HistoryFragment", "History size: ${list.size}")
                binding.lvHistory.adapter = HistoryAdapter(requireContext(), list)
            }
        }

        val historyList = viewModel.listHistory
        Log.d("===ttt ", historyList.value.size.toString())
        //binding.lvHistory.adapter = HistoryAdapter(requireContext(), historyList)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}