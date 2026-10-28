package com.shashi.expensetracker.domain.account

/**
 * An opaque request for the user's consent to Drive access. `:data` implements it around the
 * platform's prompt; `:domain` and `:app` only carry it back to the [AccountGateway].
 */
interface ConsentRequest
