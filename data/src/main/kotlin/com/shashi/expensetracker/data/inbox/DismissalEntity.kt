package com.shashi.expensetracker.data.inbox

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shashi.expensetracker.domain.inbox.Dismissal
import java.time.LocalDateTime

/** A mirrored Dismissed row: an SMS fingerprint the Inbox hides (AD-13). No SMS text is stored. */
@Entity(tableName = "dismissed")
data class DismissalEntity(
  @PrimaryKey @ColumnInfo(name = "sms_id") val smsId: String,
  @ColumnInfo(name = "dismissed_on") val dismissedOn: LocalDateTime,
)

fun DismissalEntity.toDomain(): Dismissal = Dismissal(smsId = smsId, dismissedOn = dismissedOn)

fun Dismissal.toEntity(): DismissalEntity = DismissalEntity(smsId = smsId, dismissedOn = dismissedOn)
