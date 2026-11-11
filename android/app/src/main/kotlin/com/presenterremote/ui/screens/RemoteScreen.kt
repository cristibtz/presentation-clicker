package com.presenterremote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.presenterremote.network.PresentationState

@Composable
fun RemoteScreen(
    state: PresentationState,
    timerSeconds: Long,
    timerRunning: Boolean,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onBlank: () -> Unit,
    onTimerStart: () -> Unit,
    onTimerPause: () -> Unit,
    onTimerReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Slide counter
        Text(
            text = if (state.total > 0) "${state.slide} / ${state.total}" else "—",
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = when {
                !state.showRunning && state.total == 0 -> "No presentation"
                !state.showRunning -> "Show not running"
                state.blank -> "Screen blanked"
                else -> "Show running"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                state.blank -> MaterialTheme.colorScheme.error
                state.showRunning -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Next / Prev buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPrev,
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = "Previous",
                        modifier = Modifier.size(40.dp)
                    )
                    Text("Prev", fontSize = 16.sp)
                }
            }
            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(40.dp)
                    )
                    Text("Next", fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Start/Stop + Blank
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.showRunning) {
                FilledTonalButton(
                    onClick = onStop,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Stop Show") }
            } else {
                FilledTonalButton(
                    onClick = onStart,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Start Show") }
            }
            OutlinedButton(
                onClick = onBlank,
                modifier = Modifier.weight(1f).height(48.dp),
                colors = if (state.blank)
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                else ButtonDefaults.outlinedButtonColors()
            ) { Text(if (state.blank) "Unblank" else "Blank") }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Timer
        val minutes = timerSeconds / 60
        val seconds = timerSeconds % 60
        Text(
            text = "%02d:%02d".format(minutes, seconds),
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (timerRunning) {
                FilledTonalButton(onClick = onTimerPause) { Text("Pause") }
            } else {
                FilledTonalButton(onClick = onTimerStart) { Text("Start") }
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(onClick = onTimerReset) { Text("Reset") }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
