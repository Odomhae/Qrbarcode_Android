package com.odom.barcodeqr.ui

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.odom.barcodeqr.R
import com.odom.barcodeqr.history.HistoryViewModel
import yuku.ambilwarna.AmbilWarnaDialog

class UserInputFragment : Fragment() {

    private lateinit var viewModel: HistoryViewModel
    private var currentInputType = "text"
    private var generatedBitmap: Bitmap? = null
    var selectedType : String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_input, container, false)
    }
    
    companion object {
        fun newInstance(selectedType2 : String) = UserInputFragment().apply {
            selectedType = selectedType2
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = viewModels<HistoryViewModel>().value

        val buttonGenerate = view.findViewById<Button>(R.id.buttonGenerate)
        val buttonShare = view.findViewById<Button>(R.id.buttonShare)
        val imageView = view.findViewById<ImageView>(R.id.imageView)

        buttonGenerate.setOnClickListener {
            generateQRCode()
        }

        buttonShare.setOnClickListener {
            shareQRCode()
        }
    }

    fun setInputType(type: String) {
        currentInputType = type
        updateInputVisibility()
    }

    private fun updateInputVisibility() {
        val view = view ?: return

        val textInputTitle = view.findViewById<TextView>(R.id.textInputTitle)
        val layoutTextInput = view.findViewById<LinearLayout>(R.id.layoutTextInput)
        val layoutUrlInput = view.findViewById<LinearLayout>(R.id.layoutUrlInput)
        val layoutEmailInput = view.findViewById<LinearLayout>(R.id.layoutEmailInput)
        val layoutContactInput = view.findViewById<LinearLayout>(R.id.layoutContactInput)

        // Hide all layouts first
        layoutTextInput.visibility = View.GONE
        layoutUrlInput.visibility = View.GONE
        layoutEmailInput.visibility = View.GONE
        layoutContactInput.visibility = View.GONE

        // Show relevant layout and update title
        when (currentInputType) {
            "text" -> {
                textInputTitle.text = "텍스트 입력"
                layoutTextInput.visibility = View.VISIBLE
            }
            "url" -> {
                textInputTitle.text = "URL 입력"
                layoutUrlInput.visibility = View.VISIBLE
            }
            "email" -> {
                textInputTitle.text = "이메일 입력"
                layoutEmailInput.visibility = View.VISIBLE
            }
            "contact" -> {
                textInputTitle.text = "연락처 입력"
                layoutContactInput.visibility = View.VISIBLE
            }
        }
    }

    private fun generateQRCode() {
        val view = view ?: return

        val inputText = when (currentInputType) {
            "text" -> view.findViewById<EditText>(R.id.editText).text.toString()
            "url" -> view.findViewById<EditText>(R.id.editUrl).text.toString()
            "email" -> {
                val email = view.findViewById<EditText>(R.id.editEmail).text.toString()
                val subject = view.findViewById<EditText>(R.id.editEmailSubject).text.toString()
                val body = view.findViewById<EditText>(R.id.editEmailBody).text.toString()
                "mailto:$email?subject=$subject&body=$body"
            }
            "contact" -> {
                val name = view.findViewById<EditText>(R.id.editContactName).text.toString()
                val phone = view.findViewById<EditText>(R.id.editContactPhone).text.toString()
                val email = view.findViewById<EditText>(R.id.editContactEmail).text.toString()
                "BEGIN:VCARD\nVERSION:3.0\nFN:$name\nTEL:$phone\nEMAIL:$email\nEND:VCARD"
            }
            else -> ""
        }

        if (inputText.isEmpty()) {
            return
        }

        try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                inputText,
                BarcodeFormat.QR_CODE,
                500,
                500
            )
            val barcodeEncoder = BarcodeEncoder()
            generatedBitmap = barcodeEncoder.createBitmap(bitMatrix)

            val imageView = view.findViewById<ImageView>(R.id.imageView)
            imageView.setImageBitmap(generatedBitmap)

            val buttonShare = view.findViewById<Button>(R.id.buttonShare)
            buttonShare.visibility = View.VISIBLE

            // Save to history
        //   todo jihoon  viewModel.addHistoryItem(inputText, generatedBitmap!!)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareQRCode() {
        val bitmap = generatedBitmap ?: return

        try {
            val uri = saveBitmapToCache(bitmap)
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "QR 코드 공유하기"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        val filename = "qr_code_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri = requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        uri?.let {
            val outputStream = requireContext().contentResolver.openOutputStream(it)
            outputStream?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }

        return uri ?: throw Exception("Failed to save bitmap")
    }
}
