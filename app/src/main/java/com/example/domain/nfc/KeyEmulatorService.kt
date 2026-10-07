package com.example.domain.nfc

import android.content.Context
import android.content.SharedPreferences
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import java.nio.charset.StandardCharsets
import java.util.UUID

class KeyEmulatorService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) return STATUS_FAILED

        Log.d(TAG, "processCommandApdu: received ${commandApdu.toHex()}")

        if (isSelectAidApdu(commandApdu)) {
            Log.d(TAG, "SELECT AID command recognized for MTE Merge")
            val uuid = getOrCreateDeviceKeyUuid(applicationContext)
            val uuidBytes = uuid.toByteArray(StandardCharsets.UTF_8)
            // Respond with UUID payload + 0x90 0x00 (SUCCESS)
            val response = ByteArray(uuidBytes.size + 2)
            System.arraycopy(uuidBytes, 0, response, 0, uuidBytes.size)
            response[response.size - 2] = 0x90.toByte()
            response[response.size - 1] = 0x00.toByte()
            return response
        }

        return STATUS_FAILED
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "onDeactivated: reason = $reason")
    }

    private fun isSelectAidApdu(apdu: ByteArray): Boolean {
        if (apdu.size < SELECT_AID_HEADER.size + AID_BYTES.size) return false
        // Header match: 00 A4 04 00
        for (i in SELECT_AID_HEADER.indices) {
            if (apdu[i] != SELECT_AID_HEADER[i]) return false
        }
        // Length field (index 4) should match AID length
        if (apdu[4] != AID_BYTES.size.toByte()) return false
        // AID bytes match
        for (i in AID_BYTES.indices) {
            if (apdu[5 + i] != AID_BYTES[i]) return false
        }
        return true
    }

    companion object {
        private const val TAG = "KeyEmulatorService"
        private const val PREFS_NAME = "mte_hce_prefs"
        private const val KEY_UUID = "hce_device_uuid"

        const val AID_STRING = "F0394148148100"

        val AID_BYTES = byteArrayOf(
            0xF0.toByte(), 0x39.toByte(), 0x41.toByte(), 0x48.toByte(),
            0x14.toByte(), 0x81.toByte(), 0x00.toByte()
        )

        private val SELECT_AID_HEADER = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte()
        )

        // Select AID APDU: 00 A4 04 00 07 F0 39 41 48 14 81 00 00
        val SELECT_AID_APDU = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(),
            0x07.toByte(),
            0xF0.toByte(), 0x39.toByte(), 0x41.toByte(), 0x48.toByte(),
            0x14.toByte(), 0x81.toByte(), 0x00.toByte(),
            0x00.toByte()
        )

        private val STATUS_FAILED = byteArrayOf(0x6F.toByte(), 0x00.toByte())

        fun getOrCreateDeviceKeyUuid(context: Context): String {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var uuid = prefs.getString(KEY_UUID, null)
            if (uuid == null) {
                uuid = UUID.randomUUID().toString().replace("-", "")
                prefs.edit().putString(KEY_UUID, uuid).apply()
            }
            return uuid
        }

        private fun ByteArray.toHex(): String {
            return joinToString("") { "%02X".format(it) }
        }
    }
}
