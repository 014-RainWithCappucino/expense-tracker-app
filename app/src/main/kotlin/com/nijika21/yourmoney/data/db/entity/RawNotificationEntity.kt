package com.nijika21.yourmoney.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nijika21.yourmoney.domain.model.ParseState
import com.nijika21.yourmoney.domain.model.RawNotification

@Entity(
    tableName = "raw_notification",
    indices = [
        Index(value = ["contentHash"]),
        Index(value = ["packageName", "postTime"]),
        Index(value = ["parseState"]),
        Index(value = ["sbnKey"]),
    ],
)
data class RawNotificationEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val sbnKey: String,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val postTime: Long,
    val contentHash: String,
    val parseState: String,
    val parserId: String? = null,
    val receivedAt: Long,
)

fun RawNotificationEntity.toDomain(): RawNotification = RawNotification(
    id = id,
    packageName = packageName,
    sbnKey = sbnKey,
    title = title,
    text = text,
    bigText = bigText,
    postTime = postTime,
    contentHash = contentHash,
    parseState = ParseState.valueOf(parseState),
    parserId = parserId,
    receivedAt = receivedAt,
)

fun RawNotification.toEntity(): RawNotificationEntity = RawNotificationEntity(
    id = id,
    packageName = packageName,
    sbnKey = sbnKey,
    title = title,
    text = text,
    bigText = bigText,
    postTime = postTime,
    contentHash = contentHash,
    parseState = parseState.name,
    parserId = parserId,
    receivedAt = receivedAt,
)
