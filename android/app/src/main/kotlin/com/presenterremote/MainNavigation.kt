package com.presenterremote

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.presenterremote.network.ConnectionStatus
import com.presenterremote.ui.screens.ConnectionScreen
import com.presenterremote.ui.screens.NotesScreen
import com.presenterremote.ui.screens.PointerScreen
import com.presenterremote.ui.screens.RemoteScreen

private data class Tab(val title: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Remote", Icons.Default.Slideshow),
    Tab("Notes", Icons.AutoMirrored.Filled.Notes),
    Tab("Pointer", Icons.Default.TouchApp)
)

@Composable
fun MainNavigation(viewModel: MainViewModel) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val presState by viewModel.presentationState.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val timerRunning by viewModel.timerRunning.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (connectionStatus != ConnectionStatus.CONNECTED) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                ConnectionScreen(viewModel = viewModel, connectionStatus = connectionStatus)
            }
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> RemoteScreen(
                    state = presState,
                    timerSeconds = timerSeconds,
                    timerRunning = timerRunning,
                    onNext = viewModel::nextSlide,
                    onPrev = viewModel::prevSlide,
                    onStart = viewModel::startShow,
                    onStop = viewModel::stopShow,
                    onBlank = viewModel::toggleBlank,
                    onTimerStart = viewModel::startTimer,
                    onTimerPause = viewModel::pauseTimer,
                    onTimerReset = viewModel::resetTimer
                )
                1 -> {
                    val userNotes by viewModel.currentSlideNotes.collectAsState()
                    NotesScreen(
                        currentSlide = presState.slide,
                        totalSlides = presState.total,
                        pptNotes = presState.notes,
                        userNotes = userNotes,
                        onUserNotesChanged = viewModel::saveSlideNotes
                    )
                }
                2 -> PointerScreen(
                    onMove = viewModel::sendMove,
                    onClick = { viewModel.sendClick() },
                    onLaser = viewModel::sendLaser
                )
            }
        }
    }
}
