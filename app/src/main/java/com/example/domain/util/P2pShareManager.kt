package com.example.domain.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.MteMergeApp
import com.example.data.repository.DocumentRepository
import com.example.domain.util.ShareManager
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

enum class P2pRole {
    NONE, SENDER, RECEIVER
}

enum class P2pTransferState {
    IDLE,
    ADVERTISING,
    DISCOVERING,
    CONNECTING,
    CONNECTED,
    TRANSFERRING,
    COMPLETED,
    ERROR
}

data class P2pUiState(
    val role: P2pRole = P2pRole.NONE,
    val state: P2pTransferState = P2pTransferState.IDLE,
    val statusMessage: String = "",
    val connectedDeviceName: String? = null,
    val fileName: String? = null,
    val fileSizeBytes: Long = 0L,
    val bytesTransferred: Long = 0L,
    val progress: Float = 0f,
    val completedFile: File? = null,
    val errorMessage: String? = null
)

object P2pShareManager {
    private const val TAG = "P2pShareManager"
    private const val SERVICE_ID = "com.example.mtemerge.p2p"
    private val STRATEGY = Strategy.P2P_POINT_TO_POINT

    private val _uiState = MutableStateFlow(P2pUiState())
    val uiState: StateFlow<P2pUiState> = _uiState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    private var fileToSend: File? = null
    private var outgoingFilePayloadId: Long? = null
    private val incomingMetadata = ConcurrentHashMap<Long, Pair<String, Long>>()
    private val incomingPayloads = ConcurrentHashMap<Long, Payload>()

    private val localDeviceName: String by lazy {
        val model = Build.MODEL ?: "Android"
        "MTE-$model"
    }

    /**
     * SENDER FLOW:
     * Starts advertising via Strategy.P2P_POINT_TO_POINT and waits for receiver connection.
     */
    fun startSender(context: Context, file: File) {
        val appContext = context.applicationContext
        fileToSend = file
        _uiState.update {
            P2pUiState(
                role = P2pRole.SENDER,
                state = P2pTransferState.ADVERTISING,
                statusMessage = "Advertising file '${file.name}'... Place devices close.",
                fileName = file.name,
                fileSizeBytes = file.length()
            )
        }

        val client = Nearby.getConnectionsClient(appContext)
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        client.startAdvertising(
            localDeviceName,
            SERVICE_ID,
            object : ConnectionLifecycleCallback() {
                override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                    Log.d(TAG, "Sender: Connection initiated with ${info.endpointName}")
                    _uiState.update {
                        it.copy(
                            state = P2pTransferState.CONNECTING,
                            connectedDeviceName = info.endpointName,
                            statusMessage = "Connecting with ${info.endpointName}..."
                        )
                    }
                    client.acceptConnection(endpointId, createPayloadCallback(appContext))
                }

                override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
                    if (resolution.status.isSuccess) {
                        Log.d(TAG, "Sender: Connected to $endpointId")
                        _uiState.update {
                            it.copy(
                                state = P2pTransferState.CONNECTED,
                                statusMessage = "Connected! Starting high-speed P2P transfer..."
                            )
                        }
                        sendFilePayload(appContext, endpointId, file)
                    } else {
                        Log.e(TAG, "Sender: Connection failed: ${resolution.status.statusCode}")
                        _uiState.update {
                            it.copy(
                                state = P2pTransferState.ERROR,
                                errorMessage = "Connection failed (${resolution.status.statusMessage ?: "Rejected"})"
                            )
                        }
                    }
                }

                override fun onDisconnected(endpointId: String) {
                    Log.d(TAG, "Sender: Disconnected from $endpointId")
                    if (_uiState.value.state != P2pTransferState.COMPLETED) {
                        _uiState.update {
                            it.copy(
                                state = P2pTransferState.ERROR,
                                errorMessage = "Device disconnected unexpectedly"
                            )
                        }
                    }
                }
            },
            advertisingOptions
        ).addOnFailureListener { e ->
            Log.e(TAG, "Failed to start advertising", e)
            _uiState.update {
                it.copy(
                    state = P2pTransferState.ERROR,
                    errorMessage = "Advertising error: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * RECEIVER FLOW:
     * Starts discovery via Strategy.P2P_POINT_TO_POINT to find advertising senders.
     */
    fun startReceiver(context: Context) {
        val appContext = context.applicationContext
        _uiState.update {
            P2pUiState(
                role = P2pRole.RECEIVER,
                state = P2pTransferState.DISCOVERING,
                statusMessage = "Searching for nearby sender devices with radar..."
            )
        }

        val client = Nearby.getConnectionsClient(appContext)
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        client.startDiscovery(
            SERVICE_ID,
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    Log.d(TAG, "Receiver: Found endpoint ${info.endpointName}, requesting connection")
                    _uiState.update {
                        it.copy(
                            state = P2pTransferState.CONNECTING,
                            connectedDeviceName = info.endpointName,
                            statusMessage = "Found ${info.endpointName}! Connecting..."
                        )
                    }

                    client.requestConnection(
                        localDeviceName,
                        endpointId,
                        object : ConnectionLifecycleCallback() {
                            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                                client.acceptConnection(endpointId, createPayloadCallback(appContext))
                            }

                            override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
                                if (resolution.status.isSuccess) {
                                    client.stopDiscovery()
                                    val devName = _uiState.value.connectedDeviceName ?: "Nearby Device"
                                    _uiState.update {
                                        it.copy(
                                            state = P2pTransferState.CONNECTED,
                                            statusMessage = "Connected to $devName! Waiting for file payload..."
                                        )
                                    }
                                } else {
                                    _uiState.update {
                                        it.copy(
                                            state = P2pTransferState.ERROR,
                                            errorMessage = "Could not establish secure link"
                                        )
                                    }
                                }
                            }

                            override fun onDisconnected(endpointId: String) {
                                if (_uiState.value.state != P2pTransferState.COMPLETED) {
                                    _uiState.update {
                                        it.copy(
                                            state = P2pTransferState.ERROR,
                                            errorMessage = "Device disconnected unexpectedly"
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                override fun onEndpointLost(endpointId: String) {
                    Log.d(TAG, "Receiver: Endpoint lost: $endpointId")
                }
            },
            discoveryOptions
        ).addOnFailureListener { e ->
            Log.e(TAG, "Failed to start discovery", e)
            _uiState.update {
                it.copy(
                    state = P2pTransferState.ERROR,
                    errorMessage = "Discovery error: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun sendFilePayload(context: Context, endpointId: String, file: File) {
        try {
            val client = Nearby.getConnectionsClient(context)
            val filePayload = Payload.fromFile(file)
            outgoingFilePayloadId = filePayload.id

            // Send metadata packet first: payloadId:fileName:fileSize
            val metadataBytes = "${filePayload.id}:${file.name}:${file.length()}".toByteArray(Charsets.UTF_8)
            val headerPayload = Payload.fromBytes(metadataBytes)

            client.sendPayload(endpointId, headerPayload)
            client.sendPayload(endpointId, filePayload)

            _uiState.update {
                it.copy(
                    state = P2pTransferState.TRANSFERRING,
                    statusMessage = "Sending '${file.name}'...",
                    fileName = file.name,
                    fileSizeBytes = file.length()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending payload", e)
            _uiState.update {
                it.copy(
                    state = P2pTransferState.ERROR,
                    errorMessage = "Failed to transmit file: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun createPayloadCallback(context: Context): PayloadCallback {
        return object : PayloadCallback() {
            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                when (payload.type) {
                    Payload.Type.BYTES -> {
                        val header = payload.asBytes()?.toString(Charsets.UTF_8) ?: return
                        val parts = header.split(":")
                        if (parts.size >= 3) {
                            val id = parts[0].toLongOrNull() ?: return
                            val name = parts[1]
                            val size = parts[2].toLongOrNull() ?: 0L
                            incomingMetadata[id] = Pair(name, size)
                            _uiState.update {
                                it.copy(
                                    fileName = name,
                                    fileSizeBytes = size,
                                    statusMessage = "Receiving '$name'..."
                                )
                            }
                        }
                    }
                    Payload.Type.FILE -> {
                        incomingPayloads[payload.id] = payload
                        _uiState.update {
                            it.copy(
                                state = P2pTransferState.TRANSFERRING,
                                statusMessage = "Receiving file stream..."
                            )
                        }
                    }
                }
            }

            override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                val total = update.totalBytes
                val transferred = update.bytesTransferred
                val progress = if (total > 0) (transferred.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

                when (update.status) {
                    PayloadTransferUpdate.Status.IN_PROGRESS -> {
                        _uiState.update {
                            it.copy(
                                state = P2pTransferState.TRANSFERRING,
                                bytesTransferred = transferred,
                                progress = progress,
                                statusMessage = if (it.role == P2pRole.SENDER) {
                                    "Sending... ${(progress * 100).toInt()}%"
                                } else {
                                    "Receiving... ${(progress * 100).toInt()}%"
                                }
                            )
                        }
                    }

                    PayloadTransferUpdate.Status.SUCCESS -> {
                        val payload = incomingPayloads[update.payloadId]
                        if (payload != null) {
                            // Receiver finished receiving file payload
                            handleReceivedFile(context, payload, update.payloadId)
                        } else if (_uiState.value.role == P2pRole.SENDER && update.payloadId == outgoingFilePayloadId) {
                            // Sender finished transmitting file
                            _uiState.update {
                                it.copy(
                                    state = P2pTransferState.COMPLETED,
                                    progress = 1f,
                                    statusMessage = "File sent successfully via P2P Direct!"
                                )
                            }
                            stopAll(context)
                        }
                    }

                    PayloadTransferUpdate.Status.CANCELED,
                    PayloadTransferUpdate.Status.FAILURE -> {
                        _uiState.update {
                            it.copy(
                                state = P2pTransferState.ERROR,
                                errorMessage = "Transfer interrupted or failed"
                            )
                        }
                        stopAll(context)
                    }
                }
            }
        }
    }

    private fun handleReceivedFile(context: Context, payload: Payload, payloadId: Long) {
        scope.launch {
            try {
                val meta = incomingMetadata[payloadId]
                val desiredName = meta?.first ?: "received_${System.currentTimeMillis()}.pdf"
                val receivedDir = File(context.cacheDir, "p2p_received").apply { mkdirs() }
                val targetFile = File(receivedDir, "${System.currentTimeMillis()}_$desiredName")

                val tempFile = payload.asFile()?.asJavaFile()
                if (tempFile != null && tempFile.exists()) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                } else {
                    payload.asFile()?.asParcelFileDescriptor()?.let { pfd ->
                        FileInputStream(pfd.fileDescriptor).use { input ->
                            FileOutputStream(targetFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }

                // Register received file into Room database history
                try {
                    val repo = MteMergeApp.instance.documentRepository
                    val uri = ShareManager.getFileUri(context, targetFile).toString()
                    repo.recordOperation(
                        fileName = desiredName,
                        filePath = targetFile.absolutePath,
                        uriString = uri,
                        operationType = "P2P_SHARE",
                        fileSizeBytes = targetFile.length(),
                        pageCount = 1
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Could not insert history record for received file", e)
                }

                _uiState.update {
                    it.copy(
                        state = P2pTransferState.COMPLETED,
                        progress = 1f,
                        completedFile = targetFile,
                        statusMessage = "Document received and saved to local storage!"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist received file", e)
                _uiState.update {
                    it.copy(
                        state = P2pTransferState.ERROR,
                        errorMessage = "Error saving received document: ${e.localizedMessage}"
                    )
                }
            } finally {
                stopAll(context)
            }
        }
    }

    fun stopAll(context: Context) {
        try {
            val client = Nearby.getConnectionsClient(context.applicationContext)
            client.stopAdvertising()
            client.stopDiscovery()
            client.stopAllEndpoints()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping nearby connections", e)
        }
    }

    fun reset(context: Context) {
        stopAll(context)
        fileToSend = null
        outgoingFilePayloadId = null
        incomingMetadata.clear()
        incomingPayloads.clear()
        _uiState.value = P2pUiState()
    }
}
