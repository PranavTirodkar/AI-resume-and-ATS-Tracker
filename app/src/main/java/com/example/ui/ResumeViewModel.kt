package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.ContentPart
import com.example.api.GeminiRequest
import com.example.api.RetrofitClient
import com.example.api.TextPart
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class EngineState {
    object Idle : EngineState()
    object Loading : EngineState()
    data class Success(val msg: String) : EngineState()
    data class Error(val errorMsg: String) : EngineState()
}

class ResumeViewModel(private val repository: ResumeRepository) : ViewModel() {

    // Firestore Configurations (can be customized by users via UI)
    private val _firestoreProjectId = MutableStateFlow("aistudio-resume-coach")
    val firestoreProjectId: StateFlow<String> = _firestoreProjectId

    private val _firestoreApiKey = MutableStateFlow(BuildConfig.GEMINI_API_KEY)
    val firestoreApiKey: StateFlow<String> = _firestoreApiKey

    // AI Coach Settings
    private val _interviewerPersona = MutableStateFlow("Technical Architect")
    val interviewerPersona: StateFlow<String> = _interviewerPersona

    private val _targetDifficulty = MutableStateFlow("Senior Engineer")
    val targetDifficulty: StateFlow<String> = _targetDifficulty

    private val _detailedFeedback = MutableStateFlow(true)
    val detailedFeedback: StateFlow<Boolean> = _detailedFeedback

    fun setInterviewerPersona(persona: String) {
        _interviewerPersona.value = persona
    }

    fun setTargetDifficulty(difficulty: String) {
        _targetDifficulty.value = difficulty
    }

    fun setDetailedFeedback(enabled: Boolean) {
        _detailedFeedback.value = enabled
    }

    // Historical Firestore items
    private val _cloudAtsReports = MutableStateFlow<List<CloudAtsReport>>(emptyList())
    val cloudAtsReports: StateFlow<List<CloudAtsReport>> = _cloudAtsReports

    private val _cloudInterviewSummaries = MutableStateFlow<List<CloudInterviewSummary>>(emptyList())
    val cloudInterviewSummaries: StateFlow<List<CloudInterviewSummary>> = _cloudInterviewSummaries

    private val _firestoreSyncStatus = MutableStateFlow<EngineState>(EngineState.Idle)
    val firestoreSyncStatus: StateFlow<EngineState> = _firestoreSyncStatus

    // Personal Prep Notes
    val allNotes: StateFlow<List<PreparationNote>> = repository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun insertNote(title: String, content: String, category: String) {
        viewModelScope.launch {
            repository.insertNote(PreparationNote(title = title, content = content, category = category))
        }
    }

    fun updateNote(note: PreparationNote) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteNote(note: PreparationNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    init {
        fetchCloudAtsReports()
        fetchCloudInterviewSummaries()
    }

    fun getResolvedApiKey(): String {
        val apiKey = _firestoreApiKey.value.ifBlank { BuildConfig.GEMINI_API_KEY }
        return if (apiKey == "MY_GEMINI_API_KEY") "" else apiKey
    }

    // List of all user resume profiles
    val allProfiles: StateFlow<List<ResumeProfile>> = repository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current active resume profile
    private val _activeProfile = MutableStateFlow<ResumeProfile?>(null)
    val activeProfile: StateFlow<ResumeProfile?> = _activeProfile

    // Status state for API calls
    private val _apiStatus = MutableStateFlow<EngineState>(EngineState.Idle)
    val apiStatus: StateFlow<EngineState> = _apiStatus

    // Current screen navigation or view tab
    private val _currentTab = MutableStateFlow(0) // 0: ATS Scanner, 1: Mock Interview Coach, 2: Career Roadmap & Recruiter dashboard
    val currentTab: StateFlow<Int> = _currentTab

    // Active Mock Interview Sessions
    val allSessions: StateFlow<List<MockInterviewSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeSession = MutableStateFlow<MockInterviewSession?>(null)
    val activeSession: StateFlow<MockInterviewSession?> = _activeSession

    private val _sessionMessages = MutableStateFlow<List<InterviewMessage>>(emptyList())
    val sessionMessages: StateFlow<List<InterviewMessage>> = _sessionMessages

    // State for interactive user text in active interview
    private val _interviewLoading = MutableStateFlow(false)
    val interviewLoading: StateFlow<Boolean> = _interviewLoading

    fun setTab(tabId: Int) {
        _currentTab.value = tabId
    }

    fun selectProfile(profile: ResumeProfile?) {
        _activeProfile.value = profile
        // Clear status
        _apiStatus.value = EngineState.Idle
        // Auto select session for this profile if available
        if (profile != null) {
            viewModelScope.launch {
                val sessions = repository.allSessions.first()
                val existingSession = sessions.find { it.associatedProfileId == profile.id }
                if (existingSession != null) {
                    selectSession(existingSession)
                } else {
                    selectSession(null)
                }
            }
        } else {
            selectSession(null)
        }
    }

    fun selectActiveSession(session: MockInterviewSession?) {
        selectSession(session)
    }

    fun deleteInterviewSession(session: MockInterviewSession) {
        viewModelScope.launch {
            repository.clearMessagesForSession(session.id)
            repository.deleteSession(session)
            if (_activeSession.value?.id == session.id) {
                _activeSession.value = null
                _sessionMessages.value = emptyList()
            }
        }
    }

    private fun selectSession(session: MockInterviewSession?) {
        _activeSession.value = session
        if (session != null) {
            viewModelScope.launch {
                repository.getMessagesForSession(session.id).collect { messages ->
                    _sessionMessages.value = messages
                }
            }
        } else {
            _sessionMessages.value = emptyList()
        }
    }

    fun deleteProfile(profile: ResumeProfile) {
        viewModelScope.launch {
            if (_activeProfile.value?.id == profile.id) {
                _activeProfile.value = null
            }
            repository.deleteProfile(profile)
        }
    }

    // --- Core AI Actions ---

    // 1. ATS scanner and Resume Parser Agent
    fun analyzeResume(
        resumeText: String,
        targetJobTitle: String,
        jobDescription: String
    ) {
        _apiStatus.value = EngineState.Loading
        viewModelScope.launch {
            val apiKey = getResolvedApiKey()
            if (apiKey.isEmpty()) {
                _apiStatus.value = EngineState.Error("Gemini API Key is missing. Please add GEMINI_API_KEY in the Secrets tab, or tap the cloud configs icon in the ATS tab to enter an API key.")
                return@launch
            }

            val prompt = """
                You are a premium AI Recruitment Specialist and senior ATS Optimization Parser.
                Analyze the following resume against the given Job Description to produce parsing metrics and detailed structural feedback.

                TARGET JOB ROLE:
                $targetJobTitle

                JOB DESCRIPTION:
                $jobDescription

                RESUME SOURCE:
                $resumeText

                Provide your response strictly structured with matching section headers as follows:
                [ATS]
                Score: [number only between 0 and 100 representing compatibility]
                [MATCHING_SKILLS]
                [Comma separated list of matching skills found in resume]
                [MISSING_SKILLS]
                [Comma separated list of missing skills or gaps from target JOB DESCRIPTION]
                [EDUCATION]
                [1-2 bullet points summarizing extracted Education elements]
                [EXPERIENCE]
                [1-2 bullet points summarizing parsed Experience timeline]
                [FEEDBACK]
                [Identify structural issues, keyword deficiencies, formatting errors, or readability notes. Keep spacing elegant and detailed in standard points]
            """.trimIndent()

            try {
                val request = GeminiRequest(
                    contents = listOf(ContentPart(parts = listOf(TextPart(text = prompt))))
                )
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (!responseText.isNullOrBlank()) {
                    // Extract data based on headers
                    val atsScore = extractSection(responseText, "[ATS]", "[MATCHING_SKILLS]")
                        .replace("Score:", "").trim().toIntOrNull() ?: 60
                    val matchingSkills = extractSection(responseText, "[MATCHING_SKILLS]", "[MISSING_SKILLS]").trim()
                    val missingSkills = extractSection(responseText, "[MISSING_SKILLS]", "[EDUCATION]").trim()
                    val education = extractSection(responseText, "[EDUCATION]", "[EXPERIENCE]").trim()
                    val experience = extractSection(responseText, "[EXPERIENCE]", "[FEEDBACK]").trim()
                    val feedback = extractSection(responseText, "[FEEDBACK]", null).trim()

                    // Insert to database profile
                    val profile = ResumeProfile(
                        resumeText = resumeText,
                        targetJobTitle = targetJobTitle,
                        jobDescription = jobDescription,
                        atsScore = atsScore,
                        parsedSkills = matchingSkills,
                        missingSkills = missingSkills,
                        education = education,
                        experience = experience,
                        resumeStructureIssues = feedback
                    )
                    val profileId = repository.insertProfile(profile)
                    val insertedProfile = profile.copy(id = profileId.toInt())
                    _activeProfile.value = insertedProfile
                    _apiStatus.value = EngineState.Success("Scan Complete!")
                } else {
                    _apiStatus.value = EngineState.Error("Received empty result from ATS Scanner Engine.")
                }
            } catch (e: Exception) {
                _apiStatus.value = EngineState.Error("Network failure or API limit: ${e.localizedMessage}")
            }
        }
    }

    // 2. Career Learning Roadmap Coach Agent
    fun generateCareerRoadmap(profile: ResumeProfile) {
        _apiStatus.value = EngineState.Loading
        viewModelScope.launch {
            val apiKey = getResolvedApiKey()
            if (apiKey.isEmpty()) {
                _apiStatus.value = EngineState.Error("Gemini API Key is missing. Add your GEMINI_API_KEY in the Secrets tab or tap the cloud settings gear on Tab 0 to set it.")
                return@launch
            }

            val prompt = """
                You are a senior Developer Advocate and Technical Career Architect.
                Based on the candidate's target job role of "${profile.targetJobTitle}" and their skill gaps: "${profile.missingSkills}", generate a personalized learning roadmap.

                Structure the feedback exactly with sections:
                [ROADMAP]
                [Phase-by-phase actionable educational guide to acquire missing skills]
                [CERTIFICATIONS]
                [Top 3 highest value certifications that would prove competence in these missing fields]
                [PROJECTS]
                [2-3 custom engineered project ideas they can code to showcase this mastery to recruiters]
            """.trimIndent()

            try {
                val request = GeminiRequest(
                    contents = listOf(ContentPart(parts = listOf(TextPart(text = prompt))))
                )
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (!responseText.isNullOrBlank()) {
                    val roadmap = extractSection(responseText, "[ROADMAP]", "[CERTIFICATIONS]").trim()
                    val certs = extractSection(responseText, "[CERTIFICATIONS]", "[PROJECTS]").trim()
                    val projects = extractSection(responseText, "[PROJECTS]", null).trim()

                    val updatedProfile = profile.copy(
                        learningRoadmap = roadmap,
                        certificationSuggestions = certs,
                        projectSuggestions = projects
                    )
                    repository.updateProfile(updatedProfile)
                    _activeProfile.value = updatedProfile
                    _apiStatus.value = EngineState.Success("Career Roadmaps generated successfully!")
                } else {
                    _apiStatus.value = EngineState.Error("No content returned.")
                }
            } catch (e: Exception) {
                _apiStatus.value = EngineState.Error(e.localizedMessage ?: "Coach connection failed.")
            }
        }
    }

    // 3. Mock Interview Assistant Agent
    fun startMockInterview(type: String) {
        val profile = _activeProfile.value ?: return
        _interviewLoading.value = true
        viewModelScope.launch {
            val apiKey = getResolvedApiKey()
            if (apiKey.isEmpty()) {
                _interviewLoading.value = false
                return@launch
            }

            // Create new Interview Session
            val session = MockInterviewSession(
                associatedProfileId = profile.id,
                jobRole = profile.targetJobTitle,
                interviewType = type
            )
            val sessionId = repository.insertSession(session)
            val activeSes = session.copy(id = sessionId.toInt())
            _activeSession.value = activeSes

            // Request Gemini for the first interview question
            val persona = _interviewerPersona.value
            val targetDiff = _targetDifficulty.value
            val prompt = """
                You are a challenging $persona for a candidate targeting the role: "${profile.targetJobTitle}".
                The target seniority/difficulty level is: $targetDiff.
                Based on their parsed resume summary (Skills: ${profile.parsedSkills}, Experience: ${profile.experience}), please ask the FIRST challenging and realistic question for a $type interview round.
                Do not include any greeting or conversational filler. State the question directly. Limit under 60 words.
            """.trimIndent()

            try {
                val request = GeminiRequest(
                    contents = listOf(ContentPart(parts = listOf(TextPart(text = prompt))))
                )
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val questionText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Can you describe a challenging technical project you worked on, the obstacles you faced, and how you overcame them?"

                val welcomeMsg = InterviewMessage(
                    sessionId = activeSes.id,
                    sender = "AI",
                    messageText = questionText
                )
                repository.insertMessage(welcomeMsg)
                selectSession(activeSes)
            } catch (e: Exception) {
                // Insert a graceful local offline default question if API limits hit
                val welcomeMsg = InterviewMessage(
                    sessionId = activeSes.id,
                    sender = "AI",
                    messageText = "Let's start. Please describe your experience in backend design, and what software architecture achievements you are proudest of."
                )
                repository.insertMessage(welcomeMsg)
                selectSession(activeSes)
            } finally {
                _interviewLoading.value = false
            }
        }
    }

    fun submitInterviewAnswer(answerText: String, audioFilePath: String? = null) {
        val session = _activeSession.value ?: return
        if (answerText.isBlank()) return

        _interviewLoading.value = true
        viewModelScope.launch {
            val apiKey = getResolvedApiKey()
            // Save User Answer Msg to DB
            val userMsg = InterviewMessage(
                sessionId = session.id,
                sender = "USER",
                messageText = answerText,
                audioFilePath = audioFilePath
            )
            repository.insertMessage(userMsg)

            // Reload message history to compile the prompt
            val messages = repository.getMessagesForSession(session.id).first()
            _sessionMessages.value = messages

            val lastQuestion = messages.dropLast(1).lastOrNull { it.sender == "AI" }?.messageText
                ?: "Can you explain your technical background?"

            val persona = _interviewerPersona.value
            val targetDiff = _targetDifficulty.value
            val isDetailed = _detailedFeedback.value
            val feedbackDetailConstraint = if (isDetailed) {
                "Include a code snippet example or concrete keywords they should have mentioned to score higher."
            } else {
                "Keep the feedback direct, action-oriented, and short."
            }

            val evalPrompt = """
                You are an elite $persona and Career Evaluator assessing for a $targetDiff bar.
                Analyze the candidate's response to the interview question. Give constructive coaching feedback and a quantitative rating.
                $feedbackDetailConstraint

                QUESTION ASKED:
                $lastQuestion

                CANDIDATE'S ANSWER:
                $answerText

                Provide your feedback strictly formatted with sections as:
                [SCORE]
                [Rating integer from 1 to 10 on precision]
                [IMMEDIATE_FEEDBACK]
                [Highlight strength, key missed points, and bulletproof corrections]
                [NEXT_QUESTION]
                [Introduce the next challenging progressive follow-up question. Keep it fresh and relevant.]
            """.trimIndent()

            try {
                val request = GeminiRequest(
                    contents = listOf(ContentPart(parts = listOf(TextPart(text = evalPrompt))))
                )
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (!responseText.isNullOrBlank()) {
                    val scoreStr = extractSection(responseText, "[SCORE]", "[IMMEDIATE_FEEDBACK]").trim()
                    val score = scoreStr.toIntOrNull() ?: 7
                    val feedback = extractSection(responseText, "[IMMEDIATE_FEEDBACK]", "[NEXT_QUESTION]").trim()
                    val nextQuestion = extractSection(responseText, "[NEXT_QUESTION]", null).trim()

                    // Update the user message evaluation metadata
                    val updatedUserMsg = userMsg.copy(
                        evaluationScore = score,
                        evaluationFeedback = feedback
                    )
                    // Insert rating properties
                    val id = _sessionMessages.value.lastOrNull { it.sender == "USER" }?.id
                    if (id != null) {
                        repository.insertMessage(updatedUserMsg.copy(id = id))
                    }

                    // Insert next question message from AI
                    val nextMsg = InterviewMessage(
                        sessionId = session.id,
                        sender = "AI",
                        messageText = nextQuestion.ifBlank { "Could you discuss how you handle system failures under load?" }
                    )
                    repository.insertMessage(nextMsg)
                } else {
                    // Fallback to custom message if response is null
                    insertOfflineMockAIResponse(session, "Awesome response. Let's move on: What is your favorite approach for managing thread safety in Kotlin?")
                }
            } catch (e: Exception) {
                insertOfflineMockAIResponse(session, "Good overview! Let's build on that. What tools or strategies do you use to ensure your APIs handle scale gracefully?")
            } finally {
                _interviewLoading.value = false
                selectSession(session)
            }
        }
    }

    private suspend fun insertOfflineMockAIResponse(session: MockInterviewSession, nextQuestion: String) {
        val nextMsg = InterviewMessage(
            sessionId = session.id,
            sender = "AI",
            messageText = nextQuestion
        )
        repository.insertMessage(nextMsg)
    }

    fun resetInterviewSession() {
        val session = _activeSession.value ?: return
        viewModelScope.launch {
            repository.clearMessagesForSession(session.id)
            repository.deleteSession(session)
            _activeSession.value = null
            _sessionMessages.value = emptyList()
        }
    }

    fun exitActiveSession() {
        _activeSession.value = null
        _sessionMessages.value = emptyList()
    }

    // --- Delimiter Section Extractor ---
    private fun extractSection(text: String, startTag: String, endTag: String?): String {
        val startIndex = text.indexOf(startTag)
        if (startIndex == -1) return ""
        val contentStart = startIndex + startTag.length

        val contentEnd = if (endTag != null) {
            val endIndex = text.indexOf(endTag, contentStart)
            if (endIndex != -1) endIndex else text.length
        } else {
            text.length
        }
        return text.substring(contentStart, contentEnd).trim()
    }

    // --- Firestore Operations ---
    fun setFirestoreConfigs(projectId: String, apiKey: String) {
        _firestoreProjectId.value = projectId
        _firestoreApiKey.value = apiKey
        fetchCloudAtsReports()
        fetchCloudInterviewSummaries()
    }

    fun saveAtsReportToFirestore(profile: ResumeProfile) {
        viewModelScope.launch {
            _firestoreSyncStatus.value = EngineState.Loading
            val projectId = _firestoreProjectId.value.ifBlank { "aistudio-resume-coach" }
            val apiKey = getResolvedApiKey()

            if (apiKey.isEmpty()) {
                _firestoreSyncStatus.value = EngineState.Error("Web API Key is missing. Update cloud configs.")
                return@launch
            }

            try {
                val fields = mapOf(
                    "id" to com.example.api.FirestoreValue(stringValue = profile.id.toString()),
                    "targetJobTitle" to com.example.api.FirestoreValue(stringValue = profile.targetJobTitle),
                    "atsScore" to com.example.api.FirestoreValue(integerValue = profile.atsScore.toString()),
                    "missingSkills" to com.example.api.FirestoreValue(stringValue = profile.missingSkills),
                    "parsedSkills" to com.example.api.FirestoreValue(stringValue = profile.parsedSkills),
                    "timestamp" to com.example.api.FirestoreValue(integerValue = profile.timestamp.toString())
                )
                val request = com.example.api.FirestoreDocumentRequest(fields = fields)
                com.example.api.FirestoreRetrofitClient.service.createDocument(
                    projectId = projectId,
                    collectionId = "ats_reports",
                    apiKey = apiKey,
                    document = request
                )
                _firestoreSyncStatus.value = EngineState.Success("Successfully saved current ATS Score report to Cloud Firestore!")
                fetchCloudAtsReports()
            } catch (e: Exception) {
                _firestoreSyncStatus.value = EngineState.Error("Cloud Save Failed: ${e.localizedMessage}")
            }
        }
    }

    fun fetchCloudAtsReports() {
        viewModelScope.launch {
            val projectId = _firestoreProjectId.value.ifBlank { "aistudio-resume-coach" }
            val apiKey = _firestoreApiKey.value.ifBlank { BuildConfig.GEMINI_API_KEY }

            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return@launch

            try {
                val response = com.example.api.FirestoreRetrofitClient.service.listDocuments(
                    projectId = projectId,
                    collectionId = "ats_reports",
                    apiKey = apiKey
                )
                val reports = response.documents?.mapNotNull { doc ->
                    val fields = doc.fields ?: return@mapNotNull null
                    val id = fields["id"]?.stringValue ?: ""
                    val targetJobTitle = fields["targetJobTitle"]?.stringValue ?: "Unknown Title"
                    val atsScore = fields["atsScore"]?.integerValue?.toIntOrNull() ?: 0
                    val missingSkills = fields["missingSkills"]?.stringValue ?: ""
                    val parsedSkills = fields["parsedSkills"]?.stringValue ?: ""
                    val timestamp = fields["timestamp"]?.integerValue?.toLongOrNull() ?: System.currentTimeMillis()

                    CloudAtsReport(
                        id = id.ifBlank { doc.name?.substringAfterLast("/") ?: "" },
                        targetJobTitle = targetJobTitle,
                        atsScore = atsScore,
                        missingSkills = missingSkills,
                        parsedSkills = parsedSkills,
                        timestamp = timestamp,
                        docPath = doc.name
                    )
                } ?: emptyList()

                _cloudAtsReports.value = reports.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                // Squelch background errors
            }
        }
    }

    fun saveInterviewCompletedSummary(session: MockInterviewSession, messages: List<InterviewMessage>) {
        viewModelScope.launch {
            _firestoreSyncStatus.value = EngineState.Loading
            val projectId = _firestoreProjectId.value.ifBlank { "aistudio-resume-coach" }
            val apiKey = _firestoreApiKey.value.ifBlank { BuildConfig.GEMINI_API_KEY }

            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                _firestoreSyncStatus.value = EngineState.Error("Web API Key is missing. Update cloud configs.")
                return@launch
            }

            // Calculate metrics
            val userAnswers = messages.filter { it.sender == "USER" && it.evaluationScore != null }
            val totalQuestions = userAnswers.size
            val averageScore = if (userAnswers.isNotEmpty()) {
                userAnswers.mapNotNull { it.evaluationScore }.average()
            } else {
                0.0
            }
            val feedbackList = userAnswers.mapNotNull { it.evaluationFeedback }.take(3).joinToString("\n• ", prefix = "• ")
            val feedbackHighlights = if (feedbackList.trim() == "•") "No rated feedback recorded yet." else feedbackList

            try {
                val fields = mapOf(
                    "id" to com.example.api.FirestoreValue(stringValue = session.id.toString()),
                    "jobRole" to com.example.api.FirestoreValue(stringValue = session.jobRole),
                    "interviewType" to com.example.api.FirestoreValue(stringValue = session.interviewType),
                    "totalQuestions" to com.example.api.FirestoreValue(integerValue = totalQuestions.toString()),
                    "averageScore" to com.example.api.FirestoreValue(doubleValue = averageScore),
                    "feedbackHighlights" to com.example.api.FirestoreValue(stringValue = feedbackHighlights),
                    "timestamp" to com.example.api.FirestoreValue(integerValue = session.timestamp.toString())
                )
                val request = com.example.api.FirestoreDocumentRequest(fields = fields)
                com.example.api.FirestoreRetrofitClient.service.createDocument(
                    projectId = projectId,
                    collectionId = "interview_summaries",
                    apiKey = apiKey,
                    document = request
                )
                _firestoreSyncStatus.value = EngineState.Success("Successfully saved Mock Interview summary to Cloud Firestore!")
                fetchCloudInterviewSummaries()
            } catch (e: Exception) {
                _firestoreSyncStatus.value = EngineState.Error("Cloud Save Failed: ${e.localizedMessage}")
            }
        }
    }

    fun fetchCloudInterviewSummaries() {
        viewModelScope.launch {
            val projectId = _firestoreProjectId.value.ifBlank { "aistudio-resume-coach" }
            val apiKey = _firestoreApiKey.value.ifBlank { BuildConfig.GEMINI_API_KEY }

            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return@launch

            try {
                val response = com.example.api.FirestoreRetrofitClient.service.listDocuments(
                    projectId = projectId,
                    collectionId = "interview_summaries",
                    apiKey = apiKey
                )
                val summaries = response.documents?.mapNotNull { doc ->
                    val fields = doc.fields ?: return@mapNotNull null
                    val id = fields["id"]?.stringValue ?: ""
                    val jobRole = fields["jobRole"]?.stringValue ?: "Unknown Role"
                    val interviewType = fields["interviewType"]?.stringValue ?: "TECHNICAL"
                    val totalQuestions = fields["totalQuestions"]?.integerValue?.toIntOrNull() ?: 0
                    val averageScore = fields["averageScore"]?.doubleValue ?: 0.0
                    val feedbackHighlights = fields["feedbackHighlights"]?.stringValue ?: ""
                    val timestamp = fields["timestamp"]?.integerValue?.toLongOrNull() ?: System.currentTimeMillis()

                    CloudInterviewSummary(
                        id = id.ifBlank { doc.name?.substringAfterLast("/") ?: "" },
                        jobRole = jobRole,
                        interviewType = interviewType,
                        totalQuestions = totalQuestions,
                        averageScore = averageScore,
                        feedbackHighlights = feedbackHighlights,
                        timestamp = timestamp,
                        docPath = doc.name
                    )
                } ?: emptyList()

                _cloudInterviewSummaries.value = summaries.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                // Squelch background errors
            }
        }
    }

    fun deleteCloudAtsReport(reportId: String, docPath: String?) {
        viewModelScope.launch {
            val projectId = _firestoreProjectId.value.ifBlank { "aistudio-resume-coach" }
            val apiKey = _firestoreApiKey.value.ifBlank { BuildConfig.GEMINI_API_KEY }
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return@launch

            try {
                val path = docPath ?: "projects/$projectId/databases/(default)/documents/ats_reports/$reportId"
                com.example.api.FirestoreRetrofitClient.service.deleteDocument(
                    documentPath = path,
                    apiKey = apiKey
                )
                fetchCloudAtsReports()
            } catch (e: Exception) {
                // Ignore gracefully
            }
        }
    }

    fun deleteCloudInterviewSummary(summaryId: String, docPath: String?) {
        viewModelScope.launch {
            val projectId = _firestoreProjectId.value.ifBlank { "aistudio-resume-coach" }
            val apiKey = _firestoreApiKey.value.ifBlank { BuildConfig.GEMINI_API_KEY }
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return@launch

            try {
                val path = docPath ?: "projects/$projectId/databases/(default)/documents/interview_summaries/$summaryId"
                com.example.api.FirestoreRetrofitClient.service.deleteDocument(
                    documentPath = path,
                    apiKey = apiKey
                )
                fetchCloudInterviewSummaries()
            } catch (e: Exception) {
                // Ignore gracefully
            }
        }
    }

    fun clearSyncStatus() {
        _firestoreSyncStatus.value = EngineState.Idle
    }
}

class ResumeViewModelFactory(private val repository: ResumeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResumeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ResumeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
