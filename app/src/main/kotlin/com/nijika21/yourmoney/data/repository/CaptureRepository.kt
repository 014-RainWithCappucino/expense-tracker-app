package com.nijika21.yourmoney.data.repository

import com.nijika21.yourmoney.data.db.dao.DiscoveredSourceDao
import com.nijika21.yourmoney.data.db.dao.RawNotificationDao
import com.nijika21.yourmoney.data.db.entity.RawNotificationEntity
import com.nijika21.yourmoney.data.db.entity.toDomain
import com.nijika21.yourmoney.domain.capture.ContentHasher
import com.nijika21.yourmoney.domain.capture.DiscoveredSource
import com.nijika21.yourmoney.domain.capture.SourceRegistry
import com.nijika21.yourmoney.domain.model.ParseState
import com.nijika21.yourmoney.domain.model.RawNotification
import com.nijika21.yourmoney.domain.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** What happened to one captured notification. Surfaced in diagnostics. */
enum class CaptureResult {
    /** Whitelisted package; content stored. */
    STORED,

    /** Whitelisted, but same sbn key + content hash already present (§3.6). */
    DUPLICATE,

    /** Not whitelisted; package name counted, content discarded unread. */
    DISCOVERED_ONLY,
}

/**
 * The write half of the ingestion pipeline (TDD §3.3).
 *
 * Raws are persisted **before** any parsing is attempted. Parsers will be
 * wrong, and banks change wording after app updates; with raws kept, fixing a
 * parser is a version bump plus a re-run over history. Without them, every
 * parser bug is permanent data loss.
 */
@Singleton
class CaptureRepository @Inject constructor(
    private val rawDao: RawNotificationDao,
    private val discoveredDao: DiscoveredSourceDao,
    private val time: TimeProvider,
) {

    suspend fun capture(
        packageName: String,
        sbnKey: String,
        title: String?,
        text: String?,
        bigText: String?,
        postTime: Long,
    ): CaptureResult {
        // The privacy boundary, checked first and before anything is written.
        // Only whitelisted packages ever have their content persisted, even
        // though the listener permission exposes the entire stream.
        if (!SourceRegistry.isCaptured(packageName)) {
            discoveredDao.record(packageName, time.nowMillis())
            return CaptureResult.DISCOVERED_ONLY
        }

        val contentHash = ContentHasher.hash(packageName, title, text, bigText)
        if (rawDao.existsBySbnKeyAndHash(sbnKey, contentHash)) {
            return CaptureResult.DUPLICATE
        }

        rawDao.insert(
            RawNotificationEntity(
                id = UUID.randomUUID().toString(),
                packageName = packageName,
                sbnKey = sbnKey,
                title = title,
                text = text,
                bigText = bigText,
                postTime = postTime,
                contentHash = contentHash,
                // BARU, not UNRECOGNIZED: no parser has looked at it yet.
                // The registry lands in M3 and will move these along.
                parseState = ParseState.BARU.name,
                parserId = null,
                receivedAt = time.nowMillis(),
            ),
        )
        return CaptureResult.STORED
    }

    fun observeRecent(limit: Int = 200): Flow<List<RawNotification>> =
        rawDao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

    fun observeCapturedCount(): Flow<Long> = rawDao.observeCount()

    fun observeDiscoveredSources(): Flow<List<DiscoveredSource>> =
        discoveredDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun clearDiscoveredSources() = discoveredDao.clear()
}
