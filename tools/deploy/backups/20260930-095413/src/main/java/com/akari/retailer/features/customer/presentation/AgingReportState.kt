package com.akari.retailer.features.customer.presentation

import com.akari.retailer.features.customer.domain.models.AgingReport
import com.akari.retailer.features.customer.domain.models.PartyAging

data class AgingReportState(
    val report: AgingReport? = null,
    val filteredParties: List<PartyAging> = emptyList(),
    val mode: AgingMode = AgingMode.RECEIVABLES,
    val filter: AgingFilter = AgingFilter.ALL,
    val isLoading: Boolean = true,
    val error: String? = null
)
