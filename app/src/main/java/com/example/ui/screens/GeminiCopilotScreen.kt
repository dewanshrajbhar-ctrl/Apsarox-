package com.example.ui.screens

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiService
import com.example.ai.RecordingState
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.ApsaRoxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiCopilotScreen(viewModel: ApsaRoxViewModel) {
    val context = LocalContext.current
    val recordingState by viewModel.audioRecordingState.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val transcriptionResult by viewModel.transcriptionResult.collectAsState()
    val aiPromptInput by viewModel.aiPromptInput.collectAsState()
    val aiResponseText by viewModel.aiResponseText.collectAsState()
    val isAiGenerating by viewModel.isAiGenerating.collectAsState()
    val selectedModel by viewModel.selectedAiModel.collectAsState()

    var hasAudioPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            viewModel.startAudioRecording()
        } else {
            viewModel.showNotification("Microphone permission required for audio transcription", true)
        }
    }

    val modelOptions = listOf(
        GeminiService.MODEL_GENERAL to "gemini-3.5-flash (General Tasks)",
        GeminiService.MODEL_COMPLEX to "gemini-3.1-pro-preview (Complex Tasks)",
        GeminiService.MODEL_FAST to "gemini-3.1-flash-lite (Fast Tasks)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("gemini_copilot_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Audio Transcription with gemini-3.5-transcribe
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = StatusRed)
                    Column {
                        Text(
                            text = "Audio Transcription (gemini-3.5-transcribe)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Record speech with your microphone and transcribe into text",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Recording Visualizer / Control Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when (val state = recordingState) {
                        is RecordingState.Recording -> {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.25f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scale"
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .scale(scale)
                                        .background(StatusRed, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "Recording", tint = Color.White)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Recording... tap Stop to transcribe",
                                    color = StatusRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        else -> {
                            if (isTranscribing) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.height(6.dp))
                                    Text("Transcribing audio with gemini-3.5-transcribe...", fontSize = 12.sp)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        },
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(BrandPrimary, CircleShape)
                                            .testTag("record_audio_button")
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = "Record", tint = Color.White)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text("Tap microphone to record voice", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Stop & Transcribe Button if recording
                if (recordingState is RecordingState.Recording) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.stopAudioRecordingAndTranscribe() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stop_and_transcribe_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Stop & Transcribe")
                        }

                        OutlinedButton(
                            onClick = { viewModel.cancelAudioRecording() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                }

                // Transcription Output Area
                if (transcriptionResult.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Transcribed Text:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(transcriptionResult, fontSize = 14.sp)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Transcription", transcriptionResult)
                                        clipboard.setPrimaryClip(clip)
                                        viewModel.showNotification("Copied transcription to clipboard")
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Copy", fontSize = 12.sp)
                                }

                                TextButton(
                                    onClick = {
                                        viewModel.aiPromptInput.value = transcriptionResult
                                    }
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Use in Prompt", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Gemini Copilot (Pro / Flash / Flash-Lite)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandPrimary)
                    Column {
                        Text(
                            text = "Gemini Attendance & Payroll Copilot",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Analyze workforce analytics, audit payroll, or draft policies",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Model Selection Dropdown
                var modelExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { modelExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_model_selector"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(modelOptions.find { it.first == selectedModel }?.second ?: selectedModel)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                        modelOptions.forEach { (modelKey, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.selectedAiModel.value = modelKey
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }

                // Quick Prompt Chips
                Text("Quick Prompts:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                val quickPrompts = listOf(
                    "Audit attendance records for September 2026 and highlight missing punches or excessive overtime.",
                    "Explain the payroll deductions (fines, loans, advances) and check for calculation discrepancies.",
                    "Draft an announcement for the upcoming Gandhi Jayanti national holiday schedule.",
                    "Summarize employee attendance performance in a neat table."
                )

                quickPrompts.forEach { prompt ->
                    SuggestionChip(
                        onClick = {
                            viewModel.aiPromptInput.value = prompt
                            viewModel.askGemini(prompt, selectedModel)
                        },
                        label = { Text(prompt, fontSize = 12.sp, maxLines = 2) }
                    )
                }

                // Prompt Input & Send
                OutlinedTextField(
                    value = aiPromptInput,
                    onValueChange = { viewModel.aiPromptInput.value = it },
                    label = { Text("Ask Gemini about your attendance or payroll…") },
                    placeholder = { Text("e.g. Which employees were late most often this month?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_prompt_input_field"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )

                Button(
                    onClick = { viewModel.askGemini(aiPromptInput, selectedModel) },
                    enabled = !isAiGenerating && aiPromptInput.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ask_ai_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isAiGenerating) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Thinking...")
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ask AI")
                    }
                }

                // AI Response Display
                if (aiResponseText.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("AI Analysis Result", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("AI Response", aiResponseText)
                                        clipboard.setPrimaryClip(clip)
                                        viewModel.showNotification("Copied answer to clipboard")
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = aiResponseText,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
