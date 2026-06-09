package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.InterviewMessage
import com.example.data.MockInterviewSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

// Vibrant accent colors for Voice-to-Text Experience
private val VoiceActivePrimary = Color(0xFFEF4444) // Energetic red for active recording
private val VoiceActiveSecondary = Color(0xFFFF8A80)
private val BlueAccent = Color(0xFF3B82F6)
private val GreenAccent = Color(0xFF10B981)

enum class SpeechSessionState {
    IDLE,
    LISTENING,
    SIMULATING,
    ERROR
}

/**
 * TextToSpeech helper wrapper that handles standard initialization, cleanup, and locale configuration
 */
@Composable
fun rememberTextToSpeech(onUtteranceCompleted: (() -> Unit)? = null): Pair<TextToSpeech?, (String) -> Unit> {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isInitialized by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    LaunchedEffect(tts, isInitialized) {
        if (isInitialized && tts != null) {
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onUtteranceCompleted?.invoke()
                    }
                }
                @Deprecated("Deprecated in Java", ReplaceWith("onDone"))
                override fun onError(utteranceId: String?) {}
            })
        }
    }

    val speak: (String) -> Unit = { text ->
        if (isInitialized) {
            tts?.setLanguage(Locale.US)
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "interview_question")
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "interview_question")
        }
    }

    return Pair(tts, speak)
}

@Composable
fun InterviewCoachSection(
    profile: com.example.data.ResumeProfile?,
    activeSession: MockInterviewSession?,
    messages: List<InterviewMessage>,
    isLoading: Boolean,
    errorState: EngineState,
    onStartInterview: (String) -> Unit,
    onSubmitAnswer: (String, String?) -> Unit,
    onQuit: () -> Unit,
    onNavigateToAts: () -> Unit = {},
    onSaveSummary: (() -> Unit)? = null,
    cloudSummaries: List<com.example.data.CloudInterviewSummary> = emptyList(),
    onDeleteCloudSummary: ((String, String?) -> Unit)? = null,
    firestoreSyncStatus: EngineState = EngineState.Idle,
    localSessions: List<com.example.data.MockInterviewSession> = emptyList(),
    onSelectLocalSession: ((com.example.data.MockInterviewSession) -> Unit)? = null,
    onDeleteLocalSession: ((com.example.data.MockInterviewSession) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isHandsFreeModeEnabled by remember { mutableStateOf(false) }
    var autoTriggerSpeechSessionKey by remember { mutableStateOf(0) }

    val (_, speakText) = rememberTextToSpeech(
        onUtteranceCompleted = {
            if (isHandsFreeModeEnabled) {
                autoTriggerSpeechSessionKey += 1
            }
        }
    )

    // Preferences
    var isAutoTtsEnabled by remember { mutableStateOf(false) }

    // Timer States
    var selectedTimeLimitSeconds by remember { mutableStateOf(90) } // Default is 90 seconds
    var timeLeftSeconds by remember { mutableStateOf(90) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Start, reset, and stop timer rules
    LaunchedEffect(messages.size, isLoading, activeSession) {
        if (activeSession != null) {
            val lastMessage = messages.lastOrNull()
            if (lastMessage != null && lastMessage.sender == "AI" && !isLoading) {
                // New question arrived, reset and start countdown!
                if (selectedTimeLimitSeconds > 0) {
                    timeLeftSeconds = selectedTimeLimitSeconds
                    isTimerRunning = true
                } else {
                    isTimerRunning = false
                }
            } else {
                // When user submits an answer or we are loading
                isTimerRunning = false
            }
        } else {
            isTimerRunning = false
        }
    }

    // Countdown ticking effect
    LaunchedEffect(isTimerRunning, timeLeftSeconds) {
        if (isTimerRunning && timeLeftSeconds > 0) {
            delay(1000L)
            timeLeftSeconds = timeLeftSeconds - 1
        } else if (isTimerRunning && timeLeftSeconds == 0) {
            isTimerRunning = false
            // Auto submit answer when timer runs out
            onSubmitAnswer("[Time Limit Expired] Candidate ran out of time while formulating response.", null)
        }
    }

    // If there is a new AI question and TTS is enabled, auto-read it
    LaunchedEffect(messages.size) {
        val lastMessage = messages.lastOrNull()
        if (lastMessage != null && lastMessage.sender == "AI" && isAutoTtsEnabled) {
            // Give a short delay for comfort
            delay(400)
            speakText(lastMessage.messageText)
        }
    }

    // Hands-Free loop automation fallback (when TTS is disabled but hands-free is active)
    LaunchedEffect(messages.size, isLoading) {
        val lastMessage = messages.lastOrNull()
        if (lastMessage != null && lastMessage.sender == "AI" && !isLoading) {
            if (isHandsFreeModeEnabled && !isAutoTtsEnabled) {
                // Turn on speech recognizer automatically after 800ms comfort delay
                delay(800)
                autoTriggerSpeechSessionKey += 1
            }
        }
    }

    if (profile == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked Feature",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "Mock Interview Coach Locked",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "To access customized interview simulations, you must first complete your profile setup so our dual AI agents can formulate relevant industry questions.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val requirements = listOf(
                        "1. Upload or paste your Resume on Tab 0",
                        "2. Provide your target Job Title & Description",
                        "3. Find ATS Score and core Skill Gap indices"
                    )
                    requirements.forEach { req ->
                        Text(
                            text = req,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onNavigateToAts,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("navigate_to_ats_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Go to ATS Scanner & Setup",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            }
        }
    } else {
        if (activeSession == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Pre-launch layout, mode selection with responsive touch targets and dynamic layout
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("interview_coach_start_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BlueAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = BlueAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "AI Speech Mock Interview Coach",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Practice face-to-face style dialogue using active Speech-to-Text! Let the Gemini Model evaluate your speed, key skills intersection, and structural depth.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )

                        // Timer Selection
                        Text(
                            text = "Response Time Limit Option:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val timeOptions = listOf(
                                Pair(30, "30s"),
                                Pair(60, "60s"),
                                Pair(90, "90s"),
                                Pair(120, "2m"),
                                Pair(0, "Infinite")
                            )
                            timeOptions.forEach { option ->
                                val isSelected = selectedTimeLimitSeconds == option.first
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedTimeLimitSeconds = option.first }
                                        .padding(vertical = 10.dp)
                                        .testTag("time_limit_option_${option.second}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option.second,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Select Interview Context Theme:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                InterviewModeSelectionItem(
                                    title = "Technical Round",
                                    description = "LeetCode/System Architecture",
                                    icon = Icons.Default.Build,
                                    modifier = Modifier.weight(1f).testTag("coach_start_technical"),
                                    onClick = { onStartInterview("TECHNICAL") }
                                )
                                InterviewModeSelectionItem(
                                    title = "Behavioral",
                                    description = "STAR method scenarios",
                                    icon = Icons.Default.Notifications,
                                    modifier = Modifier.weight(1f).testTag("coach_start_behavioral"),
                                    onClick = { onStartInterview("BEHAVIORAL") }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                InterviewModeSelectionItem(
                                    title = "HR Specialty",
                                    description = "Cultural alignment & background",
                                    icon = Icons.Default.Person,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onStartInterview("HR") }
                                )
                                InterviewModeSelectionItem(
                                    title = "Corporate Elite",
                                    description = "FAANG & scale alignment",
                                    icon = Icons.Default.Share,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onStartInterview("COMPANY_SPECIFIC") }
                                )
                            }
                        }
                    }
                }

                // --- Cloud Interview Summaries Progress Dashboard ---
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Cloud Interview Tracker (Firestore)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (cloudSummaries.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            Text(
                                text = "No cloud interview summaries synced yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "When starting a mock interview, speak answers and click the 'Sync Cloud' button in the toolbar to save performance metrics and track your career growth progress over time!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val averageScore = cloudSummaries.map { it.averageScore }.average()
                    val totalEvaluations = cloudSummaries.size

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Avg. Evaluation Score", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = String.format("%.1f", averageScore),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text("/10", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Rounds Synced", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$totalEvaluations rounds",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    cloudSummaries.forEach { summary ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = summary.jobRole,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "Rounds Type: ${summary.interviewType} • ${summary.totalQuestions} Questions",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (summary.averageScore >= 7.5) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                            ),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = String.format("%.1f/10", summary.averageScore),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (summary.averageScore >= 7.5) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                                            )
                                        }

                                        if (onDeleteCloudSummary != null) {
                                            IconButton(
                                                onClick = { onDeleteCloudSummary(summary.id, summary.docPath) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete cloud log",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "Evaluation Strengths & Coaching:\n${summary.feedbackHighlights}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    lineHeight = 15.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                val summaryScores = remember(summary) { calculatePerformanceScoresFromSummary(summary) }
                                PerformanceRadarOrBarChart(scores = summaryScores)
                            }
                        }
                    }
                }

                // --- Local Interview Transcripts & Playbacks Archive ---
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Local Performance Archive & Playbacks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (localSessions.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            Text(
                                text = "No local interview history found.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Conduct simulations above to record your voice and text responses locally for deep appraisal later!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    localSessions.forEach { session ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectLocalSession?.invoke(session) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${session.interviewType} Interview",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black
                                        )
                                        val formattedDateStr = java.text.SimpleDateFormat(
                                            "yyyy-MM-dd HH:mm",
                                            java.util.Locale.getDefault()
                                        ).format(java.util.Date(session.timestamp))
                                        Text(
                                            text = "Conducted: $formattedDateStr",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = { onSelectLocalSession?.invoke(session) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Review transcript and play recordings",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        if (onDeleteLocalSession != null) {
                                            IconButton(
                                                onClick = { onDeleteLocalSession(session) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete local log",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Area with Settings & Quit trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = BlueAccent,
                            modifier = Modifier
                                .size(30.dp)
                                .background(BlueAccent.copy(alpha = 0.08f), CircleShape)
                                .padding(6.dp)
                        )
                        Column {
                            Text(
                                text = "Screening Mode: ${activeSession.interviewType}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = GreenAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Active Speech-to-Text Enabled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GreenAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Speak automatically toggle + Quit Round Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { isAutoTtsEnabled = !isAutoTtsEnabled },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isAutoTtsEnabled) Icons.Default.Call else Icons.Default.Delete,
                                contentDescription = "Toggle TTS",
                                tint = if (isAutoTtsEnabled) BlueAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (onSaveSummary != null && messages.any { it.sender == "USER" && it.evaluationScore != null }) {
                            Button(
                                onClick = onSaveSummary,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("coach_sync_cloud")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Cloud", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onQuit,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("coach_quit_round")
                        ) {
                            Text("End Session", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Countdown Timer Section
                if (selectedTimeLimitSeconds > 0) {
                    val progressFraction = timeLeftSeconds.toFloat() / selectedTimeLimitSeconds.toFloat()
                    val animatedProgress by animateFloatAsState(
                        targetValue = progressFraction.coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
                    )

                    val isDangerState = timeLeftSeconds <= 15
                    val timerColor = when {
                        isDangerState -> VoiceActivePrimary
                        timeLeftSeconds <= 30 -> Color(0xFFF59E0B)
                        else -> GreenAccent
                    }

                    val scaleFactor by animateFloatAsState(
                        targetValue = if (isDangerState && isTimerRunning) 1.03f else 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(scaleX = scaleFactor, scaleY = scaleFactor)
                            .testTag("interview_timer_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = timerColor.copy(alpha = 0.05f)
                        ),
                        border = BorderStroke(1.dp, timerColor.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDangerState) Icons.Default.Warning else Icons.Default.Info,
                                        contentDescription = "Timer Icon",
                                        tint = timerColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isDangerState) "CRITICAL TIME LIMIT" else "Response Countdown",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = timerColor
                                    )
                                }

                                val minutes = timeLeftSeconds / 60
                                val seconds = timeLeftSeconds % 60
                                val timeFormattedString = String.format("%02d:%02d", minutes, seconds)

                                Text(
                                    text = timeFormattedString,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = timerColor,
                                    modifier = Modifier.testTag("formatted_timer_text")
                                )
                            }

                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .testTag("timer_progress_bar"),
                                color = timerColor,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )

                            if (isDangerState) {
                                Text(
                                    text = "Speak or enter your response before time expires to avoid penalty!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VoiceActivePrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("interview_unlimited_time_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Infinite Time",
                                tint = GreenAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Unlimited Response Time Mode is Active.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Dynamic Live Performance Analytical Dashboard
                if (messages.any { it.sender == "USER" && it.evaluationScore != null }) {
                    val activeSessionScores = remember(messages) { calculatePerformanceScores(messages) }
                    var isAnalyticsExpanded by remember { mutableStateOf(true) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isAnalyticsExpanded = !isAnalyticsExpanded }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Active Performance Appraisal",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = if (isAnalyticsExpanded) "Collapse [x]" else "Expand [o]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            if (isAnalyticsExpanded) {
                                Box(modifier = Modifier.padding(bottom = 12.dp, start = 12.dp, end = 12.dp)) {
                                    PerformanceRadarOrBarChart(scores = activeSessionScores)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Chat Message history list
                val chatListState = rememberLazyListState()
                LaunchedEffect(messages.size) {
                    if (messages.isNotEmpty()) {
                        chatListState.animateScrollToItem(messages.size - 1)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        state = chatListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("coach_chat_history"),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(messages) { msg ->
                            ExtendedChatBubbleWithAudio(
                                message = msg,
                                onSpeakAloud = { speakText(msg.messageText) }
                            )
                        }
                    }
                }

                // State indicator for loading
                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = BlueAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Interviewer compiling coaching feedback...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Advanced Vocal Input Panel
                VoiceInputPanel(
                    lastInterviewerQuestion = messages.lastOrNull { msg -> msg.sender == "AI" }?.messageText ?: "",
                    onSubmitAnswer = onSubmitAnswer,
                    isLoading = isLoading,
                    isHandsFreeModeEnabled = isHandsFreeModeEnabled,
                    onHandsFreeModeToggle = { enabled ->
                        isHandsFreeModeEnabled = enabled
                        if (enabled) {
                            isAutoTtsEnabled = true
                        }
                    },
                    autoTriggerSpeechSessionKey = autoTriggerSpeechSessionKey
                )
            }
        }
    }
}

@Composable
fun InterviewModeSelectionItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AudioPlayerWidget(audioPath: String) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayerRef by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var playbackProgress by remember { mutableStateOf(0f) }
    var playbackDuration by remember { mutableStateOf(1) } // avoid divide by zero
    var playbackPosition by remember { mutableStateOf(0) }

    // Dispose player when leaving Composition
    DisposableEffect(audioPath) {
        onDispose {
            mediaPlayerRef?.stop()
            mediaPlayerRef?.release()
            mediaPlayerRef = null
        }
    }

    // Dynamic playback progress update loop
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                val mp = mediaPlayerRef
                if (mp != null && mp.isPlaying) {
                    playbackPosition = mp.currentPosition
                    playbackProgress = if (playbackDuration > 0) {
                        mp.currentPosition.toFloat() / playbackDuration.toFloat()
                    } else {
                        0f
                    }
                } else if (mp != null && !mp.isPlaying) {
                    isPlaying = false
                }
                delay(100)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = {
                try {
                    val file = java.io.File(audioPath)
                    if (!file.exists()) {
                        android.widget.Toast.makeText(context, "Audio file not found on disk.", android.widget.Toast.LENGTH_SHORT).show()
                        return@IconButton
                    }
                    if (isPlaying) {
                        mediaPlayerRef?.pause()
                        isPlaying = false
                    } else {
                        if (mediaPlayerRef == null) {
                            val mp = android.media.MediaPlayer().apply {
                                setDataSource(audioPath)
                                prepare()
                            }
                            playbackDuration = mp.duration
                            mediaPlayerRef = mp
                            mp.setOnCompletionListener {
                                isPlaying = false
                                playbackPosition = 0
                                playbackProgress = 0f
                            }
                        }
                        mediaPlayerRef?.start()
                        isPlaying = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            modifier = Modifier
                .size(48.dp)
                .background(Color.White, CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause response playback" else "Play response audio",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Recorded Voice Response",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            LinearProgressIndicator(
                progress = { playbackProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }

        val curSec = (playbackPosition / 1000) % 60
        val durSec = if (playbackDuration > 0) (playbackDuration / 1000) % 60 else 0
        Text(
            text = String.format("%02d:%02d", 0, curSec) + " / " + String.format("%02d:%02d", 0, durSec),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ExtendedChatBubbleWithAudio(
    message: InterviewMessage,
    onSpeakAloud: () -> Unit
) {
    val isAI = message.sender == "AI"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = if (isAI) Alignment.Start else Alignment.End
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            if (isAI) {
                // AI bubble with built-in TTS button to hear questions recited aloud
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .weight(1f, fill = false)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = message.messageText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clickable { onSpeakAloud() }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = "Listen",
                                tint = BlueAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Listen Question",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BlueAccent
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .weight(1f, fill = false)
                ) {
                    Column {
                        Text(
                            text = message.messageText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        if (message.audioFilePath != null) {
                            AudioPlayerWidget(audioPath = message.audioFilePath)
                        }
                    }
                }
            }
        }

        // Live grading scorecard immediately attached below user bubble
        if (!isAI && message.evaluationScore != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .padding(top = 2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recruiter Evaluator Assessment",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (message.evaluationScore >= 8) GreenAccent.copy(alpha = 0.12f)
                                    else if (message.evaluationScore >= 6) BlueAccent.copy(alpha = 0.12f)
                                    else VoiceActivePrimary.copy(alpha = 0.12f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${message.evaluationScore}/10 Score",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (message.evaluationScore >= 8) GreenAccent
                                        else if (message.evaluationScore >= 6) BlueAccent
                                        else VoiceActivePrimary
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                    Text(
                        text = message.evaluationFeedback ?: "Analyzing details and coherence factors...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Advanced real-time voice capture panel overlay with animated high-fidelity canvas wave lines
 */
@Composable
fun VoiceInputPanel(
    lastInterviewerQuestion: String,
    onSubmitAnswer: (String, String?) -> Unit,
    isLoading: Boolean,
    isHandsFreeModeEnabled: Boolean,
    onHandsFreeModeToggle: (Boolean) -> Unit,
    autoTriggerSpeechSessionKey: Int
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val textSoftwareKeyboardController = LocalSoftwareKeyboardController.current

    // Speech states
    var uiState by remember { mutableStateOf(SpeechSessionState.IDLE) }
    var currentSpeechResultText by remember { mutableStateOf("") }
    var micRmsDbVolumeValue by remember { mutableStateOf(0f) }
    var displayErrorMessage by remember { mutableStateOf("") }

    // Manual transcription typing option (as fallback if recording isn't convenient)
    var typedManualResponseText by remember { mutableStateOf("") }
    var isKeyboardEntryModeActive by remember { mutableStateOf(false) }

    // Auto submit countdown state
    var autoSubmitCountdown by remember { mutableStateOf(-1) }

    // Media recorder and files references
    var mediaRecorderInstance by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    var recordedAudioFileRef by remember { mutableStateOf<java.io.File?>(null) }

    val stopAudioRecording: () -> Unit = {
        try {
            mediaRecorderInstance?.stop()
            mediaRecorderInstance?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorderInstance = null
    }

    val startAudioRecording: () -> Unit = {
        try {
            stopAudioRecording()
            val audioFile = java.io.File(context.cacheDir, "recording_${System.currentTimeMillis()}.3gp")
            recordedAudioFileRef = audioFile
            
            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.media.MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }
            
            recorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.THREE_GPP)
            recorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AMR_NB)
            recorder.setOutputFile(audioFile.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorderInstance = recorder
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Setup speech recognizer instances
    val speechEngineInstance = remember {
        try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            null
        }
    }

    // Standard Android permission launcher
    val launchAudioPermissionCheck = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            displayErrorMessage = "Audio recording permission is required. Launching simulation mode instead."
            uiState = SpeechSessionState.ERROR
        }
    }

    // Precompiled contextually intelligent paragraphs corresponding to common tech/behavioral answers
    // to stream beautifully letter-by-letter to replicate extremely smart spoken speech!
    val mockIntelligentAnswers = listOf(
        "To optimize rendering in Jetpack Compose, we must enforce smart key-indexing inside our LazyColumn implementations. Doing so mitigates redundant recompositions and bounds drawing computations directly. Additionally, placing heavy computation and database accesses inside Dispatchers.IO guarantees a high performance sixty-frame interface without skipping steps.",
        "Specifically, I lead database design using room migrations together with clean architecture. In my previous team, we migrated from legacy JSON endpoints to an offline-first repository pattern, leveraging SQLite query compiling. This refined our synchronization pipeline, cut network cellular payloads by thirty percent, and secured zero data losses.",
        "Regarding conflict resolution, I adopt a collaborative post-mortem technique. By isolating logical bottlenecks instead of attributing human blame, we can conduct rapid code reviews, define automated linters, and pair program when resolving critical multi-tenant database locking states, establishing a highly aligned engineering team."
    )

    // Function to initiate simulation stream
    val startSpeechTypingSimulation: () -> Unit = {
        autoSubmitCountdown = -1
        uiState = SpeechSessionState.SIMULATING
        currentSpeechResultText = ""
        startAudioRecording()
        coroutineScope.launch {
            val targetedMockResponse = when {
                lastInterviewerQuestion.contains("Compose", ignoreCase = true) || lastInterviewerQuestion.contains("Optimize", ignoreCase = true) || lastInterviewerQuestion.contains("Technical", ignoreCase = true) -> mockIntelligentAnswers[0]
                lastInterviewerQuestion.contains("Database", ignoreCase = true) || lastInterviewerQuestion.contains("Experience", ignoreCase = true) || lastInterviewerQuestion.contains("Project", ignoreCase = true) -> mockIntelligentAnswers[1]
                else -> mockIntelligentAnswers[2]
            }

            val responseWordsList = targetedMockResponse.split(" ")
            for (idx in responseWordsList.indices) {
                if (uiState != SpeechSessionState.SIMULATING) break
                val currentAggregatedText = responseWordsList.take(idx + 1).joinToString(" ")
                currentSpeechResultText = currentAggregatedText
                // Generate a lovely fluttering DB value to drive the pulsing simulator wave
                micRmsDbVolumeValue = (5f + Math.sin(idx.toDouble() * 0.9).toFloat() * 12f).coerceIn(2f, 20f)
                delay(220) // Natural typing/speaking cadence
            }
            if (uiState == SpeechSessionState.SIMULATING) {
                micRmsDbVolumeValue = 0f
                uiState = SpeechSessionState.IDLE
                if (isHandsFreeModeEnabled && currentSpeechResultText.isNotBlank()) {
                    autoSubmitCountdown = 3
                }
            }
        }
    }

    // Main controller to activate live microphone speech recognizer
    val launchActiveSpeakInputSession: () -> Unit = {
        autoSubmitCountdown = -1
        val permissionResult = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionResult != PackageManager.PERMISSION_GRANTED) {
            launchAudioPermissionCheck.launch(Manifest.permission.RECORD_AUDIO)
        } else if (speechEngineInstance == null || !SpeechRecognizer.isRecognitionAvailable(context)) {
            // Fallback gracefully to typing simulation if recognition system services aren't registered
            startSpeechTypingSimulation()
        } else {
            try {
                uiState = SpeechSessionState.LISTENING
                currentSpeechResultText = "Listening... start speaking clearly."
                startAudioRecording()

                val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                speechEngineInstance.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        currentSpeechResultText = "Connected! Speak now..."
                    }
                    override fun onBeginningOfSpeech() {
                        currentSpeechResultText = "Speech recognized..."
                    }
                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize RMS decibels (usually -2 to 12) for visual pulsing scale
                        micRmsDbVolumeValue = rmsdB.coerceAtLeast(0f)
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        // Codes: 1-9. Gracefully guide to simulated fallback if speech encounters errors
                        val errorReason = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out."
                            else -> "Mic blocked or unavailable."
                        }
                        displayErrorMessage = "$errorReason Press below to simulate speaking instead."
                        uiState = SpeechSessionState.ERROR
                        micRmsDbVolumeValue = 0f
                        stopAudioRecording()
                    }
                    override fun onResults(results: Bundle?) {
                        val matchesFound = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matchesFound.isNullOrEmpty()) {
                            val recognizedText = matchesFound[0]
                            currentSpeechResultText = recognizedText
                            if (isHandsFreeModeEnabled && recognizedText.isNotBlank() && recognizedText != "Listening... start speaking clearly." && recognizedText != "Connected! Speak now...") {
                                autoSubmitCountdown = 3
                            }
                        }
                        uiState = SpeechSessionState.IDLE
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val partialsList = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!partialsList.isNullOrEmpty()) {
                            currentSpeechResultText = partialsList[0]
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                speechEngineInstance.startListening(recognizerIntent)
            } catch (e: Exception) {
                startSpeechTypingSimulation()
            }
        }
    }

    // Stop speaking stream
    val terminatesActiveSpeechSession: () -> Unit = {
        autoSubmitCountdown = -1
        if (uiState == SpeechSessionState.LISTENING) {
            try {
                speechEngineInstance?.stopListening()
            } catch (e: Exception) {}
        }
        stopAudioRecording()
        recordedAudioFileRef = null
        uiState = SpeechSessionState.IDLE
        micRmsDbVolumeValue = 0f
    }

    // Hands-Free Automations
    LaunchedEffect(autoTriggerSpeechSessionKey) {
        if (autoTriggerSpeechSessionKey > 0 && isHandsFreeModeEnabled) {
            launchActiveSpeakInputSession()
        }
    }

    LaunchedEffect(autoSubmitCountdown) {
        if (autoSubmitCountdown > 0) {
            delay(1000L)
            autoSubmitCountdown -= 1
        } else if (autoSubmitCountdown == 0) {
            if (currentSpeechResultText.isNotBlank() && currentSpeechResultText != "Listening... start speaking clearly." && currentSpeechResultText != "Connected! Speak now...") {
                stopAudioRecording()
                onSubmitAnswer(currentSpeechResultText, recordedAudioFileRef?.absolutePath)
                currentSpeechResultText = ""
                uiState = SpeechSessionState.IDLE
            }
            autoSubmitCountdown = -1
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechEngineInstance?.destroy()
            } catch (e: Exception) {}
            stopAudioRecording()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("coach_voice_input_panel"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isKeyboardEntryModeActive) {
                // TEXT KEYBOARD INPUT WORKSPACE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = { isKeyboardEntryModeActive = false },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Switch to Mic mode",
                            tint = BlueAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    OutlinedTextField(
                        value = typedManualResponseText,
                        onValueChange = { typedManualResponseText = it },
                        placeholder = { Text("Compose candidate answer...", style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("coach_typed_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlueAccent
                        )
                    )

                    IconButton(
                        onClick = {
                            if (typedManualResponseText.isNotBlank()) {
                                onSubmitAnswer(typedManualResponseText, null)
                                typedManualResponseText = ""
                                textSoftwareKeyboardController?.hide()
                            }
                        },
                        enabled = typedManualResponseText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (typedManualResponseText.isNotBlank() && !isLoading) BlueAccent
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            .testTag("coach_submit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Submit Answer",
                            tint = if (typedManualResponseText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                // ADVANCED M3 VOICE SPEECH PANEL
                if (uiState == SpeechSessionState.LISTENING || uiState == SpeechSessionState.SIMULATING || (uiState == SpeechSessionState.IDLE && currentSpeechResultText.isNotBlank())) {
                    // Pulsing Wave Live Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(VoiceActivePrimary.copy(alpha = 0.04f))
                            .border(1.dp, VoiceActivePrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (uiState == SpeechSessionState.LISTENING) "CAPTURING VOCAL SPEECH" else if (uiState == SpeechSessionState.SIMULATING) "SIMULATING REAL-TIME SPEECH STREAM" else "VOCAL SPEECH TRANSCRIBED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = VoiceActivePrimary,
                                textAlign = TextAlign.Center
                            )

                            // Continuous sound wave bars generated live based on RMS output volume
                            if (uiState == SpeechSessionState.LISTENING || uiState == SpeechSessionState.SIMULATING) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val pulseAmt1 by animateFloatAsState(
                                        targetValue = (0.3f + micRmsDbVolumeValue * 0.15f).coerceIn(0.2f, 2.5f),
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                    )
                                    val pulseAmt2 by animateFloatAsState(
                                        targetValue = (0.2f + micRmsDbVolumeValue * 0.18f).coerceIn(0.1f, 2.2f),
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
                                    )
                                    val pulseAmt3 by animateFloatAsState(
                                        targetValue = (0.4f + micRmsDbVolumeValue * 0.08f).coerceIn(0.15f, 1.8f),
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))
                                    SoundWaveBar(heightScale = pulseAmt2, modifier = Modifier.testTag("soundwave_bar_1"))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    SoundWaveBar(heightScale = pulseAmt1, modifier = Modifier.testTag("soundwave_bar_2"))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    SoundWaveBar(heightScale = pulseAmt3, modifier = Modifier.testTag("soundwave_bar_3"))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    SoundWaveBar(heightScale = pulseAmt2, modifier = Modifier.testTag("soundwave_bar_4"))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = currentSpeechResultText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Start,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth().testTag("speech_transcription_text")
                                )
                            }

                            // Dynamic auto-sending alert progress
                            if (autoSubmitCountdown >= 0) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().animateContentSize().testTag("auto_submit_countdown_card"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            CircularProgressIndicator(
                                                progress = { autoSubmitCountdown.toFloat() / 3f },
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.5.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Auto-sending in $autoSubmitCountdown s...",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        TextButton(
                                            onClick = { autoSubmitCountdown = -1 },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Pause & Edit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = terminatesActiveSpeechSession,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1F), contentColor = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.weight(1f).testTag("stop_voice_record_btn"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Discard", style = MaterialTheme.typography.labelMedium)
                                }
                                Button(
                                    onClick = {
                                        if (currentSpeechResultText.isNotBlank() && currentSpeechResultText != "Listening... start speaking clearly." && currentSpeechResultText != "Connected! Speak now...") {
                                            stopAudioRecording()
                                            onSubmitAnswer(currentSpeechResultText, recordedAudioFileRef?.absolutePath)
                                            currentSpeechResultText = ""
                                            uiState = SpeechSessionState.IDLE
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VoiceActivePrimary, contentColor = Color.White),
                                    modifier = Modifier.weight(1f).testTag("transmit_voice_button"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Send Transcribed Speech", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                } else {
                    // IDLE OR ERROR STATUS PANEL - Controls to launch Voice session
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState == SpeechSessionState.ERROR) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(VoiceActivePrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Warning,
                                    contentDescription = "Warning",
                                    tint = VoiceActivePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = displayErrorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VoiceActivePrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = BlueAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Ready to record",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Premium Hands-Free Toggle Selector
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isHandsFreeModeEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            val nextState = !isHandsFreeModeEnabled
                                            if (nextState) {
                                                val permissionResult = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                                if (permissionResult != PackageManager.PERMISSION_GRANTED) {
                                                    launchAudioPermissionCheck.launch(Manifest.permission.RECORD_AUDIO)
                                                } else {
                                                    onHandsFreeModeToggle(true)
                                                }
                                            } else {
                                                onHandsFreeModeToggle(false)
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("hands_free_mode_toggle")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = "Hands-Free Mode Enabled",
                                        tint = if (isHandsFreeModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Hands-Free",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isHandsFreeModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Switch(
                                        checked = isHandsFreeModeEnabled,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                val permissionResult = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                                if (permissionResult != PackageManager.PERMISSION_GRANTED) {
                                                    launchAudioPermissionCheck.launch(Manifest.permission.RECORD_AUDIO)
                                                } else {
                                                    onHandsFreeModeToggle(true)
                                                }
                                            } else {
                                                onHandsFreeModeToggle(false)
                                            }
                                        },
                                        modifier = Modifier.scale(0.6f).testTag("hands_free_switch")
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Quick swap to keyboard type button
                                TextButton(
                                    onClick = { isKeyboardEntryModeActive = true },
                                    modifier = Modifier.testTag("swap_text_entry_button"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Use text", style = MaterialTheme.typography.labelSmall, color = BlueAccent)
                                }
                            }
                        }

                        // Big Mic Activation Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Simulator trigger (always reliable, types responses letter-by-letter)
                            Button(
                                onClick = startSpeechTypingSimulation,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("mimic_speaking_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BlueAccent.copy(alpha = 0.1f),
                                    contentColor = BlueAccent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Simulate Speak",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simulate Speak", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                            }

                            // Active Microphone Button (uses native android SpeechRecognizer API)
                            Button(
                                onClick = launchActiveSpeakInputSession,
                                modifier = Modifier.weight(1.5f).height(48.dp).testTag("start_voice_speak_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VoiceActivePrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Microphone Ready",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Speak Answer (Live Mic)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SoundWaveBar(
    heightScale: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(4.dp)
            .fillMaxHeight(0.85f)
            .scale(scaleX = 1f, scaleY = heightScale)
            .clip(RoundedCornerShape(2.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(VoiceActivePrimary, VoiceActiveSecondary)
                )
            )
    )
}
