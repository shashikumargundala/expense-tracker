package com.shashi.expensetracker.domain.inbox

import java.time.LocalDateTime

/**
 * One Dismissed row (AD-7, AD-13): an SMS fingerprint the Inbox hides, keyed by [smsId]. No SMS
 * text is kept. [dismissedOn] is minute-precision with no time zone (AD-19).
 */
data class Dismissal(val smsId: String, val dismissedOn: LocalDateTime)
