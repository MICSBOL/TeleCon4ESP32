package com.micsbol.telecon4esp32.ui.customdashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.ui.control_panel.components.RealTimePlot
import com.micsbol.telecon4esp32.ui.customdashboard.DashboardWaveformStats
import com.micsbol.telecon4esp32.ui.theme.TechCyanBright

@Composable
fun DashboardWaveformWidget(
    plotSeries: List<PlotData>,
    plotRevision: Long,
    stats: DashboardWaveformStats,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    DashboardWidgetCard(
        isSelected = isSelected,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            RealTimePlot(
                modifier = Modifier.fillMaxSize(),
                series = plotSeries,
                plotRevision = plotRevision,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            DashboardMetricChip(
                label = stringResource(R.string.custom_dashboard_waveform_rms),
                value = stringResource(R.string.custom_dashboard_waveform_rms_value, stats.rmsVolts),
            )
            DashboardMetricChip(
                label = stringResource(R.string.custom_dashboard_waveform_freq),
                value = stringResource(R.string.custom_dashboard_waveform_freq_value, stats.frequencyHz),
            )
            DashboardMetricChip(
                label = stringResource(R.string.custom_dashboard_waveform_peak),
                value = stringResource(R.string.custom_dashboard_waveform_peak_value, stats.peakVolts),
            )
            DashboardMetricChip(
                label = stringResource(R.string.custom_dashboard_waveform_avg),
                value = stringResource(R.string.custom_dashboard_waveform_avg_value, stats.averageVolts),
            )
        }
    }
}
