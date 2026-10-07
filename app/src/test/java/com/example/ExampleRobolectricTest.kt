package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.ContactQrData
import com.example.domain.model.WifiQrData
import com.example.domain.util.PdfManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MTE Merge", appName)
    }

    @Test
    fun `pdf range parser handles single and range values`() {
        val ranges = PdfManager.parseRangeExpression("1-3, 5, 8-10", 12)
        assertEquals(3, ranges.size)
        assertEquals(listOf(1, 2, 3), ranges[0])
        assertEquals(listOf(5), ranges[1])
        assertEquals(listOf(8, 9, 10), ranges[2])
    }

    @Test
    fun `wifi qr data formats correctly`() {
        val wifi = WifiQrData(ssid = "OfficeWifi", password = "secretPassword123", security = "WPA")
        val formatted = wifi.toQrString()
        assertEquals("WIFI:T:WPA;S:OfficeWifi;P:secretPassword123;H:false;;", formatted)
    }

    @Test
    fun `vcard contact data formats correctly`() {
        val contact = ContactQrData(name = "John Doe", phone = "+1234567890", email = "john@example.com", org = "MTE Corp")
        val vcard = contact.toVCard()
        assertTrue(vcard.contains("FN:John Doe"))
        assertTrue(vcard.contains("TEL:+1234567890"))
        assertTrue(vcard.contains("EMAIL:john@example.com"))
        assertTrue(vcard.contains("ORG:MTE Corp"))
    }
}
