package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resume_profiles")
data class ResumeProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val resumeText: String,
    val targetJobTitle: String,
    val jobDescription: String,
    // Calculated insights with defaults
    val atsScore: Int = 0,
    val parsedSkills: String = "",
    val education: String = "",
    val experience: String = "",
    val missingSkills: String = "",
    val resumeStructureIssues: String = "",
    val learningRoadmap: String = "",
    val certificationSuggestions: String = "",
    val projectSuggestions: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "mock_interview_sessions")
data class MockInterviewSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val associatedProfileId: Int,
    val jobRole: String,
    val interviewType: String, // "TECHNICAL", "HR", "BEHAVIORAL", "COMPANY_SPECIFIC"
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "interview_messages")
data class InterviewMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val sender: String, // "AI" or "USER"
    val messageText: String,
    val evaluationScore: Int? = null, // score out of 10 for user answer
    val evaluationFeedback: String? = null, // coaching tips
    val audioFilePath: String? = null, // Path to local recorded audio response
    val timestamp: Long = System.currentTimeMillis()
)

data class CloudAtsReport(
    val id: String,
    val targetJobTitle: String,
    val atsScore: Int,
    val missingSkills: String,
    val parsedSkills: String,
    val timestamp: Long,
    val docPath: String? = null
)

data class CloudInterviewSummary(
    val id: String,
    val jobRole: String,
    val interviewType: String,
    val totalQuestions: Int,
    val averageScore: Double,
    val feedbackHighlights: String,
    val timestamp: Long,
    val docPath: String? = null
)

@Entity(tableName = "preparation_notes")
data class PreparationNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: String = "General", // "General", "Interview Q", "Resume Tip", "Roadmap Goal"
    val timestamp: Long = System.currentTimeMillis()
)


