package com.example.tabatatimer

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

class MainActivity : AppCompatActivity() {

    private var startupReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val contentView = findViewById<View>(android.R.id.content)

        contentView.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (!startupReady) {
                        return false
                    }

                    contentView.viewTreeObserver
                        .removeOnPreDrawListener(this)

                    return true
                }
            }
        )

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhaseColorsProvider {
                        TimerApp(
                            onStartupReady = {
                                startupReady = true
                            },
                            onCloseApp = {
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
}