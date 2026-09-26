package com.akari.retailer.features.customer.domain.models

/**
 * Aging bucket — how long an unpaid balance has been outstanding.
 */
enum class AgingBucket(
    val minDays: Int,
    val maxDays: Int
) {
    /** 0–30 days old */
    CURRENT(0, 30),

    /** 31–60 days old */
    DAYS_30(31, 60),

    /** 61–90 days old */
    DAYS_60(61, 90),

    /** 91+ days old */
    DAYS_90(91, Int.MAX_VALUE);

    fun contains(days: Int): Boolean = days in minDays..maxDays
}

/**
 * A single unpaid line item (credit sale or purchase on credit),
 * aged by days outstanding.
 *
 * "Party" is either a customer (for receivables) or a supplier (for payables).
 */
data class AgedItem(
    val partyId: String,
    val partyName: String,
    val transactionId: String,
    val amount: Int,
    val originalAmount: Int,      // amount of the original credit
    val date: Long,               // when the credit was extended
    val daysOutstanding: Int,
    val bucket: AgingBucket
)

/**
 * Per-party aging summary.
 */
data class PartyAging(
    val partyId: String,
    val partyName: String,
    val totalOwed: Int,
    val bucketTotals: Map<AgingBucket, Int>,   // amount per bucket
    val oldestDays: Int,                        // oldest unpaid item in days
    val unpaidItems: List<AgedItem>
)

/**
 * Full aging report across all parties.
 *
 * For receivables: parties are customers, "totalOwed" = what they owe you.
 * For payables:    parties are suppliers, "totalOwed" = what you owe them.
 */
data class AgingReport(
    val generatedAt: Long = System.currentTimeMillis(),
    val totalOwed: Int,
    val totalPartiesOwing: Int,
    val bucketTotals: Map<AgingBucket, Int>,
    val bucketPartyCounts: Map<AgingBucket, Int>,
    val parties: List<PartyAging>                // sorted by totalOwed desc
)
