package com.akari.retailer.features.customer.domain.usecases

import com.akari.retailer.features.customer.domain.models.AgedCredit
import com.akari.retailer.features.customer.domain.models.AgingBucket
import com.akari.retailer.features.customer.domain.models.AgingReport
import com.akari.retailer.features.customer.domain.models.CreditTransaction
import com.akari.retailer.features.customer.domain.models.CreditTransactionType
import com.akari.retailer.features.customer.domain.models.CustomerAging
import com.akari.retailer.features.customer.domain.models.Customer

class CalculateAgingReportUseCase {

    private val MILLIS_PER_DAY = 86_400_000L

    /**
     * Compute the aging report.
     *
     * For each customer:
     *   1. Sort their credit transactions by date ascending
     *   2. Build a FIFO queue of unpaid credits
     *   3. Each payment pays off the oldest unpaid credits first
     *   4. Whatever remains = unpaid credits, aged by days since extended
     */
    fun invoke(
        customers: List<Customer>,
        transactions: List<CreditTransaction>
    ): AgingReport {
        val now = System.currentTimeMillis()
        val customerMap = customers.associateBy { it.id }

        // Group transactions by customer
        val txnsByCustomer = transactions.groupBy { it.customerId }

        val perCustomerAging = mutableListOf<CustomerAging>()

        for (customer in customers) {
            val customerTxns = txnsByCustomer[customer.id] ?: emptyList()
            if (customerTxns.isEmpty()) continue

            val unpaidCredits = computeUnpaidCredits(
                customerId = customer.id,
                customerName = customer.name,
                transactions = customerTxns.sortedBy { it.date },
                now = now
            )

            if (unpaidCredits.isEmpty()) continue

            val totalOwed = unpaidCredits.sumOf { it.amount }

            val bucketTotals = AgingBucket.values().associateWith { bucket ->
                unpaidCredits.filter { it.bucket == bucket }.sumOf { it.amount }
            }.filterValues { it > 0 }

            val oldestDays = unpaidCredits.maxOfOrNull { it.daysOutstanding } ?: 0

            perCustomerAging.add(
                CustomerAging(
                    customerId = customer.id,
                    customerName = customer.name,
                    totalOwed = totalOwed,
                    bucketTotals = bucketTotals,
                    oldestDays = oldestDays,
                    unpaidCredits = unpaidCredits.sortedByDescending { it.daysOutstanding }
                )
            )
        }

        // Sort by amount owed desc
        val sortedCustomers = perCustomerAging.sortedByDescending { it.totalOwed }

        // Global totals
        val globalTotalOwed = sortedCustomers.sumOf { it.totalOwed }
        val globalBucketTotals = AgingBucket.values().associateWith { bucket ->
            sortedCustomers.sumOf { it.bucketTotals[bucket] ?: 0 }
        }.filterValues { it > 0 }

        val globalBucketCustomerCounts = AgingBucket.values().associateWith { bucket ->
            sortedCustomers.count { (it.bucketTotals[bucket] ?: 0) > 0 }
        }.filterValues { it > 0 }

        return AgingReport(
            generatedAt = now,
            totalOwed = globalTotalOwed,
            totalCustomersOwing = sortedCustomers.size,
            bucketTotals = globalBucketTotals,
            bucketCustomerCounts = globalBucketCustomerCounts,
            customers = sortedCustomers
        )
    }

    /**
     * FIFO allocation of payments against credits.
     * Returns the remaining unpaid credits with their original dates.
     */
    private fun computeUnpaidCredits(
        customerId: String,
        customerName: String,
        transactions: List<CreditTransaction>,
        now: Long
    ): List<AgedCredit> {
        // Queue of pending (unpaid) credits: {txnId, originalAmount, remainingAmount, date}
        data class Pending(var txnId: String, var original: Int, var remaining: Int, val date: Long)

        val queue = ArrayDeque<Pending>()

        for (txn in transactions) {
            when (txn.type) {
                CreditTransactionType.SALE_ON_CREDIT -> {
                    // credit amount is positive
                    queue.addLast(
                        Pending(
                            txnId = txn.id,
                            original = txn.amount,
                            remaining = txn.amount,
                            date = txn.date
                        )
                    )
                }
                CreditTransactionType.PAYMENT -> {
                    // payment amount is negative
                    var toApply = -txn.amount  // positive value
                    while (toApply > 0 && queue.isNotEmpty()) {
                        val head = queue.first()
                        val applied = minOf(toApply, head.remaining)
                        head.remaining -= applied
                        toApply -= applied
                        if (head.remaining <= 0) {
                            queue.removeFirst()
                        }
                    }
                    // If payment > total credits, ignore extra (shouldn't happen)
                }
            }
        }

        // Convert remaining pending credits to AgedCredit
        val result = mutableListOf<AgedCredit>()
        for (pending in queue) {
            if (pending.remaining <= 0) continue
            val days = ((now - pending.date) / MILLIS_PER_DAY).toInt()
            val bucket = AgingBucket.values().first { it.contains(days) }
            result.add(
                AgedCredit(
                    customerId = customerId,
                    customerName = customerName,
                    creditTransactionId = pending.txnId,
                    amount = pending.remaining,
                    originalCreditAmount = pending.original,
                    date = pending.date,
                    daysOutstanding = days,
                    bucket = bucket
                )
            )
        }
        return result
    }
}
