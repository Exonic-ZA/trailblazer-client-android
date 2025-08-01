package org.traccar.client.trailblazer.ui

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import org.traccar.client.Position
import org.traccar.client.PositionProviderFactory
import org.traccar.client.R
import org.traccar.client.trailblazer.model.ProtocolFormatter.formatRequest
import org.traccar.client.trailblazer.network.RequestManager.RequestHandler
import org.traccar.client.trailblazer.network.RequestManager.sendRequestAsync
import org.traccar.client.trailblazer.service.AutostartReceiver
import org.traccar.client.trailblazer.service.PositionProvider
import org.traccar.client.trailblazer.service.PositionProvider.PositionListener
import org.traccar.client.trailblazer.service.TrackingService
import org.traccar.client.trailblazer.ui.Trailblazer.Server_Details.device_id
import org.traccar.client.trailblazer.ui.Trailblazer.Server_Details.location_accuracy
import org.traccar.client.trailblazer.ui.Trailblazer.Server_Details.server_url
import org.traccar.client.trailblazer.util.BatteryOptimizationHelper
import io.sentry.Sentry
import io.sentry.SentryLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.traccar.client.BuildConfig
import org.traccar.client.trailblazer.api.ApiClient
import org.traccar.client.trailblazer.data.database.ImageMetadata
import org.traccar.client.trailblazer.util.CredentialHelper
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.edit


class Trailblazer() : AppCompatActivity(), PositionListener {

    private var retryAttempt = 2
    private lateinit var connectionStatus: TextView
    private lateinit var sosButton: ImageButton
    private lateinit var deviceId: TextView
    private lateinit var clockInImage: ImageView
    private lateinit var clockInText: TextView
    private lateinit var settingsButton: ImageButton
    private lateinit var photoCaptureButton: ImageButton
    private lateinit var imageFile: MultipartBody.Part

    private lateinit var cardView: CardView
    private lateinit var deviceIdText: EditText
    private lateinit var serverUrlLabel: EditText
    private lateinit var locationAccuracyLabel: EditText

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var alarmManager: AlarmManager
    private lateinit var alarmIntent: PendingIntent
    private var requestingPermissions: Boolean = false
    private lateinit var positionProvider: PositionProvider // = PositionProviderFactory.create(this, this)
    private val handler = Handler(Looper.getMainLooper())
    private var isLongPressed = false
    private var longPressRunnable: Runnable? = null
    private var pulsateAnimator: AnimatorSet? = null
    private lateinit var infoButton: ImageButton

    private var onlineStatus = false

    private var currentPosition: Position? = null
    private val CAMERA_REQUEST_CODE = 100

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        Sentry.addBreadcrumb("Trailblazer onCreate", "Lifecycle");
        Sentry.captureMessage("Trailblazer activity created", SentryLevel.INFO);
        //TODO: Improve better security of creds
       // AuthHelper.saveCredentials(this, "system@trailblazer.internal", "Babbling+Stomp+Bottling8+Payroll")

        try {
            enableEdgeToEdge();
            supportActionBar?.hide();
            setContentView(R.layout.activity_trailblazer);
        } catch (e: Exception) {
            Sentry.captureException(e);
        }

        setupView();
        setUpLoginDialog()
        showDisclaimerIfNeeded()
        setupPreferences();
        setOnclickListeners()
        setupLogsListener();
        checkBatteryOptimization();
        longPressSosButtonSetup();

    }

    private fun setUpLoginDialog() {
        supportFragmentManager.setFragmentResultListener(LoginDialog.REQUEST_KEY_LOGIN, this) { requestKey, bundle ->
            if (requestKey == LoginDialog.REQUEST_KEY_LOGIN) {
                val isSuccess = bundle.getBoolean(LoginDialog.BUNDLE_KEY_IS_SUCCESS)
                if (isSuccess) {
                    val username = bundle.getString(LoginDialog.BUNDLE_KEY_USERNAME)
                    val password = bundle.getString(LoginDialog.BUNDLE_KEY_PASSWORD)

                    performImageUpload(deviceId.text.toString(), imageFile,
                        username.toString(), password.toString()
                    )

                }
                else
                {
                    // Handle login cancelled or failed
                    Toast.makeText(this, "Login Cancelled or Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun showDisclaimerIfNeeded() {
        val sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val hasAcceptedDisclaimer = sharedPreferences.getBoolean("disclaimer_accepted", false)

        if (!hasAcceptedDisclaimer) {
            // Create a dialog using AlertDialog.Builder
            val dialogBuilder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.disclaimer_dialog, null)
            dialogBuilder.setView(dialogView)
            dialogBuilder.setCancelable(false)

            val disclaimerDialog = dialogBuilder.create()

            // Make dialog transparent to show only the CardView with rounded corners
            disclaimerDialog.window?.let { window ->
                // Set background to transparent
                window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                // Set dim amount for background
                window.setDimAmount(0.6f)
            }

            // Set click listener for the consent button
            dialogView.findViewById<Button>(R.id.btn_consent_button).setOnClickListener {
                // Save that user has accepted the disclaimer
                sharedPreferences.edit().putBoolean("disclaimer_accepted", true).apply()
                disclaimerDialog.dismiss()
            }

            // Show the dialog
            disclaimerDialog.show()
        }
    }

    private val takePictureResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap?
            imageBitmap?.let {
                imageFile = prepareImageFile(imageBitmap) // Convert Bitmap to Multipart
                showConfirmationDialog(deviceId.text.toString(), imageFile)
            }
        }
    }


    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            handleCameraPermissionDenied()
        }
    }

    private fun handleCameraPermissionDenied() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            // User denied but can still be asked again
            showPermissionRationaleDialog()
        } else {
            // User denied with "Don't ask again" OR first time denial
            showGoToSettingsDialog()
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera Permission Needed")
            .setMessage("This app needs camera access to take photos.")
            .setPositiveButton("Try Again") { _, _ ->
                // This will show the system permission dialog again
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showGoToSettingsDialog() {
        // Only show this when user selected "Don't ask again"
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Camera permission was permanently denied. Please enable it in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setOnclickListeners() {
        photoCaptureButton.setOnClickListener {
            checkCameraPermissionAndLaunch()
            // TODO:
/*            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val credentialHelper = CredentialHelper(this@Trailblazer)
                    val preferences = PreferenceManager.getDefaultSharedPreferences(this@Trailblazer)
                    val storedCredentials =
                        preferences.getString("trailblazer_username", "")
                            ?.takeIf { it.isNotBlank() }
                            ?.let { credentialHelper.getStoredCredentials(it) }

                    if (storedCredentials != null) {
                        checkCameraPermissionAndLaunch()
                    }
                    else
                    {
                        showLoginDialog()
                    }
                }
                catch (e:Exception) {
                }
            }*/


            /*val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
            }*/
        }

        infoButton.setOnClickListener {
            // Open About Us activity when info button is clicked
            val intent = Intent(this, AboutUsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureResultLauncher.launch(takePictureIntent)
    }
/*
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                val imageFile = prepareImageFile(imageBitmap) // Convert Bitmap to Multipart
                showConfirmationDialog(deviceId.text.toString(), imageFile)
            } else {
                Log.e("API_CALL", "Failed to capture image!")
            }
        }
    }*/

    // Convert Bitmap to Multipart
    private fun prepareImageFile(bitmap: Bitmap): MultipartBody.Part {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()

        val requestFile = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("file", "image.jpg", requestFile)
    }

    private fun showConfirmationDialog(deviceSerial: String, imageFile: MultipartBody.Part) {
        ConfirmImageDialog(
            onRetake = {
                Toast.makeText(this, "Retake Clicked", Toast.LENGTH_SHORT).show()
            },
            onSend = { onComplete ->
                CoroutineScope(Dispatchers.IO).launch {
                    submitImageMetadata(deviceSerial, imageFile) // Pass required parameters
                    withContext(Dispatchers.Main) {
                        onComplete.invoke() // Hide progress bar & close dialog
                    }
                }
            }
        ).show(supportFragmentManager, "ConfirmImageDialog")
    }

    private fun submitImageMetadata(deviceSerial: String, imageFile: MultipartBody.Part) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val credentialHelper = CredentialHelper(this@Trailblazer)
                val preferences = PreferenceManager.getDefaultSharedPreferences(this@Trailblazer)
                // Try to get stored credentials first
                val storedCredentials = withContext(Dispatchers.IO) {
                    preferences.getString("trailblazer_username", "")
                        ?.takeIf { it.isNotBlank() }
                        ?.let { credentialHelper.getStoredCredentials(it) }
                }

                if (storedCredentials != null) {
                    // Use stored credentials
                    val (username, password) = storedCredentials
                    performImageUpload(deviceSerial, imageFile, username, password)
                } else {
                    // Show login dialog
                    Log.d(TAG, "submitImageMetadata: submitImageMetadata storedCredentials == null")
                    showLoginDialog()
                }

            } catch (e: Exception) {
                Log.e(TAG, "submitImageMetadata: ", e)
                Sentry.captureException(e)
                // If credential retrieval fails, show login dialog as fallback
                showLoginDialog()
            }
        }
    }

    private fun showLoginDialog(retry:Boolean = false) {
        val loginDialog = LoginDialog.newInstance(retry)
        loginDialog.show(supportFragmentManager, "LoginDialogTag")
    }


    // Helper function to dismiss progress dialog and show alert dialog
    private suspend fun showResultDialog(isSuccess: Boolean, message: String, progressDialog: ProgressDialog) {
        withContext(Dispatchers.Main) {
            progressDialog.dismiss() // Ensure dialog is dismissed on the main thread

            AlertDialog.Builder(this@Trailblazer)
                .setTitle(if (isSuccess) "Success" else "Error")
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }


    private fun performImageUpload(deviceSerial: String, imageFile: MultipartBody.Part, username: String, password: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val preferences = PreferenceManager.getDefaultSharedPreferences(this@Trailblazer)
            preferences.edit(commit = true) { putString("trailblazer_username", username) }
            val progressDialog = ProgressDialog(this@Trailblazer).apply {
                setMessage("Uploading photo...")
                setCancelable(false)
                show()
            }

            try {
                withContext(Dispatchers.IO) {
                    // Create API service with credentials
                    val apiService = ApiClient.create(this@Trailblazer, username, password)

                    val deviceResponse = apiService.getDeviceBySerial(deviceSerial)
                    if (deviceResponse.code() == 401 && retryAttempt > 0)
                    {
                        progressDialog.dismiss()
                        retryAttempt--
                        showLoginDialog(true)
                        return@withContext
                    }
                    if (deviceResponse.code() == 401 && retryAttempt == 0)
                    {
                        progressDialog.dismiss()
                        showResultDialog(false, "Your credentials was entered incorrectly too many times.", progressDialog)
                        return@withContext
                    }
                    if (!deviceResponse.isSuccessful || deviceResponse.body().isNullOrEmpty()) {
                        progressDialog.dismiss()
                        showResultDialog(false, "Device not found.", progressDialog)
                        return@withContext
                    }

                    val deviceId = deviceResponse.body()?.first()?.id ?: return@withContext

                    val metadata = ImageMetadata(
                        fileName = "tv",
                        fileExtension = "jpg",
                        deviceId = deviceId.toString(),
                        latitude = currentPosition?.latitude ?: 0.00,
                        longitude = currentPosition?.longitude ?: 0.00
                    )

                    val metadataResponse = apiService.uploadMetadata(metadata)
                    if (!metadataResponse.isSuccessful) {
                        showResultDialog(false, "Failed to upload metadata.", progressDialog)
                        return@withContext
                    }

                    val imageId = metadataResponse.body()?.id ?: return@withContext
                    val imageUploadResponse = apiService.uploadImage(imageId, imageFile.body!!)

                    if (imageUploadResponse.isSuccessful) {
                        showResultDialog(true, "Photo uploaded successfully!", progressDialog)
                    } else {
                        showResultDialog(false, "Failed to upload photo.", progressDialog)
                    }
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Sentry.captureException(e)
                showResultDialog(false, "Something went wrong!", progressDialog)
            }
        }
    }


    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Ensure the directory exists
        storageDir?.mkdirs()

        return File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
    }
    private fun showToast(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        val file = File(cacheDir, "temp_image.jpg") // Create a temp file
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return file.absolutePath // Return the absolute path of the copied file
        } catch (e: Exception) {
            Log.e("API_CALL", "Error getting real path: ${e.message}")
        }
        return null
    }


    private fun checkBatteryOptimization() {

        // Create an instance of BatteryOptimizationHelper
        val batteryOptimizationHelper = BatteryOptimizationHelper()

        // Check if battery optimization exception is needed and request it
        if (batteryOptimizationHelper.requestException(this)) {
            Log.i(TAG, "Battery optimization exception request was triggered.")
        } else {
            Log.i(TAG, "Battery optimization exception already granted or not needed.")
        }

    }

    object Server_Details {
        lateinit var device_id: String
        lateinit var server_url: String
        lateinit var location_accuracy: String
    }

    companion object {
        public val TAG = "TRAIL_CONNECTION"
        private const val ALARM_MANAGER_INTERVAL = 15000
        private const val RETRY_DELAY = 5 * 1000
        const val KEY_DEVICE = "id"
        const val KEY_URL = "url"
        const val KEY_INTERVAL = "interval"
        const val KEY_DISTANCE = "distance"
        const val KEY_ANGLE = "angle"
        const val KEY_ACCURACY = "accuracy"
        const val KEY_STATUS = "status"
        const val KEY_BUFFER = "buffer"
        const val KEY_WAKELOCK = "wakelock"
        private const val PERMISSIONS_REQUEST_LOCATION = 2
        private const val PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 3
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_LOCATION)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted for background location (required for Android 10+)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PERMISSIONS_REQUEST_BACKGROUND_LOCATION)
        } else {
            // Permissions granted, proceed with location updates
            updateConnectionOnline()
            positionProvider.startUpdates()
            positionProvider.requestSingleLocation()
            startTrackingService(checkPermission = true, initialPermission = false)
            Log.i(TAG, "User connected successfully.")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSIONS_REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Sentry.captureMessage("Location permission granted", SentryLevel.INFO);
                    connectUser();
                } else {
                    Sentry.captureMessage("Location permission denied", SentryLevel.WARNING);
                    Toast.makeText(this, "Location permission is required to access GPS", Toast.LENGTH_LONG).show()
                }
            }
            PERMISSIONS_REQUEST_BACKGROUND_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Sentry.captureMessage("Background location permission granted", SentryLevel.INFO);
                    updateConnectionOnline();
                    positionProvider.startUpdates();
                    startTrackingService(checkPermission = true, initialPermission = false);
                    Log.i(TAG, "Background location permission granted.")
                } else {
                    Sentry.captureMessage("Background location permission denied", SentryLevel.WARNING);
                    Toast.makeText(this, "Background location permission is required for tracking location in the background.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }



    private fun setupLogsListener() {
        findViewById<ImageButton>(R.id.btn_Logs).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.linContent, LogsFragment())
                .addToBackStack(null)
                .commit()
        }

    }

    private fun longPressSosButtonSetup() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        sosButton.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isLongPressed = false // Reset state
                    longPressRunnable = Runnable {
                        isLongPressed = true
                        Sentry.captureMessage("User long pressed", SentryLevel.INFO);
                        sendAlarm() // Send SOS alarm after long press
                        startPulsatingAnimation(view) // Start animation when long pressed

                        // Add vibration
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            vibrator.vibrate(500) // Fallback for older devices
                        }
                    }
                    handler.postDelayed(longPressRunnable!!, 2000) // 2 seconds delay
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPressRunnable!!)
                    if (isLongPressed) {
                        stopPulsatingAnimation(view) // Stop animation if long pressed
                    } else {
                        // Handle regular click
                        view.performClick()
                    }
                }
            }
            true
        }

        sosButton.setOnClickListener {
            stopPulsatingAnimation(sosButton)
            if (!isLongPressed) {
                Sentry.captureMessage("User did not long pressed", SentryLevel.INFO);
                Toast.makeText(
                    this@Trailblazer,
                    "Please long press for 2s to Activate SOS",
                    Toast.LENGTH_SHORT
                ).show()

            }
        }
    }



    private fun sendAlarm() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Sentry.captureMessage("SOS alarm triggered", SentryLevel.INFO)

                val progressDialog = ProgressDialog(this@Trailblazer).apply {
                    setMessage("Sending SOS...")
                    setCancelable(false)
                    show()
                }

                Toast.makeText(this@Trailblazer, "Now sending SOS to the team...", Toast.LENGTH_SHORT).show()

                val positionProvider = PositionProviderFactory.create(this@Trailblazer, object : PositionListener {
                    override fun onPositionUpdate(position: Position) {
                        CoroutineScope(Dispatchers.Main).launch {
                            //progressDialog.dismiss() // Dismiss on the main thread

                            if (position == null) {
                                Sentry.captureMessage("Received null position in onPositionUpdate", SentryLevel.ERROR)
                                Toast.makeText(this@Trailblazer, "Failed to get location", Toast.LENGTH_LONG).show()
                                return@launch
                            }

                            val preferences = PreferenceManager.getDefaultSharedPreferences(this@Trailblazer)
                            val url = server_url

                            if (url.isNullOrEmpty()) {
                                Sentry.captureMessage("SOS failed: URL is missing from preferences", SentryLevel.ERROR)
                                Toast.makeText(this@Trailblazer, "Missing SOS server URL", Toast.LENGTH_LONG).show()
                                return@launch
                            }

                            position.deviceId = device_id?.replace("\\s".toRegex(), "")?.uppercase() ?: "UNKNOWN"
                            currentPosition = position
                            Sentry.addBreadcrumb("Position update received: $position", "GPS")

                            val request = formatRequest(url, position, ShortcutActivity.ALARM_SOS)

                            // Perform request on IO dispatcher
                            withContext(Dispatchers.IO) {
                                sendRequestAsync(request, object : RequestHandler {
                                    override fun onComplete(success: Boolean) {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            if (success) {
                                                progressDialog.dismiss()
                                                Sentry.captureMessage("SOS sent successfully", SentryLevel.INFO)
                                                showSuccessModal()
                                            } else {
                                                progressDialog.dismiss()
                                                Sentry.captureMessage("SOS send failed", SentryLevel.ERROR)
                                                Toast.makeText(this@Trailblazer, R.string.status_send_fail, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                })
                            }
                        }
                    }

                    override fun onPositionError(error: Throwable) {
                        CoroutineScope(Dispatchers.Main).launch {
                            progressDialog.dismiss()
                            val errorMsg = error.message ?: "Unknown location error"
                            Toast.makeText(this@Trailblazer, errorMsg, Toast.LENGTH_LONG).show()
                            Sentry.captureException(error)
                        }
                    }
                })

                if (positionProvider != null) {
                    positionProvider.requestSingleLocation()
                } else {
                    progressDialog.dismiss()
                    Sentry.captureMessage("PositionProviderFactory returned null", SentryLevel.ERROR)
                    Toast.makeText(this@Trailblazer, "Failed to initialize GPS", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Sentry.captureException(e)
            }
        }
    }






    private fun showSuccessModal() {
        AlertDialog.Builder(this)
            .setTitle("SOS Alert")
            .setMessage("SOS alarm has been sent successfully")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }


    private fun setupView() {
        connectionStatus = findViewById<TextView>(R.id.connection_status)
        sosButton = findViewById<ImageButton>(R.id.sos)
        infoButton = findViewById(R.id.info_button)
        deviceId = findViewById<TextView>(R.id.device_id)

        clockInImage = findViewById<ImageView>(R.id.clock_in_image)
        clockInText = findViewById<TextView>(R.id.clock_in_text)
        settingsButton = findViewById<ImageButton>(R.id.settings_button)
        photoCaptureButton = findViewById(R.id.btn_photo)

        cardView = findViewById<CardView>(R.id.settings_view)
        deviceIdText = findViewById<EditText>(R.id.settings_device_id)
        serverUrlLabel = findViewById<EditText>(R.id.settings_server_url)
        locationAccuracyLabel = findViewById<EditText>(R.id.settings_location_accuracy)
        cardView.isVisible = false

        updateConnectionOffline()
    }

    private fun setupPreferences() {
        sharedPreferences = getPreferences(MODE_PRIVATE)
        alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val originalIntent = Intent(this, AutostartReceiver::class.java)
        originalIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        alarmIntent = PendingIntent.getBroadcast(this, 0, originalIntent, flags)

        if (sharedPreferences.contains(KEY_DEVICE)) {
            deviceId.text = sharedPreferences.getString(KEY_DEVICE, "")
        } else {
            sharedPreferences.edit().putString(KEY_DEVICE, "").apply()
            deviceId.setText("")
        }

        device_id = deviceId.text.toString()
        if (BuildConfig.DEBUG) {
            server_url = getString(R.string.settings_server_url_value_staging)
        }
        else {
            server_url = getString(R.string.settings_server_url_value)
        }
        location_accuracy = getString(R.string.settings_location_accuracy_value)
        positionProvider = PositionProviderFactory.create(this, this)
    }

    public final fun clockInAndOut(view: View) {
        if (deviceId.text.toString().trim().isNotEmpty() && deviceId.text.toString().trim().isNotBlank()) {
            if (this.onlineStatus) {
                Log.i(TAG, "User is online. Proceeding to disconnect.")
                disconnectUser()
            } else {
                Log.i(TAG, "User is offline. Proceeding to connect.")
                connectUser()
            }
        } else {
            Log.w(TAG, "Device ID is empty or blank. Showing card view.")
            showCardView()
        }
    }

    private fun startPulsatingAnimation(view: View) {
        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f).apply {
            duration = 600
            interpolator = LinearInterpolator()
            repeatCount = ObjectAnimator.INFINITE
        }

        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 1f).apply {
            duration = 600
            interpolator = LinearInterpolator()
            repeatCount = ObjectAnimator.INFINITE
        }

        pulsateAnimator = AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator)
            start()
        }
    }

    private fun stopPulsatingAnimation(view: View) {
        pulsateAnimator?.end()  // Stop and remove all running animations
        pulsateAnimator = null  // Clear reference

        view.scaleX = 1f  // Reset to normal
        view.scaleY = 1f
    }

    public final fun settingsClicked(view: View) {
        Log.d(TAG, "settingsClicked invoked")
        //disconnectUser()
        Log.i(TAG, "User disconnected successfully. Showing card view.")
        showCardView()
    }

    private fun connectUser() {
        try {
            Log.d(TAG, "Connecting user...")
            Sentry.captureMessage("Attempting to connect user", SentryLevel.INFO);
            onlineStatus = true
            checkLocationPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect user: ${e.message}", e)
            Sentry.captureException(e);
        }
    }

    fun updateConnectionOnline() {
        connectionStatus.text = getString(R.string.status_connected)
        connectionStatus.setTextColor(ContextCompat.getColor(this, R.color.primary))
        connectionStatus.background = ResourcesCompat.getDrawable(getResources(),
            R.drawable.status_connected, null)

        clockInText.text = getString(R.string.clock_out)
        clockInImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
            R.drawable.clock_out, null))
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    fun updateConnectionOffline() {
        connectionStatus.text = getString(R.string.status_disconnected)
        connectionStatus.setTextColor(ContextCompat.getColor(this, R.color.light_gray))
        connectionStatus.background = ResourcesCompat.getDrawable(getResources(),
            R.drawable.status_disconnected, null)

        clockInText.text = getString(R.string.clock_in)
        clockInImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
            R.drawable.clock_in, null))
    }

    private fun disconnectUser() {
        try {
            Log.d(TAG, "Disconnecting user...")
            Sentry.captureMessage("Attempting to disconnect user", SentryLevel.INFO);
            onlineStatus = false
            updateConnectionOffline()
            positionProvider.stopUpdates()
            stopTrackingService()
            Log.i(TAG, "User disconnected successfully.")
        } catch (e: Exception) {
            Sentry.captureException(e);
            Log.e(TAG, "Failed to disconnect user: ${e.message}", e)
        }
    }

    private fun showCardView() {
        Log.d(TAG, "Showing card view")
        deviceIdText.setText(device_id)
        serverUrlLabel.setText(server_url)
        locationAccuracyLabel.setText(location_accuracy)
        cardView.isVisible = true
    }

    public final fun cancelSettingsClicked(view: View) {
        Log.d(TAG, "cancelSettingsClicked invoked")
        hideKeyboard()
        cardView.isVisible = false
        Log.i(TAG, "Settings view canceled.")
    }

    public final fun saveSettingsClicked(view: View) {
        Log.d(TAG, "saveSettingsClicked invoked")
        hideKeyboard()
        if (deviceIdText.text.toString().trim().isNotEmpty() && deviceIdText.text.toString().trim().isNotBlank()) {
            try {
                sharedPreferences.edit().putString(KEY_DEVICE, deviceIdText.text.toString()).apply()
                deviceId.text = sharedPreferences.getString(KEY_DEVICE, "")
                device_id = deviceId.text.toString()
                cardView.isVisible = false
                Log.i(TAG, "Settings saved successfully. Device ID updated to: $device_id")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save settings: ${e.message}", e)
            }
        } else {
            Log.w(TAG, "Device ID is empty or blank. Prompting user.")
            Toast.makeText(this, "Please enter the device id", Toast.LENGTH_LONG).show()
        }
    }

    private fun hideKeyboard() {
        try {
            Log.d(TAG, "Hiding keyboard")
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(deviceIdText.windowToken, 0)
            Log.i(TAG, "Keyboard hidden successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide keyboard: ${e.message}", e)
        }
    }

    private fun showBackgroundLocationDialog(context: Context, onSuccess: () -> Unit) {
        val builder = AlertDialog.Builder(context)
        val option = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.backgroundPermissionOptionLabel
        } else {
            context.getString(R.string.request_background_option)
        }
        builder.setMessage(context.getString(R.string.request_background, option))
        builder.setPositiveButton(android.R.string.ok) { _, _ -> onSuccess() }
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.show()
    }



    private fun startTrackingService(checkPermission: Boolean, initialPermission: Boolean) {
        Log.d(TAG, "Starting tracking service with checkPermission=$checkPermission, initialPermission=$initialPermission")
        // Existing logic with appropriate logs for permission checks and exceptions
        var permission = initialPermission
        if (checkPermission) {
            val requiredPermissions: MutableSet<String> = HashSet()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            permission = requiredPermissions.isEmpty()
            if (!permission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(requiredPermissions.toTypedArray(), PERMISSIONS_REQUEST_LOCATION)
                }
                return
            }
        }
        if (permission) {
            ContextCompat.startForegroundService(this, Intent(this, TrackingService::class.java))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    ALARM_MANAGER_INTERVAL.toLong(), ALARM_MANAGER_INTERVAL.toLong(), alarmIntent
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestingPermissions = true
                showBackgroundLocationDialog(this) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PERMISSIONS_REQUEST_BACKGROUND_LOCATION)
                }
            } else {
                requestingPermissions = BatteryOptimizationHelper().requestException(this)
            }
        } else {
            sharedPreferences.edit().putBoolean(KEY_STATUS, false).apply()
        }
        Log.i(TAG, "Tracking service started successfully.")


    }

    private fun stopTrackingService() {
        try {
            Log.d(TAG, "Stopping tracking service...")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                alarmManager.cancel(alarmIntent)
            }
            stopService(Intent(this, TrackingService::class.java))
            Log.i(TAG, "Tracking service stopped successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop tracking service: ${e.message}", e)
        }
    }

    override fun onPositionUpdate(position: Position) {
        Log.d(TAG, "Position update received: $position")
        handler.postDelayed({
            if (onlineStatus) {
                Log.i(TAG, "Sending position update...")
                currentPosition = position
                send(position)
            }
        }, RETRY_DELAY.toLong())
    }

    override fun onPositionError(error: Throwable) {
        Log.e(TAG, "Position error encountered: ${error.message}", error)
    }

    private fun send(position: Position) {
        try {
            position.deviceId = device_id.replace("\\s".toRegex(), "").uppercase()
            val serverUrl: String = server_url
            val request = formatRequest(serverUrl, position)

            sendRequestAsync(request, object : RequestHandler {
                override fun onComplete(success: Boolean) {
                    if (success) {
                        Log.i(TAG, "Position data sent successfully.")
                    } else {
                        Log.w(TAG, "Failed to send position data.")
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send position data: ${e.message}", e)
        }
    }

}