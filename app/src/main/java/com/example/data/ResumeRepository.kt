package com.example.data

import kotlinx.coroutines.flow.Flow

class ResumeRepository(private val resumeDao: ResumeDao) {
    val allProfiles: Flow<List<ResumeProfile>> = resumeDao.getAllProfiles()
    val allSessions: Flow<List<MockInterviewSession>> = resumeDao.getAllSessions()

    fun getProfileById(id: Int): Flow<ResumeProfile?> {
        return resumeDao.getProfileById(id)
    }

    suspend fun insertProfile(profile: ResumeProfile): Long {
        return resumeDao.insertProfile(profile)
    }

    suspend fun updateProfile(profile: ResumeProfile) {
        resumeDao.updateProfile(profile)
    }

    suspend fun deleteProfile(profile: ResumeProfile) {
        resumeDao.deleteProfile(profile)
    }

    suspend fun clearAllProfiles() {
        resumeDao.clearAllProfiles()
    }

    fun getSessionById(id: Int): Flow<MockInterviewSession?> {
        return resumeDao.getSessionById(id)
    }

    suspend fun insertSession(session: MockInterviewSession): Long {
        return resumeDao.insertSession(session)
    }

    suspend fun updateSession(session: MockInterviewSession) {
        resumeDao.updateSession(session)
    }

    suspend fun deleteSession(session: MockInterviewSession) {
        resumeDao.deleteSession(session)
    }

    fun getMessagesForSession(sessionId: Int): Flow<List<InterviewMessage>> {
        return resumeDao.getMessagesForSession(sessionId)
    }

    suspend fun insertMessage(message: InterviewMessage): Long {
        return resumeDao.insertMessage(message)
    }

    suspend fun clearMessagesForSession(sessionId: Int) {
        resumeDao.clearMessagesForSession(sessionId)
    }

    // --- Preparation Notes ---
    val allNotes: Flow<List<PreparationNote>> = resumeDao.getAllNotes()

    suspend fun insertNote(note: PreparationNote): Long {
        return resumeDao.insertNote(note)
    }

    suspend fun updateNote(note: PreparationNote) {
        resumeDao.updateNote(note)
    }

    suspend fun deleteNote(note: PreparationNote) {
        resumeDao.deleteNote(note)
    }
}
