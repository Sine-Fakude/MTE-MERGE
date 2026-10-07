package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.util.P2pRole
import com.example.domain.util.P2pShareManager
import com.example.domain.util.P2pTransferState
import com.example.domain.util.P2pUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class P2pViewModel(application: Application) : AndroidViewModel(application) {

    val uiState: StateFlow<P2pUiState> = P2pShareManager.uiState

    fun startSending(context: Context, file: File) {
        P2pShareManager.startSender(context, file)
    }

    fun startReceiving(context: Context) {
        P2pShareManager.startReceiver(context)
    }

    fun cancel(context: Context) {
        P2pShareManager.reset(context)
    }

    override fun onCleared() {
        super.onCleared()
        P2pShareManager.stopAll(getApplication())
    }
}
