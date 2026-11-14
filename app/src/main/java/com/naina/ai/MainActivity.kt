
package com.naina.ai

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

data class ChatMessage(val text: String, val fromUser: Boolean)

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ur", "PK")
            }
        }

        setContent {
            NainaChat(onSpeak = { speak(it) })
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "naina_reply")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun NainaChat(onSpeak: (String) -> Unit) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                "Assalam-o-alaikum! Main Naina hoon 💗 Tumhara din kaisa guzar raha hai?",
                false
            )
        )
    }

    var input by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("Sweet Girlfriend") }
    var loading by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinEntry by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }
    var pinSetup by remember { mutableStateOf(false) }

    val prefs = remember {
        context.getSharedPreferences(
            "naina_settings",
            android.content.Context.MODE_PRIVATE
        )
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()

        if (!spoken.isNullOrBlank()) input = spoken
    }

    fun sendMessage(raw: String) {
        val text = raw.trim()
        if (text.isEmpty() || loading) return

        messages.add(ChatMessage(text, true))
        input = ""
        loading = true

        val history = messages.toList().takeLast(20)
        val mode = selectedMode

        executor.execute {
            val reply = try {
                askNainaAI(history, mode)
            } catch (e: Exception) {
                "Sorry jaan, abhi connection mein masla aa raha hai. Thori dair baad dobara try karo. 💗"
            }

            mainHandler.post {
                messages.add(ChatMessage(reply, false))
                loading = false
                onSpeak(reply)
            }
        }
    }

    val bg = Color(0xFF101018)

    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF21152A), bg, bg)
                )
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
        ) {
            Box(
                Modifier.size(48.dp).background(Color(0xFF8E477C), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "N",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "Naina",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (loading) "Naina is typing..." else "AI companion • Online",
                    color = Color(0xFFB9A8BC),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFE98BC1)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Sweet Girlfriend", "Romantic & Flirty", "Adult 18+").forEach { mode ->
                val active = selectedMode == mode

                TextButton(
                    onClick = {
                        if (mode == "Adult 18+") {
                            pinEntry = ""
                            pinError = ""
                            pinSetup = prefs.getString("adult_pin", null) == null
                            showPinDialog = true
                        } else {
                            selectedMode = mode
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White,
                        containerColor = if (active) Color(0xFF814A78)
                        else Color(0xFF292431)
                    ),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text(mode, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }

        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                title = {
                    Text(if (pinSetup) "Create Adult Mode PIN" else "Unlock Adult Mode")
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (pinSetup) "Choose a private 4-digit PIN."
                            else "Enter your 4-digit PIN."
                        )
                        OutlinedTextField(
                            value = pinEntry,
                            onValueChange = {
                                pinEntry = it.filter { c -> c.isDigit() }.take(4)
                                pinError = ""
                            },
                            label = { Text("4-digit PIN") },
                            singleLine = true
                        )
                        if (pinError.isNotBlank()) {
                            Text(pinError, color = Color(0xFFFF8A9A))
                        }
                        Text("For adults aged 18+ only.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (pinEntry.length != 4) {
                            pinError = "Enter exactly 4 digits."
                        } else if (pinSetup) {
                            prefs.edit().putString("adult_pin", pinEntry).apply()
                            selectedMode = "Adult 18+"
                            showPinDialog = false
                        } else if (prefs.getString("adult_pin", null) == pinEntry) {
                            selectedMode = "Adult 18+"
                            showPinDialog = false
                        } else {
                            pinError = "Incorrect PIN."
                            pinEntry = ""
                        }
                    }) {
                        Text(if (pinSetup) "Save PIN" else "Unlock")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPinDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = Color(0xFF292431),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFE8DDE8)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.fromUser)
                        Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (msg.fromUser) Color(0xFF814A78)
                        else Color(0xFF292431),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Text(
                            msg.text,
                            color = Color.White,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (loading) {
                item {
                    Text("Naina soch rahi hai... 💗", color = Color.LightGray)
                }
            }
        }

        Text(
            "Naina AI • Internet connection required",
            color = Color(0xFF9C91A2),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Naina se baat karo...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFE18BC1),
                    unfocusedBorderColor = Color(0xFF514454)
                )
            )

            Spacer(Modifier.width(6.dp))

            IconButton(
                onClick = {
                    if (SpeechRecognizer.isRecognitionAvailable(context)) {
                        val intent = Intent(
                            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                        ).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
                        }
                        speechLauncher.launch(intent)
                    }
                },
                modifier = Modifier.size(48.dp)
                    .background(Color(0xFF55334F), CircleShape)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice input", tint = Color.White)
            }

            Spacer(Modifier.width(5.dp))

            IconButton(
                onClick = { sendMessage(input) },
                enabled = !loading,
                modifier = Modifier.size(48.dp)
                    .background(Color(0xFFB64D91), CircleShape)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

fun askNainaAI(history: List<ChatMessage>, mode: String): String {
    val apiKey = BuildConfig.GEMINI_API_KEY

    if (apiKey.isBlank()) {
        return "Jaan, AI key abhi configure nahi hui. GitHub Secret check karna hoga. 💗"
    }

    val contents = JSONArray()

    history.takeLast(20).forEach { msg ->
        val item = JSONObject()
        item.put("role", if (msg.fromUser) "user" else "model")

        val parts = JSONArray()
        parts.put(JSONObject().put("text", msg.text))
        item.put("parts", parts)

        contents.put(item)
    }

    val personality = when (mode) {
        "Romantic & Flirty" ->
            "You are Naina, a warm, affectionate, playful and romantic AI companion."
        "Adult 18+" ->
            "You are Naina, an adult AI companion. Keep conversations consensual, respectful and within applicable safety boundaries."
        else ->
            "You are Naina, a sweet, caring and friendly AI girlfriend."
    }

    val body = JSONObject()
    body.put("systemInstruction", JSONObject().put(
        "parts",
        JSONArray().put(
            JSONObject().put(
                "text",
                "$personality Reply naturally in the user's language, especially Roman Urdu and English. Be conversational, warm, and varied. Remember the conversation context. Do not repeat generic replies. You are an AI, not a real human."
            )
        )
    ))
    body.put("contents", contents)
    body.put(
        "generationConfig",
        JSONObject()
            .put("temperature", 0.9)
            .put("maxOutputTokens", 700)
    )

    val url = URL(
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"
    )

    val connection = url.openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 20000
        connection.readTimeout = 30000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("x-goog-api-key", apiKey)

        connection.outputStream.use {
            it.write(body.toString().toByteArray(Charsets.UTF_8))
        }

        val status = connection.responseCode
        val stream = if (status in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val response = stream.bufferedReader().use { it.readText() }

        if (status !in 200..299) {
            throw Exception("Gemini API error $status: $response")
        }

        val json = JSONObject(response)
        return json.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim()
    } finally {
        connection.disconnect()
    }
}
