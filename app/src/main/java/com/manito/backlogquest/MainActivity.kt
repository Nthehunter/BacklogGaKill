package com.manito.backlogquest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.manito.backlogquest.ui.BacklogQuestApp
import com.manito.backlogquest.ui.theme.BacklogQuestTheme
import com.manito.backlogquest.data.NintendoAuthCallbackBus

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NintendoAuthCallbackBus.publish(intent?.dataString)
        enableEdgeToEdge()
        setContent {
            BacklogQuestTheme {
                BacklogQuestApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NintendoAuthCallbackBus.publish(intent.dataString)
    }
}
