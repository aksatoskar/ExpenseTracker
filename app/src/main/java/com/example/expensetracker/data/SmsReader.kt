package com.example.expensetracker.data

import android.content.Context
import android.provider.Telephony

data class RawSms(val body: String, val timestamp: Long)

class SmsReader(private val context: Context) {

    fun readSince(sinceMillis: Long, limit: Int = 500): List<RawSms> {
        val results = mutableListOf<RawSms>()
        val projection = arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE)
        val selection = "${Telephony.Sms.DATE} > ?"
        val args = arrayOf(sinceMillis.toString())
        val sortOrder = "${Telephony.Sms.DATE} ASC"

        runCatching {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                args,
                sortOrder
            )?.use { cursor ->
                val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)
                if (bodyIndex < 0 || dateIndex < 0) return@use
                while (cursor.moveToNext() && results.size < limit) {
                    val body = cursor.getString(bodyIndex) ?: continue
                    val date = cursor.getLong(dateIndex)
                    results.add(RawSms(body, date))
                }
            }
        }
        return results
    }
}
