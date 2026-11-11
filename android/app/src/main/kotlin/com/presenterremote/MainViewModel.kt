package com.presenterremote

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.presenterremote.network.ConnectionStatus
import com.presenterremote.network.PresentationState
import com.presenterremote.network.WebSocketClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "settings")
private val Context.notesStore by preferencesDataStore(name = "slide_notes")

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = application.dataStore
    private val notesStore = application.notesStore
    private val keyLastIp = stringPreferencesKey("last_ip")
    private val keyLastPort = stringPreferencesKey("last_port")
    private val keyLastPin = stringPreferencesKey("last_pin")

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _presentationState = MutableStateFlow(PresentationState())
    val presentationState: StateFlow<PresentationState> = _presentationState

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _timerSeconds = MutableStateFlow(0L)
    val timerSeconds: StateFlow<Long> = _timerSeconds

    private val _timerRunning = MutableStateFlow(false)
    val timerRunning: StateFlow<Boolean> = _timerRunning

    private val _currentSlideNotes = MutableStateFlow("")
    val currentSlideNotes: StateFlow<String> = _currentSlideNotes

    private var timerJob: Job? = null
    private var lastLoadedSlide = -1

    private val wsClient = WebSocketClient(
        onStateUpdate = { state ->
            _presentationState.value = state
            if (state.slide != lastLoadedSlide && state.slide > 0) {
                lastLoadedSlide = state.slide
                viewModelScope.launch { loadNotesForSlide(state.slide) }
            }
        },
        onConnectionStatus = { status -> _connectionStatus.value = status },
        onError = { msg -> _errorMessage.value = msg }
    )

    fun connect(ip: String, port: Int, pin: String) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[keyLastIp] = ip
                prefs[keyLastPort] = port.toString()
                prefs[keyLastPin] = pin
            }
        }
        wsClient.connect(ip, port, pin)
    }

    fun disconnect() {
        wsClient.disconnect()
    }

    fun nextSlide() = wsClient.sendCommand("next")
    fun prevSlide() = wsClient.sendCommand("prev")
    fun startShow() = wsClient.sendCommand("start")
    fun stopShow() = wsClient.sendCommand("stop")
    fun toggleBlank() = wsClient.sendCommand("blank")

    fun sendMove(dx: Float, dy: Float) = wsClient.sendMove(dx, dy)
    fun sendClick(button: String = "left") = wsClient.sendClick(button)
    fun sendLaser(state: String) = wsClient.sendLaser(state)

    fun startTimer() {
        if (_timerRunning.value) return
        _timerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_timerRunning.value) {
                delay(1000)
                if (_timerRunning.value) _timerSeconds.value++
            }
        }
    }

    fun pauseTimer() {
        _timerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        _timerRunning.value = false
        timerJob?.cancel()
        _timerSeconds.value = 0
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // --- Slide notes (user-written, stored locally) ---

    private suspend fun loadNotesForSlide(slide: Int) {
        val key = stringPreferencesKey("slide_$slide")
        val notes = notesStore.data.map { it[key] ?: "" }.first()
        _currentSlideNotes.value = notes
    }

    fun saveSlideNotes(slide: Int, text: String) {
        _currentSlideNotes.value = text
        viewModelScope.launch {
            val key = stringPreferencesKey("slide_$slide")
            notesStore.edit { it[key] = text }
        }
    }

    fun loadNotesForSlideManual(slide: Int) {
        viewModelScope.launch { loadNotesForSlide(slide) }
    }

    suspend fun getAllNotes(): Map<Int, String> {
        val prefs = notesStore.data.first()
        val result = mutableMapOf<Int, String>()
        for (key in prefs.asMap().keys) {
            val name = key.name
            if (name.startsWith("slide_")) {
                val num = name.removePrefix("slide_").toIntOrNull() ?: continue
                val text = prefs[stringPreferencesKey(name)] ?: ""
                if (text.isNotBlank()) result[num] = text
            }
        }
        return result
    }

    suspend fun getLastIp(): String =
        dataStore.data.map { it[keyLastIp] ?: "" }.first()

    suspend fun getLastPort(): String =
        dataStore.data.map { it[keyLastPort] ?: "8765" }.first()

    suspend fun getLastPin(): String =
        dataStore.data.map { it[keyLastPin] ?: "" }.first()

    override fun onCleared() {
        super.onCleared()
        wsClient.disconnect()
    }
}
