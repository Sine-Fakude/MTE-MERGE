package com.example.domain.util

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import com.example.domain.nfc.KeyEmulatorService
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.random.Random

object NfcKeyManager {
    private const val TAG = "NfcKeyManager"

    fun isNfcSupported(context: Context): Boolean {
        return NfcAdapter.getDefaultAdapter(context) != null
    }

    fun isNfcEnabled(context: Context): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return false
        return adapter.isEnabled
    }

    /**
     * Extracts raw hardware UID bytes from physical NFC tags and computes
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
     * Derives a 256-bit AES key from an HCE secondary phone's UUID token.
     */
    fun deriveAesKeyFromUuid(uuidString: String): Pair<String, String> {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(uuidString.toByteArray(StandardCharsets.UTF_8))
        val sha256Hex = hashBytes.joinToString("") { "%02x".format(it) }
        return Pair("HCE-KEY:${uuidString.take(10)}…", sha256Hex)
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
     * Uses NfcAdapter.enableReaderMode to detect NFC tags and HCE devices without
     * triggering Android's default system sounds or dialog popups.
     * Attempts IsoDep APDU communication first, then falls back to physical UID.
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
                // 1. Try IsoDep Device-to-Device HCE Key communication
                val isoDep = IsoDep.get(tag)
                if (isoDep != null) {
                    try {
                        isoDep.connect()
                        isoDep.timeout = 5000
                        val response = isoDep.transceive(KeyEmulatorService.SELECT_AID_APDU)
                        isoDep.close()

                        if (response != null && response.size >= 2) {
                            val sw1 = response[response.size - 2]
                            val sw2 = response[response.size - 1]
                            if (sw1 == 0x90.toByte() && sw2 == 0x00.toByte()) {
                                val uuidBytes = response.copyOfRange(0, response.size - 2)
                                val uuidString = String(uuidBytes, StandardCharsets.UTF_8)
                                val (rawUid, key) = deriveAesKeyFromUuid(uuidString)
                                activity.runOnUiThread {
                                    onTagDetected(rawUid, key)
                                }
                                return@enableReaderMode
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "IsoDep connection did not match HCE AID: ${e.message}")
                    }
                }

                // 2. Fallback: Physical Tag UID (rings, transit cards, fobs)
                val uid = tag.id
                if (uid != null && uid.isNotEmpty()) {
                    val (rawUid, key) = deriveAesKey(uid)
                    activity.runOnUiThread {
                        onTagDetected(rawUid, key)
                    }
                } else {
                    activity.runOnUiThread {
                        onError("NFC tag did not return a valid identifier")
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
