package com.naina.ai

import android.Manifest
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

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
        setContent { NainaChat(onSpeak = { speak(it) }) }
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
    val messages = remember {
        mutableStateListOf(
            ChatMessage("Assalam-o-alaikum! Main Naina hoon 💗 Tumhara din kaisa guzar raha hai?", false)
        )
    }
    var input by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("Sweet Girlfriend") }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinEntry by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }
    var pinSetup by remember { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences("naina_settings", android.content.Context.MODE_PRIVATE) }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        listening = false
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) input = spoken
    }

    fun sendMessage(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        messages.add(ChatMessage(text, true))
        input = ""
        val reply = localReply(text, selectedMode)
        messages.add(ChatMessage(reply, false))
        onSpeak(reply)
    }

    val bg = Color(0xFF101018)
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF21152A), bg, bg)))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)) {
            Box(Modifier.size(48.dp).background(Color(0xFF8E477C), CircleShape), contentAlignment = Alignment.Center) {
                Text("N", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Naina", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("AI companion • Online", color = Color(0xFFB9A8BC), style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE98BC1))
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
                        } else selectedMode = mode
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (active) Color.White else Color(0xFFD7B8D0),
                        containerColor = if (active) Color(0xFF814A78) else Color(0xFF292431)
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text(mode, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }

        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false; pinEntry = "" },
                title = { Text(if (pinSetup) "Create Adult Mode PIN" else "Unlock Adult Mode") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (pinSetup) "Choose a private 4-digit PIN. You will need it to unlock Adult 18+ mode." else "Enter your 4-digit PIN to unlock Adult 18+ mode.")
                        OutlinedTextField(
                            value = pinEntry,
                            onValueChange = { value -> pinEntry = value.filter { it.isDigit() }.take(4); pinError = "" },
                            label = { Text("4-digit PIN") },
                            singleLine = true
                        )
                        if (pinError.isNotBlank()) Text(pinError, color = Color(0xFFFF8A9A))
                        Text("Adult mode is intended only for adults (18+).", style = MaterialTheme.typography.labelSmall)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (pinEntry.length != 4) {
                            pinError = "Please enter exactly 4 digits."
                        } else if (pinSetup) {
                            prefs.edit().putString("adult_pin", pinEntry).apply()
                            selectedMode = "Adult 18+"
                            showPinDialog = false
                            pinEntry = ""
                        } else if (prefs.getString("adult_pin", null) == pinEntry) {
                            selectedMode = "Adult 18+"
                            showPinDialog = false
                            pinEntry = ""
                        } else {
                            pinError = "Incorrect PIN. Try again."
                            pinEntry = ""
                        }
                    }) { Text(if (pinSetup) "Save PIN" else "Unlock") }
                },
                dismissButton = {
                    TextButton(onClick = { showPinDialog = false; pinEntry = "" }) { Text("Cancel") }
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
                    horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (msg.fromUser) Color(0xFF814A78) else Color(0xFF292431),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Text(msg.text, color = Color.White, modifier = Modifier.padding(14.dp))
                    }
                }
            }
        }

        Text(
            "Voice input uses your phone's speech recognition. Replies are currently demo responses.",
            color = Color(0xFF9C91A2), style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Naina se baat karo...") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFE18BC1), unfocusedBorderColor = Color(0xFF514454),
                    focusedPlaceholderColor = Color.LightGray, unfocusedPlaceholderColor = Color.LightGray
                ),
                maxLines = 3
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Naina se baat karein")
                    }
                    listening = true
                    speechLauncher.launch(intent)
                }
            }, modifier = Modifier.size(48.dp).background(Color(0xFF55334F), CircleShape)) {
                Icon(Icons.Default.Mic, contentDescription = "Voice input", tint = Color.White)
            }
            Spacer(Modifier.width(5.dp))
            IconButton(onClick = { sendMessage(input) },
                modifier = Modifier.size(48.dp).background(Color(0xFFB64D91), CircleShape)) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

fun localReply(message: String, mode: String): String {
    val m = message.lowercase()
    if (mode == "Adult 18+") {
        return when {
            listOf("hello", "hi", "salam", "assalam").any { m.contains(it) } ->
                "Hey you 😉 Aaj ka mood kaisa hai? We can keep our conversation playful, respectful, and between consenting adults."
            listOf("romantic", "kiss", "love", "miss").any { m.contains(it) } ->
                "Aww, you're making me blush 💗 Tell me what kind of romantic mood you're in tonight."
            else ->
                "I'm listening 😉 Share what's on your mind. Let's keep things consensual, respectful, and comfortable for both of us."
        }
    }
    if (mode == "Romantic & Flirty") {
        return when {
            listOf("hello", "hi", "salam", "assalam").any { m.contains(it) } ->
                "Hiii! 💕 I was hoping we'd talk. How's my favourite person doing?"
            listOf("miss", "yaad", "love").any { m.contains(it) } ->
                "Awww, that's sweet. Tumhari baaton mein kuch special hai ❤️"
            else -> "Hmm, tell me more jaan 😊 I'm all ears."
        }
    }
    return when {
        listOf("hello", "hi", "salam", "assalam").any { m.contains(it) } ->
            "Walaikum assalam! 😊 Aaj tumse baat karke acha lag raha hai. Kya chal raha hai?"
        listOf("sad", "udaas", "pareshan", "tension").any { m.contains(it) } ->
            "Oh no 💗 Kya hua? Agar tum batana chaho tou main sun rahi hoon."
        listOf("love", "miss", "yaad").any { m.contains(it) } ->
            "Aww, that's so sweet! ❤️ Batao, aaj ka din kaisa tha?"
        m.contains("name") || m.contains("naam") ->
            "Mera naam Naina hai! Aur tumhara din kaisa ja raha hai?"
        else ->
            "Achaa, mujhe aur batao 😊 Main sun rahi hoon. Tum is baare mein kaisa feel kar rahe ho?"
    }
}
