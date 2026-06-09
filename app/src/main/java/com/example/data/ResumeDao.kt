package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeDao {
    // --- Resume Profile Operations ---
    @Query("SELECT * FROM resume_profiles ORDER BY timestamp DESC")
    fun getAllProfiles(): Flow<List<ResumeProfile>>

    @Query("SELECT * FROM resume_profiles WHERE id = :id LIMIT 1")
    fun getProfileById(id: Int): Flow<ResumeProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ResumeProfile): Long

    @Update
    suspend fun updateProfile(profile: ResumeProfile)

    @Delete
    suspend fun deleteProfile(profile: ResumeProfile)

    @Query("DELETE FROM resume_profiles")
    suspend fun clearAllProfiles()


    // --- Interview Session Operations ---
    @Query("SELECT * FROM mock_interview_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<MockInterviewSession>>

    @Query("SELECT * FROM mock_interview_sessions WHERE id = :id LIMIT 1")
    fun getSessionById(id: Int): Flow<MockInterviewSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: MockInterviewSession): Long

    @Update
    suspend fun updateSession(session: MockInterviewSession)

    @Delete
    suspend fun deleteSession(session: MockInterviewSession)


    // --- Interview Message Operations ---
    @Query("SELECT * FROM interview_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Int): Flow<List<InterviewMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: InterviewMessage): Long

    @Query("DELETE FROM interview_messages WHERE sessionId = :sessionId")
    suspend fun clearMessagesForSession(sessionId: Int)

    // --- Preparation Notes Operations ---
    @Query("SELECT * FROM preparation_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<PreparationNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: PreparationNote): Long

    @Update
    suspend fun updateNote(note: PreparationNote)

    @Delete
    suspend fun deleteNote(note: PreparationNote)
}
