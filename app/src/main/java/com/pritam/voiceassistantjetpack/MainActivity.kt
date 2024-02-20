package com.pritam.voiceassistantjetpack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Text
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Checkbox
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.TimeText
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.ResponseStoppedException
import com.google.ai.client.generativeai.type.generationConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.pritam.voiceassistantjetpack.theme.VoiceAssistantWatchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.EnumMap
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener , DataClient.OnDataChangedListener {

    private val dataClient: DataClient by lazy { Wearable.getDataClient(this) }
    private var healthcareData by mutableStateOf("")
    private var groceriesData by mutableStateOf("")
    private var bodyMeasurementData by mutableStateOf("")

    private lateinit var textToSpeech: TextToSpeech
    private val RECORD_AUDIO_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
        setContent {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    RECORD_AUDIO_REQUEST_CODE
                )
            }
            WearApp(sharedPreferences,textToSpeech)
        }
        textToSpeech = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.getDefault()
        } else {
            println("TextToSpeech initialization failed")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RECORD_AUDIO_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    override fun onStart() {
        super.onStart()
        dataClient.addListener(this)
    }

    override fun onStop() {
        dataClient.removeListener(this)
        super.onStop()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path

                if ("/data-path" == path) {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val receivedTimestamp = dataMapItem.dataMap.getLong("timestamp", 0)
                    val currentTimestamp = System.currentTimeMillis()

                    if (currentTimestamp - receivedTimestamp < 5000) {
                        healthcareData = dataMapItem.dataMap.getString("Healthcare") ?: ""
                        groceriesData = dataMapItem.dataMap.getString("Groceries") ?: ""
                        bodyMeasurementData = dataMapItem.dataMap.getString("BodyMeasurement") ?: ""

                        saveReceivedDataToPreferences("Healthcare",healthcareData)
                        saveReceivedDataToPreferences("Groceries",groceriesData)
                        saveReceivedDataToPreferences("BodyMeasurement",bodyMeasurementData)
                    }
                }
            }
        }
    }
    private fun saveReceivedDataToPreferences(key: String, data: String) {
        val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, data)
        editor.apply()
    }

}

@Composable
fun WearApp(sharedPreferences: SharedPreferences,textToSpeech: TextToSpeech) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "homeScreen") {
        composable("homeScreen") { HomeScreen(navController) }
        composable("voiceAssistantScreen") { VoiceAssistantScreen(textToSpeech) }
        composable("dataShareScreen") { DataShareScreen(sharedPreferences, navController) }
        composable(
            route = "showQR/{data}",
            arguments = listOf(navArgument("data") { type = NavType.StringType })
        ) { backStackEntry ->
            val dataReceived = backStackEntry.arguments?.getString("data") ?: ""
            ShowQR(dataReceived, navController)
        }
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    VoiceAssistantWatchTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(modifier = Modifier
                    .wrapContentWidth()
                    .padding(8.dp),
                    onClick = { navController.navigate("voiceAssistantScreen") }) {
                    Text(
                        modifier = Modifier
                            .padding(start = 10.dp, end = 10.dp),
                        fontSize = 10.sp,
                        text = "Voice assistant")
                }
                Button(modifier = Modifier
                    .wrapContentWidth()
                    .padding(8.dp),
                    onClick = { navController.navigate("dataShareScreen") }) {
                    Text(
                        modifier = Modifier
                            .padding(start = 10.dp, end = 10.dp),
                        fontSize = 10.sp,
                        text = "Data Share")
                }
            }

        }
    }

}

@Composable
fun VoiceAssistantScreen(textToSpeech: TextToSpeech) {
    VoiceAssistantWatchTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            SpeechToTextScreen(textToSpeech)
        }
    }

}

data class ToggleableInfo(
    val isChecked: Boolean,
    val text: String
)

@Composable
fun DataShareScreen(sharedPreferences: SharedPreferences,navController: NavHostController) {
    VoiceAssistantWatchTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val checkboxes = remember {
                mutableStateListOf(
                    ToggleableInfo(
                        isChecked = false,
                        text = "Healthcare"
                    ),
                    ToggleableInfo(
                        isChecked = false,
                        text = "Groceries"
                    ),
                    ToggleableInfo(
                        isChecked = false,
                        text = "BodyMeasurement"
                    )
                )
            }

            checkboxes.forEachIndexed{index, info ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable{
                        checkboxes[index] = info.copy(
                            isChecked = !info.isChecked
                        )
                    }
                ) {
                    Checkbox(checked = info.isChecked,
                        onCheckedChange = {isChecked ->
                            checkboxes[index] = info.copy(
                                isChecked = isChecked
                            )
                        })
                    Text(text = info.text)
                }
            }

            Button(onClick = {
                val checkedCheckboxes = checkboxes.filter { it.isChecked }

                var showingData = ""

                checkedCheckboxes.forEach {
                    val receivedData = sharedPreferences.getString(it.text, "") ?: ""
                    showingData += "\n" + "${it.text}: " + receivedData
                    println("Checked Checkbox: ${it.text}")
                }

                navController.navigate("showQR/$showingData")
            }) {
                Text("Show QR")
            }
        }
    }
}

@Composable
fun ShowQR(data: String, navController: NavHostController) {
    VoiceAssistantWatchTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
            generatedBitmap = generateQRCode(data)
            generatedBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Generated QR Code",
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White)
                )
            }
        }
    }
}

fun generateQRCode(text: String): Bitmap? {
    try {
        val writer = QRCodeWriter()
        val hints: MutableMap<EncodeHintType, Any> = EnumMap(EncodeHintType::class.java)
        hints[EncodeHintType.MARGIN] = 1
        val bitMatrix: BitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 200, 200, hints)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }

        return bitmap
    } catch (e: WriterException) {
        e.printStackTrace()
    }
    return null
}


@Composable
fun SpeechToTextScreen(textToSpeech: TextToSpeech) {
    val context = LocalContext.current

    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    val viewModel: SpeechToTextViewModel = viewModel()

    var transcription by remember { mutableStateOf("") }

    transcription = viewModel.transcription

    var isMicOn by remember { mutableStateOf(false) }

    MicToggle { micState ->
        isMicOn = micState
    }

    var answer by remember {
        mutableStateOf("")
    }

    val config = generationConfig {
        temperature = 0.9f
        topK = 16
        topP = 1f
        maxOutputTokens = 200

    }

    val generativeModel = GenerativeModel(
        modelName = "gemini-pro",
        apiKey = "AIzaSyBGJLECyMVnHOPmY-2mBwHEata8JMPr0jI",
        generationConfig = config
    )

    val prompt = transcription
    println(prompt)

    LaunchedEffect(prompt) {
        if (prompt.isNotEmpty()){
            try {
                val response = withContext(Dispatchers.IO) {
                    generativeModel.generateContent(prompt = prompt).text!!
                }
                answer = response
                textToSpeech.speak(answer, TextToSpeech.QUEUE_FLUSH, null, null)
            } catch (e: ResponseStoppedException) {
                answer = "Content generation stopped. Please provide a shorter prompt."
            }
        }

    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = transcription,
            fontSize = 18.sp
        )

        Text(
            text = answer,
            fontSize = 16.sp
        )

        SpeechToText(
            recognizer = recognizer,
            onTranscription = { transcription ->
                viewModel.transcription = transcription
            },
            isMicOn = isMicOn
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MicToggle(onMicStateChanged: (Boolean) -> Unit) {
    var isMicOn by remember { mutableStateOf(false) }
    var previousAnimationRes by remember { mutableStateOf(R.raw.mic) }

    Box(
        modifier = Modifier
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (!isMicOn) {
                            previousAnimationRes = R.raw.audio_wave
                            isMicOn = true
                            onMicStateChanged(true)
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        if (isMicOn) {
                            previousAnimationRes = R.raw.mic
                            isMicOn = false
                            onMicStateChanged(false)
                        }
                    }
                }
                true
            }
    ) {
        val composition by rememberLottieComposition(
            spec = LottieCompositionSpec.RawRes(
                if (isMicOn) R.raw.audio_wave else previousAnimationRes
            )
        )

        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = if (isMicOn) LottieConstants.IterateForever else 1
        )

        LottieAnimation(
            composition = composition,
            progress = progress,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
fun SpeechToText(
    recognizer: SpeechRecognizer,
    onTranscription: (String) -> Unit,
    isMicOn: Boolean
) {
    LaunchedEffect(key1 = isMicOn) {
        if (isMicOn) {
            startListeningForSpeech(recognizer, onTranscription)
        } else {
            recognizer.stopListening()
        }
    }
}

private fun startListeningForSpeech(
    recognizer: SpeechRecognizer,
    onTranscription: (String) -> Unit
) {
    recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(p0: Bundle?) {}

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(p0: Float) {}

        override fun onBufferReceived(p0: ByteArray) {}

        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {}

        override fun onResults(results: Bundle?) {
            val transcription =
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.joinToString("\n")
                    ?.trim()
                    ?: "Nothing detected"

            println(transcription)

            onTranscription(transcription)
        }

        override fun onPartialResults(p0: Bundle?) {}
        override fun onEvent(p0: Int, p1: Bundle?) {}
    })

    val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    recognizer.startListening(speechIntent)
}

class SpeechToTextViewModel : ViewModel() {
    var transcription: String by mutableStateOf("")
}