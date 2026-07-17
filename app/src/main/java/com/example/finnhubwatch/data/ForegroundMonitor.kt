package com.example.finnhubwatch.data

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForegroundMonitor
    @Inject
    constructor(
        private val lifecycleOwner: LifecycleOwner,
    ) : DefaultLifecycleObserver {
        private val _isForeground = MutableStateFlow(false)
        val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()

        init {
            Handler(Looper.getMainLooper()).post {
                lifecycleOwner.lifecycle.addObserver(this)
            }
        }

        override fun onStart(owner: LifecycleOwner) {
            _isForeground.value = true
        }

        override fun onStop(owner: LifecycleOwner) {
            _isForeground.value = false
        }
    }
