package com.presenterremote.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotesScreen(
    currentSlide: Int,
    totalSlides: Int,
    pptNotes: String,
    userNotes: String,
    onUserNotesChanged: (slide: Int, text: String) -> Unit
) {
    var fontSize by remember { mutableStateOf(18f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (totalSlides > 0) "Slide $currentSlide / $totalSlides" else "Not connected",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            FilledTonalIconButton(
                onClick = { fontSize = (fontSize - 2f).coerceAtLeast(12f) }
            ) { Text("A-", fontSize = 14.sp) }
            Spacer(modifier = Modifier.width(4.dp))
            FilledTonalIconButton(
                onClick = { fontSize = (fontSize + 2f).coerceAtMost(32f) }
            ) { Text("A+", fontSize = 14.sp) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // PPT speaker notes
        Text(
            "Speaker Notes",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = pptNotes.ifEmpty { "No speaker notes for this slide" },
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.4f).sp,
            fontStyle = if (pptNotes.isEmpty()) FontStyle.Italic else FontStyle.Normal,
            color = if (pptNotes.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // User's own notes
        Text(
            "My Notes",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = userNotes,
            onValueChange = { if (currentSlide > 0) onUserNotesChanged(currentSlide, it) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            textStyle = TextStyle(fontSize = fontSize.sp, lineHeight = (fontSize * 1.4f).sp),
            placeholder = { Text("Write your notes here...") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            )
        )
    }
}
