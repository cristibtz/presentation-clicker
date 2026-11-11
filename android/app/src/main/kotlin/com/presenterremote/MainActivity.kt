package com.presenterremote

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.presenterremote.network.ConnectionStatus
import com.presenterremote.ui.theme.PresenterRemoteTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setContent {
            PresenterRemoteTheme {
                MainNavigation(viewModel = viewModel)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (viewModel.connectionStatus.value == ConnectionStatus.CONNECTED) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    viewModel.prevSlide()
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    viewModel.nextSlide()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
