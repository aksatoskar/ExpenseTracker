package com.example.expensetracker.data.sms

import android.content.Context
import android.provider.Telephony
import com.example.expensetracker.domain.model.RawSms
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Reads bank/payment SMS bodies from the system inbox via [Telephony]. */
class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Returns up to [limit] inbox messages at/after [sinceMillis], oldest first. */
    fun readSince(sinceMillis: Long, limit: Int = 500, inclusive: Boolean = false): List<RawSms> {
        val results = mutableListOf<RawSms>()
        val projection = arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.ADDRESS)
        val comparator = if (inclusive) ">=" else ">"
        val selection = "${Telephony.Sms.DATE} $comparator ?"
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
                val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                if (bodyIndex < 0 || dateIndex < 0) return@use
                while (cursor.moveToNext() && results.size < limit) {
                    val body = cursor.getString(bodyIndex) ?: continue
                    val date = cursor.getLong(dateIndex)
                    val address = if (addressIndex >= 0) cursor.getString(addressIndex) else null
                    results.add(RawSms(body = body, timestamp = date, address = address))
                }
            }
        }
        return results
    }

    /** Returns up to [limit] inbox messages in `[startMillis, endMillis]`, oldest first. */
    fun readBetween(startMillis: Long, endMillis: Long, limit: Int = 5_000): List<RawSms> {
        val results = mutableListOf<RawSms>()
        val projection = arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.ADDRESS)
        val selection = "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} <= ?"
        val args = arrayOf(startMillis.toString(), endMillis.toString())
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
                val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                if (bodyIndex < 0 || dateIndex < 0) return@use
                while (cursor.moveToNext() && results.size < limit) {
                    val body = cursor.getString(bodyIndex) ?: continue
                    val date = cursor.getLong(dateIndex)
                    val address = if (addressIndex >= 0) cursor.getString(addressIndex) else null
                    results.add(RawSms(body = body, timestamp = date, address = address))
                }
            }
        }
        return results
    }
}
