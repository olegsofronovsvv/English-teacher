package ru.savvina.englishtutor

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class Screen { Login, Register, Home, Course, Lesson, Video, Exercise, Vocabulary, Profile }

internal data class TutorUiState(
    val screen: Screen? = null,
    val data: TutorData? = null,
    val selectedLessonId: Long? = null,
    val error: String? = null,
    val isRefreshing: Boolean = false,
) {
    val selectedLesson: LessonDto?
        get() = selectedLessonId?.let { id -> data?.lessons?.firstOrNull { it.id == id } }
            ?: data?.lessons?.firstOrNull()
}

internal class TutorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TutorUiState())
    val uiState: StateFlow<TutorUiState> = _uiState.asStateFlow()

    init {
        initializeSession()
    }

    private fun initializeSession() = viewModelScope.launch {
        val authReady = runCatching {
            SupabaseProvider.client.auth.awaitInitialization()
            if (SupabaseProvider.client.auth.currentSessionOrNull() != null) {
                SupabaseProvider.client.auth.refreshCurrentSession()
            }
        }.isSuccess

        if (authReady && SupabaseProvider.client.auth.currentSessionOrNull() != null) {
            _uiState.update { it.copy(screen = Screen.Home) }
            reloadData()
        } else {
            _uiState.value = TutorUiState(screen = Screen.Login)
        }
    }

    fun signedIn() {
        _uiState.value = TutorUiState(screen = Screen.Home, isRefreshing = true)
        viewModelScope.launch { reloadData() }
    }

    fun navigate(screen: Screen) {
        _uiState.update { it.copy(screen = screen, error = null) }
    }

    fun openLesson(lesson: LessonDto) {
        _uiState.update { it.copy(selectedLessonId = lesson.id, screen = Screen.Lesson, error = null) }
    }

    fun refresh() = viewModelScope.launch { reloadData() }

    fun completeLesson(lesson: LessonDto, correct: Int, total: Int) = viewModelScope.launch {
        val currentData = _uiState.value.data ?: return@launch
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        runCatching {
            TutorRepository.completeLesson(lesson, correct, total)
            val finishedLevel = currentData.lessons.all { item ->
                item.id == lesson.id || item.id in currentData.completedLessonIds
            }
            if (finishedLevel) {
                nextLevelCode(currentData.profile.levelCode)?.let { TutorRepository.updateLevel(it) }
            }
        }.onSuccess {
            reloadData(Screen.Home)
        }.onFailure(::showError)
    }

    fun setWordSaved(word: VocabularyDto, saved: Boolean) = viewModelScope.launch {
        runCatching { TutorRepository.setWordSaved(word.id, saved) }
            .onSuccess { reloadData() }
            .onFailure(::showError)
    }

    fun updateLevel(levelCode: String) = viewModelScope.launch {
        if (_uiState.value.data?.profile?.levelCode == levelCode) return@launch
        _uiState.update { it.copy(isRefreshing = true, selectedLessonId = null, error = null) }
        runCatching { TutorRepository.updateLevel(levelCode) }
            .onSuccess { reloadData(Screen.Profile) }
            .onFailure(::showError)
    }

    suspend fun updateBio(bio: String) {
        TutorRepository.updateBio(bio)
        reloadData()
    }

    suspend fun uploadAvatar(bytes: ByteArray, mimeType: String, extension: String) {
        TutorRepository.uploadAvatar(bytes, mimeType, extension)
        reloadData()
    }

    fun loggedOut() {
        _uiState.value = TutorUiState(screen = Screen.Login)
    }

    private suspend fun reloadData(destination: Screen? = null) {
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        runCatching { TutorRepository.load() }
            .onSuccess { loaded ->
                _uiState.update { current ->
                    current.copy(
                        screen = destination ?: current.screen ?: Screen.Home,
                        data = loaded,
                        selectedLessonId = current.selectedLessonId?.takeIf { id -> loaded.lessons.any { it.id == id } },
                        error = null,
                        isRefreshing = false,
                    )
                }
            }
            .onFailure(::showError)
    }

    private fun showError(error: Throwable) {
        Log.e("EnglishTutor", "Failed to update learning data", error)
        _uiState.update { it.copy(error = friendlyDataError(error), isRefreshing = false) }
    }
}
