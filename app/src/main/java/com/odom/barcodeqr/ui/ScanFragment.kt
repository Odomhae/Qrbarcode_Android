package com.odom.barcodeqr.ui

import android.Manifest
import android.R.id.input
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.journeyapps.barcodescanner.BarcodeCallback
import com.odom.barcodeqr.R
import com.odom.barcodeqr.databinding.FragmentScanBinding
import com.odom.barcodeqr.history.HistoryViewModel
import com.odom.barcodeqr.utils.AdManager

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    val PERMISSIONS: Array<String> = arrayOf(Manifest.permission.CAMERA)
    private var torchOn = false
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private lateinit var viewModel: HistoryViewModel
    private lateinit var adManager: AdManager

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
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        val root: View = binding.root

        checkPermission()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adManager = AdManager(requireContext())

        binding.btFlash.setOnClickListener {
            toggleFlash()
            binding.btFlash.background =
                if (torchOn) resources.getDrawable(R.drawable.ic_flash) else resources.getDrawable(R.drawable.ic_flash_off)
        }

        binding.btCameraSwitch.setOnClickListener {
            binding.zxingBarcodeScanner.pause() // 기존 카메라 정지

            // 전면 <-> 후면 카메라 토글
            if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                binding.zxingBarcodeScanner.cameraSettings.requestedCameraId = 1 // Set to front camera
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                binding.btFlash.background = resources.getDrawable(R.drawable.ic_flash_off)
            } else {
                binding.zxingBarcodeScanner.cameraSettings.requestedCameraId = 0
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            }

            binding.zxingBarcodeScanner.resume() // 새 카메라 시작
        }

    }

    fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            binding.zxingBarcodeScanner.decodeContinuous(barcodeCallback)
        } else {
            ActivityCompat.requestPermissions(requireActivity(), PERMISSIONS, 200)
        }
    }


    private fun toggleFlash() {
        torchOn = !torchOn
        binding.zxingBarcodeScanner.setTorch(torchOn)
    }

    val barcodeCallback : BarcodeCallback = BarcodeCallback { result ->
        binding.zxingBarcodeScanner.pause()


        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_scan_result, null)

        // 2. AlertDialog 생성
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false) // 밖을 눌러도 안 꺼지게 설정 (선택사항)
            .create()

        // 3. 커스텀 뷰 내의 위젯 참조 및 데이터 설정
        val tvContent = dialogView.findViewById<TextView>(R.id.tv_dialog_content)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_dialog_save)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_dialog_cancel)

        tvContent.text = result.text ?: ""
        val isUrl = android.util.Patterns.WEB_URL.matcher(result.text).matches()
        if (isUrl) {
            // URL인 경우 스타일 변경 (파란색, 밑줄)
            tvContent.setTextColor(android.graphics.Color.BLUE)
            tvContent.paintFlags = tvContent.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG

        }

        tvContent.setOnClickListener {
            viewModel.addHistory(result.text)

            if (isUrl) {
                try {
                    // URL이 http:// 또는 https://로 시작하지 않으면 붙여줌
                    val url = if (!tvContent.text.startsWith("http://") && !tvContent.text.startsWith("https://")) {
                        "http://$tvContent.text"
                    } else {
                        tvContent.text
                    }
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(
                        url as String?
                    ))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), getString(R.string.error_invalid_url), Toast.LENGTH_SHORT).show()
                }

            }

        }

        // 4. 버튼 클릭 리스너 설정
        btnSave.setOnClickListener {
            viewModel.addHistory(result.text)
            dialog.dismiss()
            binding.zxingBarcodeScanner.resume()
            
            // Check if we should show interstitial ad
            if (adManager.incrementScanCount()) {
                adManager.showInterstitialAd(requireActivity()) {
                    binding.zxingBarcodeScanner.resume()
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            binding.zxingBarcodeScanner.resume()
        }

        // 5. 다이얼로그 표시
        dialog.show()

    }

    override fun onResume() {
        super.onResume()
        binding.zxingBarcodeScanner.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.zxingBarcodeScanner.pause()
        binding.zxingBarcodeScanner.setTorch(false)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}