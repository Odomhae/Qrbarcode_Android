package com.odom.barcodeqr.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.journeyapps.barcodescanner.BarcodeCallback
import com.odom.barcodeqr.R
import com.odom.barcodeqr.databinding.FragmentScanBinding

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    val PERMISSIONS: Array<String> = arrayOf(Manifest.permission.CAMERA)
    private var torchOn = false
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA


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

        AlertDialog.Builder(requireContext())
            .setTitle(result.result.toString())
            .setMessage(result.text.toString() + " / " + result.barcodeFormat)
            .setPositiveButton("저장") { dialog, _ ->
                // 확인 버튼 클릭 시 실행할 코드
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                // 취소 버튼 클릭 시 실행할 코드
                dialog.dismiss()
                binding.zxingBarcodeScanner.resume()
            }

            .show()

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