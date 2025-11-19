package com.odom.barcodeqr.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.odom.barcodeqr.R
import com.odom.barcodeqr.databinding.FragmentGenerateBinding
import com.odom.barcodeqr.history.HistoryViewModel
import java.io.File
import java.io.FileOutputStream
import yuku.ambilwarna.AmbilWarnaDialog

class GenerateFragment : Fragment() {


    private lateinit var viewModel: HistoryViewModel


    private var _binding: FragmentGenerateBinding? = null

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

//        binding.imageView.setImageBitmap(generateQRCode("Hello World"))

        // val logoBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_notifications_black_24dp)
        // bitmap인지 vector drawable인지 확인

        var defaultColor = Color.RED

        val strokeWidth = 4 // in pixels
        var strokeColor = defaultColor
        val cornerRadius2 = 12f // optional


        binding.btPickColor.setOnClickListener {
            val colorPicker = AmbilWarnaDialog(requireContext(), defaultColor,
                object : AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onCancel(dialog: AmbilWarnaDialog) {
                        // 취소 시 아무 동작 없음
                    }

                    override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                        defaultColor = color
                        binding.colorPreview.setBackgroundColor(color)

                        val drawable = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            setStroke(strokeWidth, defaultColor)
                            cornerRadius = cornerRadius2
                            setColor(Color.TRANSPARENT) // background color
                        }

                        binding.imageView.background = drawable
                    }
                })
            colorPicker.show()
        }

        var drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setStroke(strokeWidth, strokeColor)
            cornerRadius = cornerRadius2
            setColor(Color.TRANSPARENT) // background color
        }

        binding.imageView.background = drawable

        binding.buttonGenerate.setOnClickListener {
            generateQRCodeWithCircularLogo()

            // 저장
            viewModel.addHistory(binding.textView.text.toString())
        }

        binding.buttonSave.setOnClickListener {
            val uri = saveBitmapToFile(requireContext(), binding.imageView.drawable.toBitmap(), "qr_code_example")
            Toast.makeText(requireContext(), uri.toString(), Toast.LENGTH_SHORT).show()
        }

        binding.buttonShare.setOnClickListener {
            //val qrBitmap = generateQRCode()
         //   val dd = getBitmapFromVectorDrawable(requireContext(), binding.imageView.id)
            val imageUri = saveBitmapToFile(requireContext(),  binding.imageView.drawable.toBitmap(), "qr_code_example")
            if (imageUri != null) {
                shareImage(requireContext(), imageUri)
            }

        }

    }

    fun generateQRCodeWithCircularLogo() {
        val logoBitmap = getBitmapFromVectorDrawable(requireContext(), R.drawable.ic_launcher_foreground)
        val qrBitmap = generateQRCodeWithCircularLogo(binding.textView.text.toString(), 512, logoBitmap)
        binding.imageView.setImageBitmap(qrBitmap)

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

    /*
    val qrBitmap = generateColoredQRCode(
        content = "https://example.com",
        size = 512,
        fgColor = Color.BLUE,     // 전경색 (QR 블록)
        bgColor = Color.YELLOW    // 배경색
    )

    */
    fun generateColoredQRCode(content: String, size: Int = 512, fgColor: Int, bgColor: Int): Bitmap? {
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}