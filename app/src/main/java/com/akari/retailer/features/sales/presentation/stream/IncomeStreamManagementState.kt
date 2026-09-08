package com.akari.retailer.features.sales.presentation.stream

import com.akari.retailer.features.sales.domain.models.IncomeStream

data class IncomeStreamManagementState(
    val streams: List<IncomeStream> = emptyList(),
    val defaultStreams: List<IncomeStream> = emptyList(),
    val customStreams: List<IncomeStream> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDialog: Boolean = false,
    val editingStream: IncomeStream? = null,
    val dialogName: String = "",
    val isSaving: Boolean = false,
    val selectedTab: Int = 0
)
