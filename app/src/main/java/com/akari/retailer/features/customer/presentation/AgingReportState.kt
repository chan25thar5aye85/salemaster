package com.akari.retailer.features.customer.presentation

import com.akari.retailer.features.customer.domain.models.AgingBucket
import com.akari.retailer.features.customer.domain.models.AgingReport

/**
 * Filter for the customer list — tap a bucket to see only customers in it.
 */
enum class AgingFilter {
    ALL,
    CURRENT,
    DAYS_30,
    DAYS_60,
    DAYS_90;

    fun toBucket(): AgingBucket? = when (this) {
        ALL -> null
        CURRENT -> AgingBucket.CURRENT
        DAYS_30 -> AgingBucket.DAYS_30
        DAYS_60 -> AgingBucket.DAYS_60
        DAYS_90 -> AgingBucket.DAYS_90
    }
}

data class AgingReportState(
    val report: AgingReport? = null,
    val filteredCustomers: List<com.akari.retailer.features.customer.domain.models.CustomerAging> = emptyList(),
    val filter: AgingFilter = AgingFilter.ALL,
    val isLoading: Boolean = true,
    val error: String? = null
)
