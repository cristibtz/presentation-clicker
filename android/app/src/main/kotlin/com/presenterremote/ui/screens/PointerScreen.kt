package com.presenterremote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

enum class PointerMode { TRACKPAD, LASER }

@Composable
fun PointerScreen(
    onMove: (Float, Float) -> Unit,
    onClick: () -> Unit,
    onLaser: (String) -> Unit
) {
    var mode by remember { mutableStateOf(PointerMode.TRACKPAD) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = mode == PointerMode.TRACKPAD,
                onClick = { mode = PointerMode.TRACKPAD },
                label = { Text("Trackpad") },
                modifier = Modifier.padding(end = 8.dp)
            )
            FilterChip(
                selected = mode == PointerMode.LASER,
                onClick = { mode = PointerMode.LASER },
                label = { Text("Laser") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    when (mode) {
                        PointerMode.TRACKPAD -> Modifier
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onMove(dragAmount.x, dragAmount.y)
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { onClick() }
                                )
                            }
                        PointerMode.LASER -> Modifier
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { onLaser("down") },
                                    onDragEnd = { onLaser("up") },
                                    onDragCancel = { onLaser("up") },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onMove(dragAmount.x, dragAmount.y)
                                    }
                                )
                            }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (mode) {
                    PointerMode.TRACKPAD -> "Drag to move cursor\nTap to click"
                    PointerMode.LASER -> "Touch and drag for laser pointer"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
