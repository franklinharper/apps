package com.franklinharper.whatsapp.settings.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.franklinharper.whatsapp.settings.domain.DetectionSource
import com.franklinharper.whatsapp.settings.domain.StatusTrackingRepository
import com.franklinharper.whatsapp.settings.domain.UnrestrictedSession
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus
import com.franklinharper.whatsapp.settings.domain.toPersistenceValue
import com.franklinharper.whatsapp.settings.domain.toWhatsAppStatusOrNull

class StatusTrackingDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION),
    StatusTrackingRepository {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE status_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestampMillis INTEGER NOT NULL,
                status TEXT NOT NULL,
                source TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE unrestricted_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                startTimestampMillis INTEGER NOT NULL,
                endTimestampMillis INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX index_status_history_timestamp ON status_history(timestampMillis)")
        db.execSQL("CREATE INDEX index_unrestricted_sessions_start ON unrestricted_sessions(startTimestampMillis)")
        db.execSQL("CREATE INDEX index_unrestricted_sessions_end ON unrestricted_sessions(endTimestampMillis)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS status_history")
        db.execSQL("DROP TABLE IF EXISTS unrestricted_sessions")
        onCreate(db)
    }

    override fun recordIfChanged(
        status: WhatsAppStatus,
        timestampMillis: Long,
        source: DetectionSource,
    ) {
        writableDatabase.transaction {
            val previousStatus = latestStatus()
            if (previousStatus == status) return@transaction

            insertStatusHistory(status, timestampMillis, source)
            updateSessions(previousStatus, status, timestampMillis)
        }
    }

    override fun getUnrestrictedSessionsNewestFirst(): List<UnrestrictedSession> {
        val sessions = mutableListOf<UnrestrictedSession>()
        readableDatabase.rawQuery(
            """
            SELECT id, startTimestampMillis, endTimestampMillis
            FROM unrestricted_sessions
            ORDER BY startTimestampMillis DESC, id DESC
            """.trimIndent(),
            emptyArray(),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                sessions += UnrestrictedSession(
                    id = cursor.getLong(0),
                    startTimestampMillis = cursor.getLong(1),
                    endTimestampMillis = if (cursor.isNull(2)) null else cursor.getLong(2),
                )
            }
        }
        return sessions
    }

    private fun SQLiteDatabase.latestStatus(): WhatsAppStatus? {
        rawQuery(
            """
            SELECT status
            FROM status_history
            ORDER BY timestampMillis DESC, id DESC
            LIMIT 1
            """.trimIndent(),
            emptyArray(),
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0).toWhatsAppStatusOrNull() else null
        }
    }

    private fun SQLiteDatabase.insertStatusHistory(
        status: WhatsAppStatus,
        timestampMillis: Long,
        source: DetectionSource,
    ) {
        insert(
            "status_history",
            null,
            ContentValues().apply {
                put("timestampMillis", timestampMillis)
                put("status", status.toPersistenceValue())
                put("source", source.name)
            },
        )
    }

    private fun SQLiteDatabase.updateSessions(
        previousStatus: WhatsAppStatus?,
        newStatus: WhatsAppStatus,
        timestampMillis: Long,
    ) {
        if (newStatus == WhatsAppStatus.BackgroundUsageUnrestricted) {
            closeOpenSessions(timestampMillis)
            insert(
                "unrestricted_sessions",
                null,
                ContentValues().apply {
                    put("startTimestampMillis", timestampMillis)
                    putNull("endTimestampMillis")
                },
            )
        } else if (previousStatus == WhatsAppStatus.BackgroundUsageUnrestricted) {
            closeOpenSessions(timestampMillis)
        }
    }

    private fun SQLiteDatabase.closeOpenSessions(endTimestampMillis: Long) {
        execSQL(
            """
            UPDATE unrestricted_sessions
            SET endTimestampMillis = ?
            WHERE endTimestampMillis IS NULL
            """.trimIndent(),
            arrayOf(endTimestampMillis),
        )
    }

    private inline fun SQLiteDatabase.transaction(block: SQLiteDatabase.() -> Unit) {
        beginTransaction()
        try {
            block()
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    companion object {
        private const val DATABASE_NAME = "status_tracking.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: StatusTrackingDatabase? = null

        fun getInstance(context: Context): StatusTrackingDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: StatusTrackingDatabase(context).also { INSTANCE = it }
        }
    }
}
