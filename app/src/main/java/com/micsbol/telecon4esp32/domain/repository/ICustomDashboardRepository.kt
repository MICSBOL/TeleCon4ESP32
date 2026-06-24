package com.micsbol.telecon4esp32.domain.repository

import com.micsbol.telecon4esp32.domain.model.SavedDashboardLayout
import kotlinx.coroutines.flow.Flow

interface ICustomDashboardRepository {
    val savedDashboardsFlow: Flow<List<SavedDashboardLayout>>
    suspend fun saveDashboards(layouts: List<SavedDashboardLayout>)
}
