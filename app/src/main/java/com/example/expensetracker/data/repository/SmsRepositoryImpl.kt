package com.example.expensetracker.data.repository

import android.content.Context
import com.example.expensetracker.data.sms.SmsReader
import com.example.expensetracker.domain.model.RawSms
import com.example.expensetracker.domain.repository.SmsRepository
import com.example.expensetracker.util.PermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** [SmsRepository] backed by [SmsReader] and the package manager. */
@Singleton
class SmsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsReader: SmsReader
) : SmsRepository {

    override fun canReadSms(): Boolean = PermissionHelper.hasReadSmsPermission(context)

    override fun readSince(sinceMillis: Long): List<RawSms> = smsReader.readSince(sinceMillis)
}
