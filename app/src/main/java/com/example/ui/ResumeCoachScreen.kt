package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.InterviewMessage
import com.example.data.ResumeProfile
import com.example.ui.theme.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.InputStream
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor

suspend fun extractTextFromPdfUri(context: android.content.Context, uri: Uri): String = withContext(Dispatchers.IO) {
    var reader: PdfReader? = null
    var inputStream: InputStream? = null
    try {
        inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) return@withContext ""
        reader = PdfReader(inputStream)
        val sb = java.lang.StringBuilder()
        val pages = reader.numberOfPages
        for (i in 1..pages) {
            val pageText = PdfTextExtractor.getTextFromPage(reader, i)
            if (pageText != null) {
                sb.append(pageText).append("\n")
            }
        }
        sb.toString()
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    } finally {
        try {
            reader?.close()
        } catch (ignored: Exception) {}
        try {
            inputStream?.close()
        } catch (ignored: Exception) {}
    }
}

// Pre-packaged high-fidelity demo profiles for instant evaluation & recruiter review!
val SampleProfiles = listOf(
    Pair(
        "Senior Software Engineer (Android/Kotlin)",
        """
        PRANAV TIRODKAR
        Email: pranav@example.com | Phone: +1-555-0192
        
        EXPERIENCE:
        - Android Developer, TechCorp (2022 - Present)
          Built visual analytics dashboards using Kotlin and Jetpack Compose. Reduced startup latency by 20%.
        - Software Engineer Intern, StartupInc (2021)
          Maintained legacy Java Android apps and migrated network operations to Retrofit.
        
        SKILLS:
        Kotlin, Java, Jetpack Compose, Retrofit, Git, SQLite, Unit Testing, Coroutines.
        
        EDUCATION:
        BS Computer Science, State University, GPA: 3.8/4.0
        
        CERTIFICATIONS:
        Associate Android Developer
        """.trimIndent()
    ),
    Pair(
        "Full-Stack Web Dev (React & Node)",
        """
        ANANYA SHARMA
        Email: ananya@example.com | Web: ananya.dev
        
        SUMMARY:
        Ambitious web developer with 3 years of hands-on experience building SaaS platforms.
        
        EXPERIENCE:
        - Full-Stack Engineer, WebSolutions (2023 - Present)
          Authored React frontend dashboard. Integrated secure payment processing using Stripe API.
          Designed backend APIs in Node.js and Express.
        - Frontend Intern, PixelPerfect (2022)
          Translated Figma files into high performance HTML & CSS.
        
        SKILLS:
        JavaScript, React, Tailwind CSS, HTML5, CSS3, Express, MongoDB, Git, Agile.
        
        EDUCATION:
        Bachelor of Engineering (IT), Tech Institute (2020 - 2024)
        """.trimIndent()
    ),
    Pair(
        "Cloud DevOps Engineer",
        """
        ROHIT KUMAR
        Email: rohit.devops@example.com
        
        SUMMARY:
        Automating server infrastructure and establishing zero-downtime CI/CD deployment pipelines.
        
        EXPERIENCE:
        - Platform Infrastructure Analyst, CloudSky (2022 - Present)
          Wrote Ansible roles that automated provisioning of 200+ AWS EC2 virtual machines.
          Authored deployment charts using Docker and Kubernetes.
        
        SKILLS:
        AWS (EC2, S3, IAM), Linux, Docker, Ansible, Terraform, Bash, GitHub Actions.
        """.trimIndent()
    )
)

val SampleJobDescriptions = listOf(
    Pair(
        "Senior Android Engineer (M3 & Room)",
        "We are looking for a Senior Android Engineer to scale our product portfolio. Must have expert knowledge of Kotlin, Jetpack Compose, asynchronous flows using Coroutines, and local local databases using Room. Strong interest in ATS optimization and architectural design patterns."
    ),
    Pair(
        "Advanced Full-Stack Engineer",
        "Seeking an experienced engineering lead. Key requirements include expert modern React, TypeScript, secure API routing with Node/FastAPI, Docker containerization, PostgreSQL querying, and caching using Redis."
    ),
    Pair(
        "Lead DevOps architect",
        "Targeting a Cloud Architect with AWS Solutions certification. Required skillsets: Docker, Kubernetes container scaling, Jenkins/GitHub Actions, Infrastructure as Code using Terraform, scripting, and site reliability engineering."
    )
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ResumeCoachScreen(
    viewModel: ResumeViewModel,
    modifier: Modifier = Modifier
) {
    val activeProfile by viewModel.activeProfile.collectAsState()
    val apiStatus by viewModel.apiStatus.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val sessionMessages by viewModel.sessionMessages.collectAsState()
    val interviewLoading by viewModel.interviewLoading.collectAsState()
    val profiles by viewModel.allProfiles.collectAsState()

    val context = LocalContext.current
    var resumeInputText by remember { mutableStateOf("") }
    var jdInputText by remember { mutableStateOf("") }
    var targetJobTitleTerm by remember { mutableStateOf("") }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth >= 760.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // App Premium Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.AccountBox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    if (isWideScreen) {
                        Text(
                            text = "Recruiter-Ready Elite ATS & Interview Prep",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Operational Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            RoundedCornerShape(32.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ProgressGreen)
                        )
                        Text(
                            text = "Dual-Core AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Tab bar switcher
            ScrollableTabRow(
                selectedTabIndex = currentTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { viewModel.setTab(0) },
                    text = { Text(stringResource(id = R.string.tab_ats), fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = {
                        if (activeProfile != null) {
                            viewModel.setTab(1)
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "Please uploaded or select an analyzed Resume profile first to unlock Mock Interview Coach!",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stringResource(id = R.string.tab_interview),
                                fontWeight = FontWeight.Bold,
                                color = if (activeProfile != null) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            if (activeProfile == null) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    },
                    icon = {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = if (activeProfile != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                )
                Tab(
                    selected = currentTab == 2,
                    onClick = {
                        if (activeProfile != null) {
                            viewModel.setTab(2)
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "Please uploaded or select an analyzed Resume profile first to unlock Career Learning Roadmap!",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stringResource(id = R.string.tab_roadmap),
                                fontWeight = FontWeight.Bold,
                                color = if (activeProfile != null) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            if (activeProfile == null) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    },
                    icon = {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            tint = if (activeProfile != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                )
                Tab(
                    selected = currentTab == 3,
                    onClick = { viewModel.setTab(3) },
                    text = { Text(stringResource(id = R.string.tab_settings), fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )
            }

            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> {
                        // TAB 0: ATS Scanner & Profile Creation
                        AtsScannerTab(
                            isWide = isWideScreen,
                            profiles = profiles,
                            selectedProfile = activeProfile,
                            apiStatus = apiStatus,
                            resumeText = resumeInputText,
                            jdText = jdInputText,
                            jobTitle = targetJobTitleTerm,
                            onResumeChange = { resumeInputText = it },
                            onJdChange = { jdInputText = it },
                            onTitleChange = { targetJobTitleTerm = it },
                            onProfileSelect = { profile ->
                                viewModel.selectProfile(profile)
                                if (profile != null) {
                                    resumeInputText = profile.resumeText
                                    jdInputText = profile.jobDescription
                                    targetJobTitleTerm = profile.targetJobTitle
                                }
                            },
                            onDeleteProfile = { viewModel.deleteProfile(it) },
                            onRunAnalysis = {
                                viewModel.analyzeResume(resumeInputText, targetJobTitleTerm, jdInputText)
                            },
                            viewModel = viewModel
                        )
                    }
                    1 -> {
                        // TAB 1: Mock Interview Coach (Voice-to-Text & Gemini API Enabled)
                        val cloudSummaries by viewModel.cloudInterviewSummaries.collectAsState()
                        val firestoreSyncStatus by viewModel.firestoreSyncStatus.collectAsState()
                        val localSessions by viewModel.allSessions.collectAsState()

                        InterviewCoachSection(
                            profile = activeProfile,
                            activeSession = activeSession,
                            messages = sessionMessages,
                            isLoading = interviewLoading,
                            errorState = apiStatus,
                            onStartInterview = { type -> viewModel.startMockInterview(type) },
                            onSubmitAnswer = { answer, audioPath -> viewModel.submitInterviewAnswer(answer, audioPath) },
                            onQuit = { viewModel.exitActiveSession() },
                            onNavigateToAts = { viewModel.setTab(0) },
                            onSaveSummary = {
                                val currentSess = activeSession
                                if (currentSess != null) {
                                    viewModel.saveInterviewCompletedSummary(currentSess, sessionMessages)
                                }
                            },
                            cloudSummaries = cloudSummaries,
                            onDeleteCloudSummary = { id, doc -> viewModel.deleteCloudInterviewSummary(id, doc) },
                            firestoreSyncStatus = firestoreSyncStatus,
                            localSessions = localSessions,
                            onSelectLocalSession = { sess -> viewModel.selectActiveSession(sess) },
                            onDeleteLocalSession = { sess -> viewModel.deleteInterviewSession(sess) }
                        )
                    }
                    2 -> {
                        // TAB 2: Career Learning Roadmap Coach
                        CareerRoadmapTab(
                            profile = activeProfile,
                            apiStatus = apiStatus,
                            onGenerateRoadmap = { profile -> viewModel.generateCareerRoadmap(profile) },
                            onNavigateToAts = { viewModel.setTab(0) },
                            viewModel = viewModel
                        )
                    }
                    3 -> {
                        // TAB 3: Settings, Personal Prep Notes & About Us
                        SettingsPrepNotesAboutTab(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

// ================== TAB COMPOSABLES ==================

@Composable
fun AtsScannerTab(
    isWide: Boolean,
    profiles: List<ResumeProfile>,
    selectedProfile: ResumeProfile?,
    apiStatus: EngineState,
    resumeText: String,
    jdText: String,
    jobTitle: String,
    onResumeChange: (String) -> Unit,
    onJdChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onProfileSelect: (ResumeProfile?) -> Unit,
    onDeleteProfile: (ResumeProfile) -> Unit,
    onRunAnalysis: () -> Unit,
    viewModel: ResumeViewModel
) {
    if (isWide) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left list of existing profiles (35%) + Cloud progress charts
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileHistoryList(
                    profiles = profiles,
                    selectedProfile = selectedProfile,
                    onSelect = onProfileSelect,
                    onDelete = onDeleteProfile
                )

                if (selectedProfile == null) {
                    CloudProgressTrackerSection(viewModel = viewModel)
                }
            }

            // Right form and results (65%)
            Column(
                modifier = Modifier
                    .weight(2.0f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AtsInputAndResultsArea(
                    selectedProfile = selectedProfile,
                    apiStatus = apiStatus,
                    resumeText = resumeText,
                    jdText = jdText,
                    jobTitle = jobTitle,
                    onResumeChange = onResumeChange,
                    onJdChange = onJdChange,
                    onTitleChange = onTitleChange,
                    onClearSelection = { onProfileSelect(null) },
                    onRunAnalysis = onRunAnalysis,
                    viewModel = viewModel
                )

                InteractiveAtsKeywordComparator(
                    resumeText = resumeText,
                    onResumeChange = onResumeChange
                )
            }
        }
    } else {
        // Stacked Layout
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (profiles.isNotEmpty()) {
                    Text(
                        text = "Your Analyzer Profiles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            Card(
                                modifier = Modifier
                                    .size(width = 140.dp, height = 90.dp)
                                    .clickable { onProfileSelect(null) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedProfile == null) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (selectedProfile == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("New Custom Scan", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        items(profiles) { profile ->
                            Card(
                                modifier = Modifier
                                    .size(width = 190.dp, height = 90.dp)
                                    .clickable { onProfileSelect(profile) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedProfile?.id == profile.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (selectedProfile?.id == profile.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = profile.targetJobTitle.ifBlank { "Unspecified Role" },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "ATS: ${profile.atsScore}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (profile.atsScore >= 75) ProgressGreen else PriorityMedium
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteProfile(profile) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                AtsInputAndResultsArea(
                    selectedProfile = selectedProfile,
                    apiStatus = apiStatus,
                    resumeText = resumeText,
                    jdText = jdText,
                    jobTitle = jobTitle,
                    onResumeChange = onResumeChange,
                    onJdChange = onJdChange,
                    onTitleChange = onTitleChange,
                    onClearSelection = { onProfileSelect(null) },
                    onRunAnalysis = onRunAnalysis,
                    viewModel = viewModel
                )
            }

            item {
                InteractiveAtsKeywordComparator(
                    resumeText = resumeText,
                    onResumeChange = onResumeChange
                )
            }

            if (selectedProfile == null) {
                item {
                    CloudProgressTrackerSection(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun ProfileHistoryList(
    profiles: List<ResumeProfile>,
    selectedProfile: ResumeProfile?,
    onSelect: (ResumeProfile?) -> Unit,
    onDelete: (ResumeProfile) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ATS Evaluation History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your custom analyzed candidates",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { onSelect(null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan New Resume", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (profiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No evaluation profiles saved yet. Fill out the form to parse with Gemini!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(profiles) { profile ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(profile) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedProfile?.id == profile.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.07f) else Color.Transparent
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (selectedProfile?.id == profile.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile.targetJobTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Ats score: ${profile.atsScore}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (profile.atsScore >= 75) ProgressGreen else PriorityMedium
                                    )
                                }
                                IconButton(onClick = { onDelete(profile) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AtsInputAndResultsArea(
    selectedProfile: ResumeProfile?,
    apiStatus: EngineState,
    resumeText: String,
    jdText: String,
    jobTitle: String,
    onResumeChange: (String) -> Unit,
    onJdChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onClearSelection: () -> Unit,
    onRunAnalysis: () -> Unit,
    viewModel: ResumeViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isExtractingPdf by remember { mutableStateOf(false) }
    var pdfLoadingError by remember { mutableStateOf<String?>(null) }
    var pdfSuccessMsg by remember { mutableStateOf<String?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isExtractingPdf = true
            pdfLoadingError = null
            pdfSuccessMsg = null
            coroutineScope.launch {
                val extractedText = extractTextFromPdfUri(context, uri)
                isExtractingPdf = false
                if (extractedText.isNotBlank()) {
                    onResumeChange(extractedText)
                    pdfSuccessMsg = context.getString(R.string.pdf_uploaded)
                } else {
                    pdfLoadingError = context.getString(R.string.pdf_error)
                }
            }
        }
    }

    if (selectedProfile != null) {
        // Show Detailed Analytical Reports
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header of Report
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = ProgressGreen)
                        Text(
                            text = "ATS Optimization Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onClearSelection) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Score Card Block
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Score Gauge
                    AtsScoreGauge(score = selectedProfile.atsScore)

                    Column {
                        Text(
                            text = "Target Role: ${selectedProfile.targetJobTitle.ifBlank { "Unassigned Target" }}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                selectedProfile.atsScore >= 80 -> "Excellent Candidate Profile. Very high interview likelihood."
                                selectedProfile.atsScore >= 65 -> "Medium Compatibility. Consider adding missing keywords below."
                                else -> "Needs Significant Work. Structural and keyword gaps found."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                AtsScoreBreakdownSection(profile = selectedProfile)

                Spacer(modifier = Modifier.height(14.dp))

                ResumeOptimizationSuggestionsModule(profile = selectedProfile)

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        PdfReporter.generateAndSharePdf(context, selectedProfile)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("download_pdf_report_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "PDF Download & Share Icon",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Download PDF Report",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }

                // --- CLOUD FIRESTORE SYNCHRONIZATION SECTION ---
                Spacer(modifier = Modifier.height(14.dp))

                var showConfigDialog by remember { mutableStateOf(false) }
                val firestoreProjectId by viewModel.firestoreProjectId.collectAsState()
                val firestoreApiKey by viewModel.firestoreApiKey.collectAsState()
                val syncStatus by viewModel.firestoreSyncStatus.collectAsState()

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("firestore_sync_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Firestore Cloud Synchronization", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                            }
                            IconButton(onClick = { showConfigDialog = true }, modifier = Modifier.size(36.dp).testTag("firestore_config_btn")) {
                                Icon(Icons.Default.Settings, contentDescription = "Cloud Configs")
                            }
                        }

                        Text(
                            text = "Save this ATS score trajectory report to Firestore Cloud to track improvements, skills acquired, and progress over time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.saveAtsReportToFirestore(selectedProfile) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("firestore_save_report_btn"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync ATS Report", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.fetchCloudAtsReports() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.height(44.dp).width(48.dp).testTag("firestore_refresh_btn"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh Cloud Reports", modifier = Modifier.size(16.dp))
                            }
                        }

                        // Operational Sync status indication
                        when (syncStatus) {
                            is EngineState.Loading -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Saving snapshot to Firestore...", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            is EngineState.Success -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ProgressGreen.copy(alpha = 0.08f)),
                                    modifier = Modifier.fillMaxWidth().testTag("sync_success_banner")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = ProgressGreen, modifier = Modifier.size(16.dp))
                                        Text((syncStatus as EngineState.Success).msg, style = MaterialTheme.typography.labelSmall, color = ProgressGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            is EngineState.Error -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
                                    modifier = Modifier.fillMaxWidth().testTag("sync_error_banner")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        Text((syncStatus as EngineState.Error).errorMsg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }

                if (showConfigDialog) {
                    CloudConfigDialog(
                        currentProjectId = firestoreProjectId,
                        currentApiKey = firestoreApiKey,
                        onSave = { pid, key ->
                            viewModel.setFirestoreConfigs(pid, key)
                            showConfigDialog = false
                        },
                        onDismiss = { showConfigDialog = false }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Matched Skills Chips
                if (selectedProfile.parsedSkills.isNotBlank()) {
                    Text("Skills Found in Resume", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedProfile.parsedSkills.split(",").forEach { skill ->
                            if (skill.trim().isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ProgressGreen.copy(alpha = 0.08f))
                                        .border(1.dp, ProgressGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = skill.trim(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ProgressGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Missing Skills Red Chips
                if (selectedProfile.missingSkills.isNotBlank()) {
                    Text("Missing Crucial Skills/Keywords", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedProfile.missingSkills.split(",").forEach { skill ->
                            if (skill.trim().isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = skill.trim(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Extracted Sections
                if (selectedProfile.education.isNotBlank()) {
                    Text("Education Check", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Text(selectedProfile.education, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (selectedProfile.experience.isNotBlank()) {
                    Text("Experience check", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Text(selectedProfile.experience, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Feedback
                if (selectedProfile.resumeStructureIssues.isNotBlank()) {
                    Text("Structural Feedback & Actionable Steps", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            selectedProfile.resumeStructureIssues,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    } else {
        // Show Resume Input Fields Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "A.I. Resume Optimization Engine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Demo preset selector for instant clicks!
                Column {
                    Text("Demo Profiles Preset (Click to instantly populate)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(SampleProfiles.zip(SampleJobDescriptions)) { (profileData, jdData) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .clickable {
                                        onResumeChange(profileData.second)
                                        onTitleChange(profileData.first)
                                        onJdChange(jdData.second)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = profileData.first.substringBefore(" ("),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // PDF Upload section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.03f))
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Have a PDF Resume?",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Instantly extract details to analyze",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        if (isExtractingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Button(
                                onClick = { pdfPickerLauncher.launch("application/pdf") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("upload_pdf_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(id = R.string.btn_upload_pdf),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (pdfSuccessMsg != null) {
                        Text(
                            text = pdfSuccessMsg!!,
                            color = ProgressGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (pdfLoadingError != null) {
                        Text(
                            text = pdfLoadingError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = jobTitle,
                    onValueChange = onTitleChange,
                    label = { Text("Target Job Title") },
                    placeholder = { Text("e.g. Senior Software Engineer") },
                    modifier = Modifier.fillMaxWidth().testTag("add_title_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = resumeText,
                    onValueChange = onResumeChange,
                    label = { Text("Paste Resume Content") },
                    placeholder = { Text(stringResource(id = R.string.resume_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("add_resume_input"),
                    maxLines = 10,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = jdText,
                    onValueChange = onJdChange,
                    label = { Text("Target Job Description (JD)") },
                    placeholder = { Text(stringResource(id = R.string.jd_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("add_jd_input"),
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp)
                )

                // Submit Scanner button
                if (apiStatus is EngineState.Loading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.loading_analysis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onRunAnalysis,
                        enabled = resumeText.isNotBlank() && jdText.isNotBlank() && jobTitle.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("analyze_ats_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.btn_analyze),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (apiStatus is EngineState.Error) {
                    Text(
                        text = (apiStatus as EngineState.Error).errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().testTag("error_msg")
                    )
                }
            }
        }
    }
}

@Composable
fun AtsScoreGauge(score: Int) {
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "atsScoreGaugeProgress"
    )

    Box(
        modifier = Modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        val strokeColor = if (score >= 75) ProgressGreen else PriorityMedium
        val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Track base
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
            // Progress arc
            drawArc(
                color = strokeColor,
                startAngle = -90f,
                sweepAngle = (animatedScore * 3.6f),
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${animatedScore.toInt()}%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = strokeColor
        )
    }
}

// ================== INTERVIEW COACH TAB ==================

@Composable
fun MockInterviewTab(
    profile: ResumeProfile?,
    activeSession: com.example.data.MockInterviewSession?,
    messages: List<InterviewMessage>,
    isLoading: Boolean,
    errorState: EngineState,
    onStartInterview: (String) -> Unit,
    onSubmitAnswer: (String) -> Unit,
    onQuit: () -> Unit
) {
    if (profile == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(48.dp))
            Text("Please scan or select a resume profile first in Tab 1 to activate mock interviews.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (activeSession == null) {
                // Setup and selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Interactive AI Interview Coaching",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Conduct a realistic mock interview. Gemini analyzes your answers inline and assigns progressive feedback scores based on recruiter rubric standards.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Select Interview Mode:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onStartInterview("TECHNICAL") },
                                modifier = Modifier.weight(1f).testTag("start_tech_interview"),
                                contentPadding = PaddingValues(4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Technical Round", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { onStartInterview("BEHAVIORAL") },
                                modifier = Modifier.weight(1f).testTag("start_behavioral_interview"),
                                contentPadding = PaddingValues(4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Behavioral (STAR)", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onStartInterview("HR") },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("HR Specialist", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { onStartInterview("COMPANY_SPECIFIC") },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Corporate Round", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            } else {
                // Active Interview Workspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Live ${activeSession.interviewType} Interview Simulation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(onClick = onQuit, modifier = Modifier.testTag("quit_interview_button")) {
                        Text("Quit Round", color = MaterialTheme.colorScheme.error)
                    }
                }

                // Messages Dialog Feed
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val listState = rememberLazyListState()
                    LaunchedEffect(messages.size) {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().testTag("chat_history"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages) { message ->
                            ChatBubble(message = message)
                        }
                    }
                }

                // Chat Input box
                var userResponseText by remember { mutableStateOf("") }
                val softwareKeyboardController = LocalSoftwareKeyboardController.current

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = userResponseText,
                        onValueChange = { userResponseText = it },
                        placeholder = { Text("Compose candidate answer…") },
                        modifier = Modifier.weight(1f).testTag("chat_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(
                            onClick = {
                                if (userResponseText.isNotBlank()) {
                                    onSubmitAnswer(userResponseText)
                                    userResponseText = ""
                                    softwareKeyboardController?.hide()
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("send_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.Send,
                                contentDescription = "Send",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: InterviewMessage) {
    val isAI = message.sender == "AI"

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalAlignment = if (isAI) Alignment.Start else Alignment.End
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isAI) 2.dp else 16.dp,
                        bottomEnd = if (isAI) 16.dp else 2.dp
                    )
                )
                .background(
                    if (isAI) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.primary
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = message.messageText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isAI) MaterialTheme.colorScheme.onSurface else Color.White
            )
        }

        // Display Score & Coaching feedback metadata below user bubble
        if (!isAI && message.evaluationScore != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.widthIn(max = 280.dp).padding(top = 2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Coaching Rubric Score", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background((if (message.evaluationScore >= 8) ProgressGreen else PriorityMedium).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${message.evaluationScore}/10",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (message.evaluationScore >= 8) ProgressGreen else PriorityMedium
                            )
                        }
                    }

                    if (!message.evaluationFeedback.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message.evaluationFeedback,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

// ================== LEARNING & ROADMAP TAB ==================

@Composable
fun CareerRoadmapTab(
    profile: ResumeProfile?,
    apiStatus: EngineState,
    onGenerateRoadmap: (ResumeProfile) -> Unit,
    onNavigateToAts: () -> Unit = {},
    viewModel: ResumeViewModel? = null
) {
    var showConfigDialog by remember { mutableStateOf(false) }

    if (showConfigDialog && viewModel != null) {
        val firestoreProjectId by viewModel.firestoreProjectId.collectAsState()
        val firestoreApiKey by viewModel.firestoreApiKey.collectAsState()
        CloudConfigDialog(
            currentProjectId = firestoreProjectId,
            currentApiKey = firestoreApiKey,
            onSave = { pid, key ->
                viewModel.setFirestoreConfigs(pid, key)
                showConfigDialog = false
            },
            onDismiss = { showConfigDialog = false }
        )
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
                    text = "Career & Syllabus Roadmap Locked",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "To generate personalized technical training roadmaps, target certified credentials, and custom build specs, you must first complete your profile setup.",
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
                        .testTag("navigate_to_ats_roadmap_btn"),
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Engineering Learning Roadmap Builder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A.I. analyzes missing CV skills to assemble milestone roadmaps, verified target certifications, and software build specs to impress interviewers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (profile.learningRoadmap.isBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Roadmap Offline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Press below to compile your step-by-step roadmap tailored specifically for ${profile.targetJobTitle}.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                             if (apiStatus is EngineState.Loading) {
                                CircularProgressIndicator()
                            } else {
                                Button(
                                    onClick = { onGenerateRoadmap(profile) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("generate_roadmap_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Compile Learning Roadmap")
                                }
                            }

                            if (apiStatus is EngineState.Error) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = apiStatus.errorMsg,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        
                                        if (viewModel != null) {
                                            TextButton(
                                                onClick = { showConfigDialog = true }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Configure API Key",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    InteractiveRoadmapDashboard(
                        profile = profile,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

object AtsScoringCalculator {
    data class ScoreBreakdown(
        val keywordMatchScore: Int,
        val matchedCount: Int,
        val totalCount: Int,
        val keywordDensityScore: Int,
        val densityPercentage: Double,
        val densityFeedback: String,
        val densityColor: Color,
        val formattingScore: Int,
        val wordCount: Int,
        val structuralTips: List<String>,
        val experienceScore: Int,
        val maxYearsDerived: Int,
        val seniorityKeywords: List<String>,
        val experienceStatus: String,
        val experienceFeedback: String,
        val overallScore: Int
    )

    fun computeBreakdown(profile: ResumeProfile): ScoreBreakdown {
        // 1. Technical Keyword Match
        val matchedList = profile.parsedSkills.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val missingList = profile.missingSkills.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val totalCount = matchedList.size + missingList.size
        val keywordMatchScore = if (totalCount > 0) {
            (matchedList.size * 100) / totalCount
        } else {
            profile.atsScore
        }

        // 2. Keyword Density Balance
        val cleanedText = profile.resumeText.lowercase().trim()
        val words = profile.resumeText.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size
        
        // Count mentions of parsed skills in resumeText
        var mentions = 0
        matchedList.forEach { skill ->
            val kw = skill.lowercase().trim()
            if (kw.isNotEmpty()) {
                var idx = 0
                while (true) {
                    idx = cleanedText.indexOf(kw, idx)
                    if (idx == -1) break
                    mentions++
                    idx += kw.length
                }
            }
        }
        val densityPercentage = if (wordCount > 0) {
            val rawDensity = (mentions.toDouble() / wordCount) * 100
            if (rawDensity == 0.0 && matchedList.isNotEmpty()) {
                (matchedList.size.toDouble() / (wordCount.coerceAtLeast(100))) * 100
            } else {
                rawDensity
            }
        } else {
            0.0
        }

        val roundedDensity = Math.round(densityPercentage * 100.0) / 100.0

        val (densityScore, densityFeedback, densityColor) = when {
            roundedDensity == 0.0 -> Triple(0, "No critical job description keywords found. Add top technical stacks.", Color(0xFFEF4444))
            roundedDensity < 0.5 -> Triple(30, "Incompatible keyword density index (Too weak).", Color(0xFFEF4444))
            roundedDensity < 1.5 -> Triple(75, "Moderate keyword density.", Color(0xFFF59E0B))
            roundedDensity <= 4.0 -> Triple(100, "Optimal keyword density (Professional).", Color(0xFF10B981))
            roundedDensity <= 6.0 -> Triple(70, "Elevated keyword density.", Color(0xFFF59E0B))
            else -> Triple(40, "Warning: Excess keyword density (Stuffing detected).", Color(0xFFEF4444))
        }

        // 3. Formatting Compliance
        val hasEmail = profile.resumeText.contains("@")
        val hasPhone = profile.resumeText.contains(Regex("\\+?\\d{1,3}[-.\\s]?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}")) || 
                       profile.resumeText.contains(Regex("\\b\\d{10}\\b"))

        val experienceFound = profile.experience.isNotBlank() && !profile.experience.contains("No experience specified", ignoreCase = true)
        val educationFound = profile.education.isNotBlank() && !profile.education.contains("No education specified", ignoreCase = true)
        val skillsFound = profile.parsedSkills.isNotBlank()

        val sectionsFoundCount = (if (experienceFound) 1 else 0) +
                                 (if (educationFound) 1 else 0) +
                                 (if (skillsFound) 1 else 0) + 1
        val sectionsScore = (sectionsFoundCount * 100) / 4

        val contactMultiplier = (if (hasEmail) 1.0 else 0.0) + (if (hasPhone) 1.0 else 0.0)
        val contactScore = ((contactMultiplier / 2.0) * 100).toInt()

        val lengthScore = when {
            wordCount < 200 -> 40
            wordCount < 400 -> 80
            wordCount <= 1100 -> 100
            else -> 70
        }
        val formattingScore = ((sectionsScore * 0.4) + (contactScore * 0.3) + (lengthScore * 0.3)).toInt()

        val tips = mutableListOf<String>()
        if (!experienceFound) tips.add("Add a clear header named 'Experience' or 'Professional Work'.")
        if (!educationFound) tips.add("List your 'Education' and academic qualifications.")
        if (!skillsFound) tips.add("Organize a designated 'Skills' grid for readability.")
        if (!hasEmail) tips.add("Include a readable email contact account.")
        if (!hasPhone) tips.add("Add your target mobile phone number.")
        if (wordCount > 1100) tips.add("Condense verbose paragraphs; aim for 1 or 2 tidy pages.")
        if (tips.isEmpty()) tips.add("Formatting is compliant with commercial parser standards!")

        // 4. Experience & Seniority Alignment
        val yearsMatches = Regex("(\\d+)\\+?\\s*(?:years?|yrs?|yr)\\b").findAll(cleanedText)
        val parsedYearsList = yearsMatches.map { it.groupValues[1].toInt() }.toList()
        val maxYearsFound = parsedYearsList.maxOrNull() ?: if (profile.experience.contains("year", ignoreCase = true)) {
            val digitMatch = Regex("\\b(\\d+)\\b").find(profile.experience)
            digitMatch?.groupValues?.get(1)?.toIntOrNull() ?: 2
        } else {
            3
        }

        val minExpected = when {
            profile.targetJobTitle.contains("senior", ignoreCase = true) -> 5
            profile.targetJobTitle.contains("lead", ignoreCase = true) -> 5
            profile.targetJobTitle.contains("manager", ignoreCase = true) -> 5
            profile.targetJobTitle.contains("architect", ignoreCase = true) -> 6
            profile.targetJobTitle.contains("principal", ignoreCase = true) -> 8
            else -> 2
        }

        val seniorityKeywords = listOf("senior", "lead", "architect", "principal", "manager", "expert", "experienced", "engineer", "mid")
        val matchedSeniority = seniorityKeywords.filter { cleanedText.contains(it) }

        var experienceScore = 50
        if (maxYearsFound >= minExpected) {
            experienceScore += 30
        } else if (maxYearsFound > 0) {
            val diff = minExpected - maxYearsFound
            experienceScore += maxOf(0, 30 - (diff * 10))
        }

        if (matchedSeniority.isNotEmpty()) {
            experienceScore += minOf(20, matchedSeniority.size * 10)
        } else {
            if (minExpected >= 5) {
                experienceScore -= 15
            }
        }
        experienceScore = minOf(100, maxOf(0, experienceScore))

        val alignmentStatus = when {
            experienceScore >= 85 -> "Target Experience Fully Aligned"
            experienceScore >= 60 -> "Adequate Dynamic Alignment"
            else -> "Experience Gaps Detected"
        }
        val experienceFeedback = when {
            experienceScore >= 85 -> "Excellent alignment. The CV expresses $maxYearsFound+ years and seniority triggers fitting a ${profile.targetJobTitle}."
            experienceScore >= 60 -> "Adequate alignment detected. Expectations for ${profile.targetJobTitle} are $minExpected+ years. Detected: $maxYearsFound."
            else -> "The target job requires substantial ${profile.targetJobTitle} foundations ($minExpected+ years). Please update description."
        }

        val overallScoreVal = profile.atsScore

        return ScoreBreakdown(
            keywordMatchScore = keywordMatchScore,
            matchedCount = matchedList.size,
            totalCount = totalCount,
            keywordDensityScore = densityScore,
            densityPercentage = roundedDensity,
            densityFeedback = densityFeedback,
            densityColor = densityColor,
            formattingScore = formattingScore,
            wordCount = wordCount,
            structuralTips = tips,
            experienceScore = experienceScore,
            maxYearsDerived = maxYearsFound,
            seniorityKeywords = matchedSeniority,
            experienceStatus = alignmentStatus,
            experienceFeedback = experienceFeedback,
            overallScore = overallScoreVal
        )
    }
}

@Composable
fun AtsScoreBreakdownSection(profile: ResumeProfile) {
    val breakdown = remember(profile) { AtsScoringCalculator.computeBreakdown(profile) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("ats_score_breakdown_section"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "ATS Scanner Score Breakdown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "These indices replicate corporate ATS parsers scoring rules (weighted keyword intersection, document sizing, density pacing and seniority alignment).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)

            // 1. Keyword Match (40% Weight)
            BreakdownRowItem(
                title = "Keyword Matching",
                weight = "40% weight",
                score = breakdown.keywordMatchScore,
                subtitle = "${breakdown.matchedCount} of ${breakdown.totalCount} critical skills matching",
                icon = Icons.Default.Search,
                testTag = "breakdown_progress_keyword_match",
                gaugeColor = if (breakdown.keywordMatchScore >= 80) ProgressGreen 
                             else if (breakdown.keywordMatchScore >= 60) PriorityMedium 
                             else PriorityHigh
            )

            // 2. Keyword Density (20% Weight)
            BreakdownRowItem(
                title = "Keyword Density",
                weight = "20% weight",
                score = breakdown.keywordDensityScore,
                subtitle = "Density: ${breakdown.densityPercentage}% — ${breakdown.densityFeedback}",
                icon = Icons.Default.Build,
                testTag = "breakdown_progress_keyword_density",
                gaugeColor = breakdown.densityColor
            )

            // 3. Formatting & Structure (20% Weight)
            BreakdownRowItem(
                title = "Formatting Compliance",
                weight = "20% weight",
                score = breakdown.formattingScore,
                subtitle = "Word count: ${breakdown.wordCount} words — ${breakdown.structuralTips.firstOrNull() ?: ""}",
                icon = Icons.Default.Edit,
                testTag = "breakdown_progress_formatting",
                gaugeColor = if (breakdown.formattingScore >= 80) ProgressGreen 
                             else if (breakdown.formattingScore >= 60) PriorityMedium 
                             else PriorityHigh
            )

            // 4. Experience & Seniority (20% Weight)
            BreakdownRowItem(
                title = "Experience Alignment",
                weight = "20% weight",
                score = breakdown.experienceScore,
                subtitle = "${breakdown.experienceStatus} (${breakdown.maxYearsDerived}+ yrs parsed)",
                icon = Icons.Default.Star,
                testTag = "breakdown_progress_experience",
                gaugeColor = if (breakdown.experienceScore >= 80) ProgressGreen 
                             else if (breakdown.experienceScore >= 60) PriorityMedium 
                             else PriorityHigh
            )
        }
    }
}

@Composable
fun BreakdownRowItem(
    title: String,
    weight: String,
    score: Int,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    gaugeColor: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = score.toFloat() / 100f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "breakdownProgress"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(gaugeColor.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = gaugeColor
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = weight,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
            Text(
                text = "${(animatedProgress * 100).toInt()}/100",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = gaugeColor
            )
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .testTag(testTag),
            color = gaugeColor,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 38.dp)
        )
    }
}

// ================== CLOUD TRACKING SUPPORTING COMPOSABLES ==================

@Composable
fun CloudConfigDialog(
    currentProjectId: String,
    currentApiKey: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var projectId by remember { mutableStateOf(currentProjectId) }
    var apiKey by remember { mutableStateOf(currentApiKey) }
    var useSandbox by remember { mutableStateOf(currentProjectId == "aistudio-resume-coach") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Cloud Firestore Configs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Configure Firestore connection to sync reports and track your career growth progress over time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            useSandbox = true
                            projectId = "aistudio-resume-coach"
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (useSandbox) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            contentColor = if (useSandbox) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Standard Sandbox", fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            useSandbox = false
                            if (projectId == "aistudio-resume-coach") {
                                projectId = ""
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!useSandbox) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            contentColor = if (!useSandbox) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Custom Project", fontSize = 11.sp, maxLines = 1)
                    }
                }

                OutlinedTextField(
                    value = projectId,
                    onValueChange = {
                        projectId = it
                        useSandbox = (it == "aistudio-resume-coach")
                    },
                    label = { Text("Firestore Project ID") },
                    placeholder = { Text("e.g. my-firebase-project-123") },
                    modifier = Modifier.fillMaxWidth().testTag("config_project_id_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Web API Key") },
                    placeholder = { Text("Using Gemini Key by default") },
                    modifier = Modifier.fillMaxWidth().testTag("config_api_key_input"),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text(
                    text = "*Standard Sandbox uses a global public testing database so you can sync and play out-of-the-box. Change to Custom Project for private connections.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(projectId, apiKey) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_config_btn")
            ) {
                Text("Save Configuration")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CloudProgressChart(reports: List<com.example.data.CloudAtsReport>) {
    val sortedReports = reports.sortedBy { it.timestamp }
    val scores = sortedReports.map { it.atsScore }

    if (scores.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "ATS Career Score Trajectory",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Draw grid/background lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = height * i / gridLines
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    if (scores.size == 1) {
                        // Plot a single dot representing the point
                        val x = width / 2f
                        val y = height * (1f - (scores[0] / 100f))
                        drawCircle(
                            color = Color(0xFF4CAF50),
                            radius = 12f,
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 6f,
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                    } else {
                        // Draw continuous line path with translucent area fill
                        val points = scores.mapIndexed { idx, score ->
                            val x = width * idx / (scores.size - 1)
                            val y = height * (1f - (score / 100f))
                            androidx.compose.ui.geometry.Offset(x, y)
                        }

                        val linePath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }

                        val areaPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points[0].x, height)
                            for (i in points.indices) {
                                lineTo(points[i].x, points[i].y)
                            }
                            lineTo(points.last().x, height)
                            close()
                        }

                        // Gradient fill
                        drawPath(
                            path = areaPath,
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    ProgressGreen.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )

                        // Path stroke outline
                        drawPath(
                            path = linePath,
                            color = ProgressGreen,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 3.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )

                        // Draw highlight dots on nodes
                        for (pt in points) {
                            drawCircle(
                                color = Color.White,
                                radius = 6f,
                                center = pt
                            )
                            drawCircle(
                                color = ProgressGreen,
                                radius = 4f,
                                center = pt,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CloudProgressTrackerSection(viewModel: ResumeViewModel) {
    val cloudAtsReports by viewModel.cloudAtsReports.collectAsState()
    val firestoreProjectId by viewModel.firestoreProjectId.collectAsState()
    val firestoreApiKey by viewModel.firestoreApiKey.collectAsState()
    var showConfigDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("cloud_progress_tracker_section"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Cloud Progress (Firestore)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = { showConfigDialog = true }, modifier = Modifier.size(36.dp).testTag("cloud_hub_settings_btn")) {
                    Icon(Icons.Default.Settings, contentDescription = "Firestore Settings", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (showConfigDialog) {
                CloudConfigDialog(
                    currentProjectId = firestoreProjectId,
                    currentApiKey = firestoreApiKey,
                    onSave = { pid, key ->
                        viewModel.setFirestoreConfigs(pid, key)
                        showConfigDialog = false
                    },
                    onDismiss = { showConfigDialog = false }
                )
            }

            if (cloudAtsReports.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    Text(
                        text = "No Cloud Reports Saved Yet",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Fill out a resume form, run analysis, and click 'Sync ATS Report' to start tracking progress!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.fetchCloudAtsReports() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refresh Cloud", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Draw native visual trajectory chart
                CloudProgressChart(reports = cloudAtsReports)

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved Evaluations History",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(onClick = { viewModel.fetchCloudAtsReports() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Cloud Reports", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }

                cloudAtsReports.sortedByDescending { report -> report.timestamp }.forEach { report ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = report.targetJobTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val dateStr = remember(report.timestamp) {
                                    try {
                                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(report.timestamp))
                                    } catch (e: Exception) {
                                        "Date Synced"
                                    }
                                }
                                Text(
                                    text = "Date Synced: $dateStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (report.atsScore >= 75) ProgressGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${report.atsScore}%",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = if (report.atsScore >= 75) ProgressGreen else MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteCloudAtsReport(report.id, report.docPath) },
                                    modifier = Modifier.size(32.dp).testTag("delete_cloud_report_${report.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Cloud Report",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
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

// ================== REAL-TIME ATS KEYWORD COMPARATOR SUPPORT SYSTEM ==================

enum class IndustryDomain(val displayName: String, val essentialKeywords: List<String>) {
    SOFTWARE_ENG("Software Engineering", listOf("Agile", "Git", "API", "Testing", "CI/CD", "REST", "Databases", "System Design", "Architecture", "Debugging", "Code Review", "OOP")),
    FRONTEND("Frontend Web Developer", listOf("HTML", "CSS", "JavaScript", "TypeScript", "React", "Vue", "Angular", "Tailwind", "Sass", "Webpack", "Responsive Design", "GraphQL", "Next.js", "SEO")),
    BACKEND("Backend Developer", listOf("Node.js", "Python", "Java", "Go", "Spring Boot", "Express", "SQL", "PostgreSQL", "MongoDB", "Redis", "Docker", "gRPC", "Microservices", "AWS")),
    MOBILE_DEV("Mobile App Developer", listOf("Kotlin", "Swift", "Android", "iOS", "Jetpack Compose", "SwiftUI", "Flutter", "React Native", "Dependency Injection", "Coroutines", "CocoaPods", "Play Store", "App Store")),
    DATA_SCIENCE("Data Science & AI", listOf("Python", "Machine Learning", "Deep Learning", "TensorFlow", "PyTorch", "Pandas", "NumPy", "SQL", "Tableau", "Statistics", "Gemini", "NLP", "LLMs", "R")),
    DEVOPS("DevOps & Cloud Systems", listOf("AWS", "Docker", "Kubernetes", "Terraform", "CI/CD", "Jenkins", "Linux", "GCP", "Azure", "Ansible", "Monitoring", "Bash", "YAML", "Security")),
    PRODUCT_MGMT("Product Management", listOf("Roadmap", "Scrum", "User Stories", "KPIs", "Analytics", "Market Research", "Stakeholders", "Agile", "Wireframes", "Prioritization", "Product Launch", "SQL"))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveAtsKeywordComparator(
    resumeText: String,
    onResumeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDomain by remember { mutableStateOf(IndustryDomain.SOFTWARE_ENG) }
    var customKeywordInput by remember { mutableStateOf("") }
    var userKeywords by remember { mutableStateOf(setOf<String>()) }

    val cleanedResume = remember(resumeText) {
        resumeText.lowercase(java.util.Locale.getDefault())
    }

    // Combined target keywords
    val targetKeywords = remember(selectedDomain, userKeywords) {
        selectedDomain.essentialKeywords + userKeywords
    }

    // Analysis results
    val matchedCount = remember(targetKeywords, cleanedResume) {
        targetKeywords.count { cleanedResume.contains(it.lowercase(java.util.Locale.getDefault())) }
    }

    val totalCount = targetKeywords.size
    val matchPercentage = if (totalCount > 0) (matchedCount * 100) / totalCount else 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ats_keyword_comparator_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Real-Time ATS Keyword Comparator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Scan resume against essential industry terms live",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Industry selection list
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Select Target Discipline / Domain",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(IndustryDomain.entries.toTypedArray()) { domain ->
                        val isSelected = domain == selectedDomain
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDomain = domain },
                            label = { Text(domain.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                borderWidth = 1.dp
                            ),
                            modifier = Modifier.testTag("domain_chip_${domain.name.lowercase(java.util.Locale.getDefault())}")
                        )
                    }
                }
            }

            // Score Banner & Progress Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Percentage score
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { matchPercentage.toFloat() / 100f },
                        modifier = Modifier.size(56.dp).testTag("live_comparator_progress"),
                        color = if (matchPercentage >= 75) ProgressGreen else if (matchPercentage >= 50) PriorityMedium else PriorityHigh,
                        strokeWidth = 6.dp,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                    Text(
                        text = "$matchPercentage%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Match Score: $matchedCount of $totalCount terms found",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val commentText = when {
                        matchPercentage >= 75 -> "Excellent industry vocabulary representation! Optimized for parsers."
                        matchPercentage >= 50 -> "Good foundational keyword alignment. Add highlighted grey terms below to score higher."
                        else -> "Weak keywords representation. Prioritize placing standard terminology on your CV."
                    }
                    Text(
                        text = commentText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Custom target keywords input
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Add Custom Target Competency",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customKeywordInput,
                        onValueChange = { customKeywordInput = it },
                        placeholder = { Text("e.g. Jenkins, Scrum, GraphQL") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_keyword_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    Button(
                        onClick = {
                            if (customKeywordInput.isNotBlank()) {
                                val trimmed = customKeywordInput.trim()
                                if (!userKeywords.contains(trimmed)) {
                                    userKeywords = userKeywords + trimmed
                                }
                                customKeywordInput = ""
                            }
                        },
                        enabled = customKeywordInput.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("add_custom_keyword_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add custom target skill", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Live matches breakdown list
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Essential Skills Match Matrix",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    targetKeywords.forEach { keyword ->
                        val isFound = cleanedResume.contains(keyword.lowercase(java.util.Locale.getDefault()))
                        val isCustom = userKeywords.contains(keyword)

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isFound) ProgressGreen.copy(alpha = 0.08f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                                )
                                .border(
                                    border = BorderStroke(
                                        1.dp,
                                        if (isFound) ProgressGreen.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isFound) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = if (isFound) "Matched" else "Missing",
                                tint = if (isFound) ProgressGreen else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = keyword,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isFound) ProgressGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = if (isFound) FontWeight.Bold else FontWeight.Normal
                            )

                            if (isCustom) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove custom keyword",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            userKeywords = userKeywords - keyword
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Quick live simulation help banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Instruction tip",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Pro-Tip: Try typing or pasting missing keyphrases directly into your 'Paste Resume Content' above to see your score upgrade instantly!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ================== RESUME OPTIMIZATION SUGGESTIONS PLAYBOOK SYSTEM ==================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResumeOptimizationSuggestionsModule(
    profile: ResumeProfile,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("Leadership") }
    var selectedVerb by remember { mutableStateOf("Spearheaded") }
    
    // Checklist state is remembered per CV
    var checklistState by remember(profile.id) {
        mutableStateOf(
            mapOf(
                "action_verbs" to false,
                "xyz_formula" to false,
                "contact_info" to false,
                "structural_grid" to false,
                "keyword_stuffing" to false
            )
        )
    }

    val resumeLower = remember(profile.resumeText) { profile.resumeText.lowercase(java.util.Locale.getDefault()) }
    val detectedWeakPhrases = remember(resumeLower) {
        listOf(
            "responsible for" to "Spearheaded, engineered, or pioneered",
            "helped" to "Collaborated on, facilitated, or championed",
            "assisted" to "Co-architected, supported, or reinforced",
            "managed" to "Directed, steered, orchestrated, or pioneered",
            "worked on" to "Designed, implemented, deployed, or executed",
            "handled" to "Steered, streamlined, resolved, or engineered",
            "did" to "Accomplished, engineered, executed, or generated",
            "made" to "Formulated, fabricated, constructed, or delivered",
            "duties included" to "Spearheaded, delivered, managed, or guided"
        ).filter { (weak, _) -> resumeLower.contains(weak) }
    }

    val missingSkillsList = remember(profile.missingSkills) {
        profile.missingSkills.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    val verbCategories = listOf("Leadership", "Execution", "Optimization", "Analysis")
    
    val verbsInCategories = mapOf(
        "Leadership" to listOf("Spearheaded", "Orchestrated", "Championed"),
        "Execution" to listOf("Architected", "Deployed", "Executed"),
        "Optimization" to listOf("Optimized", "Leveraged", "Amplified"),
        "Analysis" to listOf("Deciphered", "Quantified", "Validated")
    )

    val verbExamples = mapOf(
        "Spearheaded" to Pair(
            "Responsible for managing the mobile application migration project.",
            "Spearheaded complete Android UI rewrite to Jetpack Compose for a 120k active user application, boosting responsiveness index by 63%."
        ),
        "Orchestrated" to Pair(
            "Helped run meetings and project releases with our developers.",
            "Orchestrated cross-functional Agile routines, accelerating deployment cycles from monthly to bi-weekly while decreasing team regression bugs by 40%."
        ),
        "Championed" to Pair(
            "Told the team we should start using Kotlin coroutines to fix crashes.",
            "Championed strict background thread guidelines using Kotlin Coroutines, slashing secondary memory crashes and runtime exceptions by 48%."
        ),
        "Architected" to Pair(
            "I wrote a database for storage on the client side.",
            "Architected a redundant local SQLite Room DB with background KSP flows, supporting offline-first data synchronization with zero UI-thread freeze."
        ),
        "Deployed" to Pair(
            "Assisted the company in writing and fixing deployment scripts.",
            "Deployed automated CI/CD pipelines via GitHub Actions, stripping manual staging overhead by 12 developer-hours per sprint."
        ),
        "Executed" to Pair(
            "Did manual backend performance tests and bug finding.",
            "Executed strategic end-to-end unit tests with Robolectric, catching 30+ critical background execution faults before production rollout."
        ),
        "Optimized" to Pair(
            "Helped make slow SQL queries load faster for users.",
            "Optimized nested SQL repository indexes, boosting real-time statistics generation speed by 54%."
        ),
        "Leveraged" to Pair(
            "Used Room database queries for high efficiency.",
            "Leveraged SQLite SQLite-indexing triggers and LiveData state wrappers, rendering local analytics reports with 80ms display speed."
        ),
        "Amplified" to Pair(
            "Worked on getting more organic user registration views.",
            "Amplified search discoverability by 28% through custom metadata injection and asset pre-fetching routines."
        ),
        "Deciphered" to Pair(
            "Did data data analytics work to research user behavior bugs.",
            "Deciphered high-density user dropout logs, formulating corrective flow models that reclaimed $24k in stalled subscription revenue."
        ),
        "Quantified" to Pair(
            "Handled analytics reports for management review.",
            "Quantified performance friction indexes to present a business case for upgrading legacy API microservice backends."
        ),
        "Validated" to Pair(
            "Helped test the REST APIs to ensure they worked.",
            "Validated 18 cloud backend API gateways for absolute telemetry compliance, establishing robust client payload security."
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("resume_optimization_suggestions_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Optimization Suggestions Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Resume Optimization Suggestions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Actionable playbook to boost your ATS matching score",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // 1. Dynamic Phase Priority Card
            val (phaseTitle, phaseColor, phaseDesc, phaseIcon) = when {
                profile.atsScore < 50 -> Quadruple(
                    "Phase 1: Hard Recovery Needed",
                    Color(0xFFEF4444),
                    "Your CV exhibits severe formatting or essential keyword gaps. Focus on adding core skills and mandatory contact details.",
                    Icons.Default.Info
                )
                profile.atsScore < 80 -> Quadruple(
                    "Phase 2: High-Impact Phrasing Calibration",
                    Color(0xFFF59E0B),
                    "Foundational sections are present. Enhance score by substituting passive phrasing with strong action verbs and quantifying achievements.",
                    Icons.Default.Edit
                )
                else -> Quadruple(
                    "Phase 3: Elite Styling & Polish",
                    Color(0xFF10B981),
                    "Excellent CV! Fine-tune the structural hierarchy and balance keyword density to guarantee compatibility.",
                    Icons.Default.Check
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(phaseColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .border(1.dp, phaseColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = phaseIcon,
                    contentDescription = null,
                    tint = phaseColor,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = phaseTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = phaseColor
                    )
                    Text(
                        text = phaseDesc,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)

            // 2. ATS Keyword Optimization Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "A. Essential Missing Keywords Injector",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "To clear corporate screening algorithms, integrate these target skills into your professional experiences listing. Do not just list them; frame them with actionable metrics.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                if (missingSkillsList.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        missingSkillsList.forEach { skill ->
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFFEF4444), CircleShape)
                                )
                                Text(
                                    text = skill,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF10B981).copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Matched", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Text(
                            text = "No missing technical skills identified! Excellent corporate context alignment.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)

            // 3. Passive Phrasing Scanner
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "B. Live Resume Weak Verb Scanner",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Weak or passive phrasing sounds descriptive rather than impact-driven. Our parser actively scans your uploaded CV text to point out weak sections.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                if (detectedWeakPhrases.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        detectedWeakPhrases.forEach { (weak, recommendation) ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Weak word warning",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Weak phrasing detected: \"$weak\"",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Replace with: $recommendation",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF10B981).copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Clean Verb Scan", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Text(
                            text = "Excellent! No passive triggers detected. Every line represents active authorship.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)

            // 4. Interactive Active Verb Coach
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "C. Interactive Impact-Verb Playbook",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Choose an actionable competency category and select a strong active verb to visualize its transformation using Google's X-Y-Z formula instructions.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                // Category selector row
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(verbCategories) { category ->
                        val isSelected = category == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                selectedCategory = category 
                                selectedVerb = verbsInCategories[category]?.firstOrNull() ?: ""
                            },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                borderWidth = 1.dp
                            ),
                            modifier = Modifier.testTag("verb_cat_chip_$category")
                        )
                    }
                }

                // Verb selection list for active category
                val verbs = verbsInCategories[selectedCategory] ?: emptyList()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    verbs.forEach { verb ->
                        val isSelected = verb == selectedVerb
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedVerb = verb }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("verb_select_chip_$verb")
                        ) {
                            Text(
                                text = verb,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Selected Verb transformation visualization
                val example = verbExamples[selectedVerb]
                if (example != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("verb_example_card_${selectedVerb.lowercase(java.util.Locale.getDefault())}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = "Verb Spotlight", tint = Color(0xFFD946EF), modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Spotlight: \"$selectedVerb\"",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Output comparisons
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Before text
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.02f), RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "PASSIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFEF4444),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "\"${example.first}\"",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }

                                // After text
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF10B981).copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "OPTIMIZED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = example.second,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)

            // 5. Dynamic Optimization Task Planner
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "D. CV Optimization Task Planner",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Track your action goals offline as you revise your local resume structure and word selections:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val taskLabels = listOf(
                        "action_verbs" to "Verify every bullet starts with an interactive high-impact verb",
                        "xyz_formula" to "Recast experiences with the X-Y-Z formula (numerical achievements)",
                        "contact_info" to "Integrate standard email and phone headers in formatting layout",
                        "structural_grid" to "Structure distinct work history headings matching ATS indices",
                        "keyword_stuffing" to "Verify density to ensure keywords are within normal pacing bounds (1.5% - 4.0%)"
                    )

                    taskLabels.forEach { (key, label) ->
                        val checked = checklistState[key] ?: false
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    checklistState = checklistState.toMutableMap().apply { put(key, !checked) }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (checked) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .background(
                                        color = if (checked) Color(0xFF10B981).copy(alpha = 0.1f) else Color.Transparent,
                                        shape = RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (checked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Task completed",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (checked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (checked) FontWeight.Normal else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsPrepNotesAboutTab(
    viewModel: ResumeViewModel
) {
    var subTab by remember { mutableStateOf(0) } // 0: Notes, 1: Settings, 2: About Us

    // Notes input state
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var noteCategory by remember { mutableStateOf("Interview Q") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("All") }

    val notes by viewModel.allNotes.collectAsState()
    val interviewerPersona by viewModel.interviewerPersona.collectAsState()
    val targetDifficulty by viewModel.targetDifficulty.collectAsState()
    val detailedFeedback by viewModel.detailedFeedback.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sub-Tab Switcher
        TabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text("Prep Notes", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("AI Settings", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = subTab == 2,
                onClick = { subTab = 2 },
                text = { Text("About Us", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        // Sub-Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (subTab) {
                0 -> {
                    // ----------------- SUB-TAB 0: PERSONAL PREP NOTES -----------------
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Note creation form
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Create Study or Prep Note",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    OutlinedTextField(
                                        value = noteTitle,
                                        onValueChange = { noteTitle = it },
                                        label = { Text("Note Title") },
                                        placeholder = { Text("e.g. Kotlin Thread Safety strategy") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = noteContent,
                                        onValueChange = { noteContent = it },
                                        label = { Text("Note Content") },
                                        placeholder = { Text("Use coroutines state flow instead of shared variables...") },
                                        minLines = 3,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Category selection chips
                                    Text(
                                        text = "Select Category",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf("General", "Interview Q", "Resume Tip", "Roadmap Goal").forEach { cat ->
                                            FilterChip(
                                                selected = noteCategory == cat,
                                                onClick = { noteCategory = cat },
                                                label = { Text(cat, fontSize = 11.sp) }
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (noteTitle.isNotBlank() && noteContent.isNotBlank()) {
                                                viewModel.insertNote(noteTitle, noteContent, noteCategory)
                                                // Clear states
                                                noteTitle = ""
                                                noteContent = ""
                                            }
                                        },
                                        enabled = noteTitle.isNotBlank() && noteContent.isNotBlank(),
                                        modifier = Modifier.align(Alignment.End),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Note")
                                    }
                                }
                            }
                        }

                        // Note search and category filtering
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    label = { Text("Search your notes...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                                )

                                // Horizontal filter category chips
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("All", "General", "Interview Q", "Resume Tip", "Roadmap Goal").forEach { categoryTab ->
                                        FilterChip(
                                            selected = selectedFilterCategory == categoryTab,
                                            onClick = { selectedFilterCategory = categoryTab },
                                            label = { Text(categoryTab, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        // List existing Prep Notes
                        val filteredNotes = notes.filter { note ->
                            val matchesCategory = (selectedFilterCategory == "All" || note.category == selectedFilterCategory)
                            val matchesSearch = (note.title.contains(searchQuery, ignoreCase = true) || note.content.contains(searchQuery, ignoreCase = true))
                            matchesCategory && matchesSearch
                        }

                        if (filteredNotes.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No preparation notes found matching filters.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        } else {
                            items(filteredNotes) { note ->
                                var expanded by remember { mutableStateOf(false) }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expanded = !expanded },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                             horizontalArrangement = Arrangement.SpaceBetween,
                                             verticalAlignment = Alignment.CenterVertically,
                                             modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = note.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                // Category tag
                                                Surface(
                                                    color = when (note.category) {
                                                        "Interview Q" -> MaterialTheme.colorScheme.primaryContainer
                                                        "Resume Tip" -> MaterialTheme.colorScheme.secondaryContainer
                                                        "Roadmap Goal" -> MaterialTheme.colorScheme.tertiaryContainer
                                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                                    },
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = note.category,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteNote(note) }
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete Note",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                                )
                                            }
                                        }

                                        Text(
                                            text = note.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            maxLines = if (expanded) 20 else 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(note.timestamp)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // ----------------- SUB-TAB 1: AI COACH SETTINGS -----------------
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Interview Coach Persona",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Customize how the AI frames questions and analyzes response quality during your mock interviews:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )

                                val personas = listOf("Technical Architect", "Strict Recruiter", "Encouraging Mentor")
                                personas.forEach { item ->
                                    Surface(
                                        color = if (interviewerPersona == item) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.setInterviewerPersona(item) }
                                            .border(
                                                1.dp,
                                                if (interviewerPersona == item) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = interviewerPersona == item,
                                                onClick = { viewModel.setInterviewerPersona(item) }
                                            )
                                            Text(
                                                text = item,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Target Seniority & Difficulty",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                val difficulties = listOf("Junior Developer", "Senior Engineer", "Lead Architect")
                                difficulties.forEach { level ->
                                    Surface(
                                        color = if (targetDifficulty == level) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.setTargetDifficulty(level) }
                                            .border(
                                                1.dp,
                                                if (targetDifficulty == level) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = targetDifficulty == level,
                                                onClick = { viewModel.setTargetDifficulty(level) }
                                            )
                                            Text(
                                                text = level,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Detailed Code Feedback",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Enable specific code examples and syntactical keywords in evaluation answers.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Switch(
                                    checked = detailedFeedback,
                                    onCheckedChange = { viewModel.setDetailedFeedback(it) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // ----------------- SUB-TAB 2: ABOUT US -----------------
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(
                            text = "AI Resume & Interview Coach",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "Version 3.5.0 - Pro Edition",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Divider(modifier = Modifier.padding(horizontal = 32.dp))

                        Text(
                            text = "Our platform utilizes a cutting-edge Dual-Agent Core powered by Gemini AI. The system seamlessly parses raw resume files, computes critical skill gaps versus live employment standards, structures custom roadmaps with milestone achievements, and runs responsive voice-assisted mock conversation simulations.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Key System Capabilities",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text("Surgical ATS parsing & skill metrics score", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text("Step-by-step interactive roadmap builder", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text("Conversational state-aware mock interview coach", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text("Offline-first data security with dynamic profile switching", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Text(
                            text = "Designed for ambitious developers making bold career steps worldwide. Fuelled by Google Gemini AI.",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
