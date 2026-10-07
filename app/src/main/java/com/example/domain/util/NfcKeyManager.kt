package com.example.domain.util

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import java.security.MessageDigest
import kotlin.random.Random

object NfcKeyManager {

    fun isNfcSupported(context: Context): Boolean {
        return NfcAdapter.getDefaultAdapter(context) != null
    }

    fun isNfcEnabled(context: Context): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return false
        return adapter.isEnabled
    }

    /**
     * Extracts raw hardware UID bytes from android.nfc.Tag and computes
     * SHA-256 hash to derive a secure 256-bit AES encryption key.
     */
    fun deriveAesKey(uidBytes: ByteArray): Pair<String, String> {
        val rawUidHex = uidBytes.joinToString(":") { "%02X".format(it) }
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(uidBytes)
        val sha256Hex = hashBytes.joinToString("") { "%02x".format(it) }
        return Pair(rawUidHex, sha256Hex)
    }

    /**
     * Generates a deterministic or simulated tag UID for testing/simulation
     * on environments without physical NFC tags or emulators.
     */
    fun generateSimulatedTag(): Pair<String, String> {
        val simulatedBytes = ByteArray(7)
        Random.nextBytes(simulatedBytes)
        return deriveAesKey(simulatedBytes)
    }

    /**
     * Uses NfcAdapter.enableReaderMode to detect NFC tags without triggering
     * Android's default system sounds or dialog popups.
     */
    fun startReaderMode(
        activity: Activity,
        onTagDetected: (rawUidHex: String, derivedKey: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val adapter = NfcAdapter.getDefaultAdapter(activity)
        if (adapter == null) {
            onError("NFC is not supported on this device")
            return
        }
        if (!adapter.isEnabled) {
            onError("NFC is disabled in system settings")
            return
        }

        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        try {
            adapter.enableReaderMode(activity, { tag: Tag ->
                val uid = tag.id
                if (uid != null && uid.isNotEmpty()) {
                    val (rawUid, key) = deriveAesKey(uid)
                    activity.runOnUiThread {
                        onTagDetected(rawUid, key)
                    }
                } else {
                    activity.runOnUiThread {
                        onError("NFC tag did not return a valid hardware UID")
                    }
                }
            }, flags, Bundle())
        } catch (e: Exception) {
            onError("Failed to activate NFC reader: ${e.localizedMessage}")
        }
    }

    fun stopReaderMode(activity: Activity) {
        try {
            val adapter = NfcAdapter.getDefaultAdapter(activity)
            adapter?.disableReaderMode(activity)
        } catch (_: Exception) {}
    }
}
