package com.akari.retailer.features.customer.domain.usecases

import com.akari.retailer.features.customer.domain.models.AgedItem
import com.akari.retailer.features.customer.domain.models.AgingBucket
import com.akari.retailer.features.customer.domain.models.AgingReport
import com.akari.retailer.features.customer.domain.models.PartyAging

/**
 * Generic input to the aging algorithm: one party with a list of
 * (date, amount) credit entries and a list of payments.
 */
data class AgingInput(
    val partyId: String,
    val partyName: String,
    /**
     * Credits extended to this party (customer owes you, or you owe supplier).
     * Positive amounts, sorted by date ascending is preferred (we sort anyway).
     */
    val credits: List<CreditLine>,
    /**
     * Payments received (customer) or made (supplier).
     * Positive amounts.
     */
    val payments: List<PaymentLine>
)

data class CreditLine(
    val transactionId: String,
    val amount: Int,
    val date: Long
)

data class PaymentLine(
    val amount: Int,
    val date: Long
)

class CalculateAgingReportUseCase {

    private val MILLIS_PER_DAY = 86_400_000L

    /**
     * Compute an aging report from a list of [AgingInput].
     *
     * For each party:
     *   1. Sort credits and payments by date
     *   2. Build a FIFO queue of unpaid credits
     *   3. Each payment pays off the oldest credits first
     *   4. Remaining credits = unpaid, aged by days since extended
     */
    fun invoke(inputs: List<AgingInput>): AgingReport {
        val now = System.currentTimeMillis()
        val perPartyAging = mutableListOf<PartyAging>()

        for (input in inputs) {
            val unpaid = computeUnpaidItems(
                partyId = input.partyId,
                partyName = input.partyName,
                credits = input.credits.sortedBy { it.date },
                payments = input.payments.sortedBy { it.date },
                now = now
            )
            if (unpaid.isEmpty()) continue

            val totalOwed = unpaid.sumOf { it.amount }
            val bucketTotals = AgingBucket.values().associateWith { bucket ->
                unpaid.filter { it.bucket == bucket }.sumOf { it.amount }
            }.filterValues { it > 0 }

            val oldestDays = unpaid.maxOfOrNull { it.daysOutstanding } ?: 0

            perPartyAging.add(
                PartyAging(
                    partyId = input.partyId,
                    partyName = input.partyName,
                    totalOwed = totalOwed,
                    bucketTotals = bucketTotals,
                    oldestDays = oldestDays,
                    unpaidItems = unpaid.sortedByDescending { it.daysOutstanding }
                )
            )
        }

        val sortedParties = perPartyAging.sortedByDescending { it.totalOwed }

        val globalTotalOwed = sortedParties.sumOf { it.totalOwed }
        val globalBucketTotals = AgingBucket.values().associateWith { bucket ->
            sortedParties.sumOf { it.bucketTotals[bucket] ?: 0 }
        }.filterValues { it > 0 }

        val globalBucketPartyCounts = AgingBucket.values().associateWith { bucket ->
            sortedParties.count { (it.bucketTotals[bucket] ?: 0) > 0 }
        }.filterValues { it > 0 }

        return AgingReport(
            generatedAt = now,
            totalOwed = globalTotalOwed,
            totalPartiesOwing = sortedParties.size,
            bucketTotals = globalBucketTotals,
            bucketPartyCounts = globalBucketPartyCounts,
            parties = sortedParties
        )
    }

    private fun computeUnpaidItems(
        partyId: String,
        partyName: String,
        credits: List<CreditLine>,
        payments: List<PaymentLine>,
        now: Long
    ): List<AgedItem> {
        data class Pending(
            val txnId: String,
            val original: Int,
            var remaining: Int,
            val date: Long
        )

        val queue = ArrayDeque<Pending>()

        // Walk merged timeline: we need to process credits and payments in date order.
        // Simplest: iterate credits to enqueue, then process each payment against
        // the queue in date order. But payments must be applied only to credits
        // that came BEFORE them.
        //
        // We'll do a chronological merge.

        var creditIdx = 0
        var paymentIdx = 0

        while (creditIdx < credits.size || paymentIdx < payments.size) {
            val nextCreditDate = credits.getOrNull(creditIdx)?.date ?: Long.MAX_VALUE
            val nextPaymentDate = payments.getOrNull(paymentIdx)?.date ?: Long.MAX_VALUE

            if (nextCreditDate <= nextPaymentDate) {
                // Enqueue credit
                val credit = credits[creditIdx]
                queue.addLast(
                    Pending(
                        txnId = credit.transactionId,
                        original = credit.amount,
                        remaining = credit.amount,
                        date = credit.date
                    )
                )
                creditIdx++
            } else {
                // Apply payment to oldest queue entries
                var toApply = payments[paymentIdx].amount
                while (toApply > 0 && queue.isNotEmpty()) {
                    val head = queue.first()
                    val applied = minOf(toApply, head.remaining)
                    head.remaining -= applied
                    toApply -= applied
                    if (head.remaining <= 0) queue.removeFirst()
                }
                paymentIdx++
            }
        }

        return queue.mapNotNull { p ->
            if (p.remaining <= 0) return@mapNotNull null
            val days = ((now - p.date) / MILLIS_PER_DAY).toInt()
            val bucket = AgingBucket.values().first { it.contains(days) }
            AgedItem(
                partyId = partyId,
                partyName = partyName,
                transactionId = p.txnId,
                amount = p.remaining,
                originalAmount = p.original,
                date = p.date,
                daysOutstanding = days,
                bucket = bucket
            )
        }
    }
}
