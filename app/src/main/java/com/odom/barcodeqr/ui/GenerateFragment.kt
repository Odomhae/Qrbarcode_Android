package com.odom.barcodeqr.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.odom.barcodeqr.R
import com.odom.barcodeqr.databinding.FragmentGenerateBinding
import com.odom.barcodeqr.history.HistoryViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import yuku.ambilwarna.AmbilWarnaDialog

class GenerateFragment : Fragment() {

   // private val args: GenerateFragmentArgs by navArgs()
    private lateinit var viewModel: HistoryViewModel

    private var _binding: FragmentGenerateBinding? = null
    private val binding get() = _binding!!

    // Customization variables
    private var foregroundColor = Color.BLACK
    private var backgroundColor = Color.WHITE
    private var borderColor = Color.BLACK
    private var selectedLogoBitmap: Bitmap? = null
    private var addImageToQR = false

    // Activity result launcher for image selection
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = BitmapFactory.decodeStream(requireContext().contentResolver.openInputStream(it))
                selectedLogoBitmap = correctImageOrientation(bitmap, it)
                binding.selectedImagePreview.setImageBitmap(selectedLogoBitmap)
                binding.selectedImagePreview.visibility = View.VISIBLE
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Activity result launcher for camera
    private lateinit var photoUri: Uri
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            try {
                val bitmap = BitmapFactory.decodeStream(requireContext().contentResolver.openInputStream(photoUri))
                selectedLogoBitmap = correctImageOrientation(bitmap, photoUri)
                binding.selectedImagePreview.setImageBitmap(selectedLogoBitmap)
                binding.selectedImagePreview.visibility = View.VISIBLE
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "사진을 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        fun newInstance(selectedType : String) : GenerateFragment {
            val fragment = GenerateFragment()
            val bundle = Bundle()
            bundle.putString("selectedType", selectedType)
            fragment.arguments = bundle

            return fragment
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

      //  val selectedType = args.selectedType
      //  Toast.makeText(requireContext(), selectedType , Toast.LENGTH_SHORT).show()

        // AndroidViewModel은 Application을 필요로 하므로 requireActivity().application을 전달
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
        _binding = FragmentGenerateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCustomizationPanel()

        binding.buttonGenerate.setOnClickListener {
            generateCustomizedQRCode()
        }

        // 공유하기
        binding.buttonShare.setOnClickListener {
            val imageUri = saveBitmapToFile(requireContext(),  binding.imageView.drawable.toBitmap(), "qr_code_example")
            if (imageUri != null) {
                shareImage(requireContext(), imageUri)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
           // findNavController().navigate(R.id.navigation_radio)
        }
    }

    private fun setupCustomizationPanel() {
        // Color pickers setup
        binding.btnPickForegroundColor.setOnClickListener {
            showColorPicker(foregroundColor) { color ->
                foregroundColor = color
                binding.foregroundColorPreview.setBackgroundColor(color)
            }
        }

        binding.btnPickBackgroundColor.setOnClickListener {
            showColorPicker(backgroundColor) { color ->
                backgroundColor = color
                binding.backgroundColorPreview.setBackgroundColor(color)
            }
        }

        binding.btnPickBorderColor.setOnClickListener {
            showColorPicker(borderColor) { color ->
                borderColor = color
                binding.borderColorPreview.setBackgroundColor(color)
            }
        }

        // Image options setup
        binding.checkAddImage.setOnCheckedChangeListener { _, isChecked ->
            addImageToQR = isChecked
            binding.btnSelectImage.isEnabled = isChecked
            binding.btnTakePhoto.isEnabled = isChecked
            if (!isChecked) {
                binding.selectedImagePreview.visibility = View.GONE
                selectedLogoBitmap = null
            }
        }

        binding.btnSelectImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnTakePhoto.setOnClickListener {
            dispatchTakePictureIntent()
        }
    }

    private fun showColorPicker(initialColor: Int, onColorSelected: (Int) -> Unit) {
        val colorPicker = AmbilWarnaDialog(requireContext(), initialColor,
            object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onCancel(dialog: AmbilWarnaDialog) {
                    // 취소 시 아무 동작 없음
                }

                override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                    onColorSelected(color)
                }
            })
        colorPicker.show()
    }

    private fun generateCustomizedQRCode() {
        var input = binding.editText.text.toString()
        val qrBitmap = if (addImageToQR && selectedLogoBitmap != null) {
            generateQRCodeWithCircularLogoAndBorder(input, 512, selectedLogoBitmap!!)
        } else {
            generateColoredQRCodeWithBorder(input, 512, foregroundColor, backgroundColor, borderColor)
        }

        binding.imageView.setImageBitmap(qrBitmap)
        viewModel.addHistory(input.toString())
        binding.buttonShare.visibility = View.VISIBLE
    }

    fun generateQRCode(content: String, size: Int = 512): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size
            )
            val barcodeEncoder = BarcodeEncoder()
            barcodeEncoder.createBitmap(bitMatrix)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateColoredQRCodeWithBorder(content: String, size: Int = 512, fgColor: Int, bgColor: Int, borderColor: Int): Bitmap? {
        return try {
            val bitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size
            )

            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    val color = if (bitMatrix[x, y]) fgColor else bgColor
                    bitmap.setPixel(x, y, color)
                }
            }

            // Add border
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.style = Paint.Style.STROKE
            paint.color = borderColor
            paint.strokeWidth = 4f // Border width
            canvas.drawRect(2f, 2f, (size - 2).toFloat(), (size - 2).toFloat(), paint)

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /*
    val logoBitmap = BitmapFactory.decodeResource(resources, R.drawable.my_logo)
    val qrWithLogo = generateQRCodeWithLogo("https://example.com", 512, logoBitmap)
     */
    fun generateQRCodeWithLogo(content: String, size: Int = 512, logo: Bitmap): Bitmap? {
        return try {
            val bitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size
            )

            val qrBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    val color = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    qrBitmap.setPixel(x, y, color)
                }
            }

            // 로고 크기 조정
            val scaledLogo = Bitmap.createScaledBitmap(logo, size / 5, size / 5, false)

            // QR 코드와 로고 합성
            val canvas = Canvas(qrBitmap)
            val centerX = (qrBitmap.width - scaledLogo.width) / 2
            val centerY = (qrBitmap.height - scaledLogo.height) / 2
            canvas.drawBitmap(scaledLogo, centerX.toFloat(), centerY.toFloat(), null)

            qrBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 로고를 원형으로 자르고 테두리 추가하는 함수
    fun createCircularLogoWithBorder(logo: Bitmap, borderSize: Int = 8, borderColor: Int = Color.WHITE): Bitmap {
        val size = minOf(logo.width, logo.height)
        val outputSize = size + borderSize * 2

        val output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 원형 클리핑
        val path = Path().apply {
            addCircle(
                outputSize / 2f,
                outputSize / 2f,
                size / 2f,
                Path.Direction.CCW
            )
        }

        canvas.save()
        canvas.translate(borderSize.toFloat(), borderSize.toFloat())
        canvas.clipPath(path)
        canvas.drawBitmap(Bitmap.createScaledBitmap(logo, size, size, false), 0f, 0f, paint)
        canvas.restore()

        // 테두리 그리기
        paint.style = Paint.Style.STROKE
        paint.color = borderColor
        paint.strokeWidth = borderSize.toFloat()
        canvas.drawCircle(
            outputSize / 2f,
            outputSize / 2f,
            size / 2f,
            paint
        )

        return output
    }

    // Drawable을 Bitmap으로 변환
    fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId)!!
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /*
    val logoBitmap = BitmapFactory.decodeResource(resources, R.drawable.my_logo)
    val qrBitmap = generateQRCodeWithCircularLogo("https://example.com", 512, logoBitmap)
     */
    fun generateQRCodeWithCircularLogo(content: String, size: Int = 512, logo: Bitmap): Bitmap? {
        return try {
            val bitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size
            )

            val qrBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    val color = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    qrBitmap.setPixel(x, y, color)
                }
            }

            // 원형 로고 + 테두리 생성
            val circularLogo = createCircularLogoWithBorder(logo, borderSize = 8, borderColor = Color.WHITE)
            val scaledLogo = Bitmap.createScaledBitmap(circularLogo, size / 5, size / 5, false)

            // QR 코드에 로고 합성
            val canvas = Canvas(qrBitmap)
            val centerX = (qrBitmap.width - scaledLogo.width) / 2
            val centerY = (qrBitmap.height - scaledLogo.height) / 2
            canvas.drawBitmap(scaledLogo, centerX.toFloat(), centerY.toFloat(), null)

            qrBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveBitmapToFile(context: Context, bitmap: Bitmap, filename: String): Uri? {
        val imagesDir = File(context.cacheDir, "qr_images")
        if (!imagesDir.exists()) imagesDir.mkdirs()

        val imageFile = File(imagesDir, "$filename.png")
        val outputStream = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider", // AndroidManifest에 등록된 provider
            imageFile
        )
    }

    /*
    val qrBitmap = generateQRCode("https://example.com")
val imageUri = saveBitmapToFile(this, qrBitmap!!, "qr_code_example")
if (imageUri != null) {
    shareImage(this, imageUri)
}
     */
    fun shareImage(context: Context, imageUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "QR 코드 공유하기"))
    }

    // QR 코드 이미지를 갤러리에 저장하는 함수 (MediaStore 방식)
    // 저장위치 Pictures/QR_Codes 폴더
    /*
    val qrBitmap = generateQRCode("https://example.com")
    val savedUri = saveBitmapToGallery(this, qrBitmap!!, "qr_example")
    if (savedUri != null) {
        Toast.makeText(this, "갤러리에 저장 완료!", Toast.LENGTH_SHORT).show()
    }
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, filename: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QR_Codes")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val contentResolver = context.contentResolver
        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        imageUri?.let { uri ->
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, contentValues, null, null)
        }

        return imageUri
    }

    // Function to correct image orientation
    private fun correctImageOrientation(bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val exif = inputStream?.use { ExifInterface(it) }
            
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postScale(-1f, 1f)
                    matrix.postRotate(90f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postScale(-1f, 1f)
                    matrix.postRotate(270f)
                }
                else -> {
                    // No rotation needed
                    return bitmap
                }
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap // Return original bitmap if correction fails
        }
    }

    // Function to dispatch camera intent
    private fun dispatchTakePictureIntent() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "QR_$timeStamp.jpg"
        
        val storageDir = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "QR_Codes")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        
        val imageFile = File(storageDir, imageFileName)
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            imageFile
        )
        
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }
        
        try {
            cameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "카메라를 열 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // New function to generate QR code with circular logo and border
    fun generateQRCodeWithCircularLogoAndBorder(content: String, size: Int = 512, logo: Bitmap): Bitmap? {
        return try {
            val bitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size
            )

            val qrBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    val color = if (bitMatrix[x, y]) foregroundColor else backgroundColor
                    qrBitmap.setPixel(x, y, color)
                }
            }

            // 원형 로고 + 테두리 생성
            val circularLogo = createCircularLogoWithBorder(logo, borderSize = 8, borderColor = borderColor)
            val scaledLogo = Bitmap.createScaledBitmap(circularLogo, size / 5, size / 5, false)

            // QR 코드에 로고 합성
            val canvas = Canvas(qrBitmap)
            val centerX = (qrBitmap.width - scaledLogo.width) / 2
            val centerY = (qrBitmap.height - scaledLogo.height) / 2
            canvas.drawBitmap(scaledLogo, centerX.toFloat(), centerY.toFloat(), null)

            // Add border to QR code
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.style = Paint.Style.STROKE
            paint.color = borderColor
            paint.strokeWidth = 4f // Border width
            canvas.drawRect(2f, 2f, (size - 2).toFloat(), (size - 2).toFloat(), paint)

            qrBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}