package com.akari.retailer.features.customer.domain.models

/**
 * Aging bucket — how long an unpaid credit has been outstanding.
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
 * A single unpaid credit sale, aged by days outstanding.
 */
data class AgedCredit(
    val customerId: String,
    val customerName: String,
    val creditTransactionId: String,
    val amount: Int,
    val originalCreditAmount: Int,      // amount of the original credit
    val date: Long,                     // when the credit was extended
    val daysOutstanding: Int,
    val bucket: AgingBucket
)

/**
 * Per-customer aging summary.
 */
data class CustomerAging(
    val customerId: String,
    val customerName: String,
    val totalOwed: Int,
    val bucketTotals: Map<AgingBucket, Int>,   // amount per bucket
    val oldestDays: Int,                        // oldest unpaid credit in days
    val unpaidCredits: List<AgedCredit>
)

/**
 * Full aging report across all customers.
 */
data class AgingReport(
    val generatedAt: Long = System.currentTimeMillis(),
    val totalOwed: Int,
    val totalCustomersOwing: Int,
    val bucketTotals: Map<AgingBucket, Int>,   // amount per bucket across all
    val bucketCustomerCounts: Map<AgingBucket, Int>,  // # of customers with $ in that bucket
    val customers: List<CustomerAging>          // sorted by totalOwed desc
)
