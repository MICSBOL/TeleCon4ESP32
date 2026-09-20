package com.micsbol.telecon4esp32.ui.control_panel

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.ControlAnalogHistory
import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.model.ChannelRouting
import com.micsbol.telecon4esp32.domain.model.ControlPanelCenterMode
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.PlotCalibration
import com.micsbol.telecon4esp32.domain.model.PlotGraphMode
import com.micsbol.telecon4esp32.domain.model.PlotLineStyle
import com.micsbol.telecon4esp32.domain.model.TelemetryChannel
import com.micsbol.telecon4esp32.domain.model.TelemetrySink
import com.micsbol.telecon4esp32.domain.model.StickChannelLink
import com.micsbol.telecon4esp32.domain.model.overlayStickOnPlotSeries
import com.micsbol.telecon4esp32.domain.model.parseCalibrationFloat
import com.micsbol.telecon4esp32.domain.model.resolvedYRange
import com.micsbol.telecon4esp32.domain.model.toCalibrationDraftText
import com.micsbol.telecon4esp32.ui.components.EmitterBrandLogo
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.theme.Neo
import com.micsbol.telecon4esp32.ui.control_panel.components.ControlPanelDisplayFrame
import com.micsbol.telecon4esp32.ui.control_panel.components.ButtonSide
import com.micsbol.telecon4esp32.ui.control_panel.components.HorizontalTextAnimation
import com.micsbol.telecon4esp32.ui.control_panel.components.PlotYAxisScale
import com.micsbol.telecon4esp32.ui.control_panel.components.PushButtonSide
import com.micsbol.telecon4esp32.ui.control_panel.components.RealTimePlot
import com.micsbol.telecon4esp32.ui.theme.titanOneRegular
import kotlinx.coroutines.delay
import kotlin.apply
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private val CenterDisplayBezelWidth = 5.dp
private val CenterDisplayOuterCornerRadius = 8.dp
private val CenterDisplayInnerCornerRadius = 3.dp
private val CenterDisplayChinHeight = 8.dp
private val CenterDisplayHeaderLogoSize = 16.dp

@Composable
fun CenterDisplay(
    modifier: Modifier = Modifier,
    series: List<PlotData> = emptyList(),
    radarSeries: List<PlotData> = emptyList(),
    plotRevision: Long = 0L,
    plotCalibrations: List<PlotCalibration> = PlotCalibration.defaults(),
    channelRouting: ChannelRouting = ChannelRouting.defaults(),
    onRadarSourceChange: (TelemetrySink, TelemetryChannel) -> Unit = { _, _ -> },
    onPlotLabelChange: (Int, String) -> Unit = { _, _ -> },
    onPlotChannelChange: (Int, TelemetryChannel) -> Unit = { _, _ -> },
    onPlotCalibrationChange: (Int, PlotCalibration) -> Unit = { _, _ -> },
    centerMode: ControlPanelCenterMode = ControlPanelCenterMode.PLOTS,
    centerModeUnlocked: Boolean = true,
    onUnlockCenterMode: () -> Unit = {},
    leftStickXy: Pair<Float, Float> = Pair(0f, 0f),
    rightStickXy: Pair<Float, Float> = Pair(0f, 0f),
    leftStickMode: JoystickMode = JoystickMode.Spring(),
    rightStickMode: JoystickMode = JoystickMode.Spring(),
    leftKnobValue: Float = 0.5f,
    rightKnobValue: Float = 0.5f,
    topStartOverlay: @Composable () -> Unit = {},
    displaySettings: ControlPanelPlotDisplaySettings? = null,
    onDisplaySettingsChange: (ControlPanelPlotDisplaySettings) -> Unit = {},
) {
    var localDisplay by rememberSaveable(stateSaver = ControlPanelPlotDisplaySettingsSaver) {
        mutableStateOf(ControlPanelPlotDisplaySettings.DEFAULT)
    }
    val plotDisplay = displaySettings ?: localDisplay
    val radarSettings = plotDisplay.radarSettings
    val plotVisible = plotDisplay.plotVisible
    val graphModes = plotDisplay.graphModes
    val lineStyles = plotDisplay.lineStyles
    val plotOnTop = plotDisplay.plotOnTop
    val leftStickLink = plotDisplay.leftStickLink
    val rightStickLink = plotDisplay.rightStickLink
    val leftKnobLink = plotDisplay.leftKnobLink
    val rightKnobLink = plotDisplay.rightKnobLink
    val analogOverlay = remember { ControlAnalogHistory() }
    var overlayChannels by remember { mutableStateOf<Map<TelemetryChannel, List<Float>>>(emptyMap()) }
    val latestLeftXy by rememberUpdatedState(leftStickXy)
    val latestRightXy by rememberUpdatedState(rightStickXy)
    val latestLeftMode by rememberUpdatedState(leftStickMode)
    val latestRightMode by rememberUpdatedState(rightStickMode)
    val latestLeftLink by rememberUpdatedState(leftStickLink)
    val latestRightLink by rememberUpdatedState(rightStickLink)
    val latestLeftKnob by rememberUpdatedState(leftKnobValue)
    val latestRightKnob by rememberUpdatedState(rightKnobValue)
    val latestLeftKnobLink by rememberUpdatedState(leftKnobLink)
    val latestRightKnobLink by rememberUpdatedState(rightKnobLink)
    LaunchedEffect(
        leftStickLink.enabled,
        leftStickLink.vertical,
        leftStickLink.horizontal,
        rightStickLink.enabled,
        rightStickLink.vertical,
        rightStickLink.horizontal,
        leftKnobLink.enabled,
        leftKnobLink.channel,
        rightKnobLink.enabled,
        rightKnobLink.channel,
    ) {
        if (!leftStickLink.enabled &&
            !rightStickLink.enabled &&
            !leftKnobLink.enabled &&
            !rightKnobLink.enabled
        ) {
            analogOverlay.ingest(emptyMap())
            overlayChannels = emptyMap()
            return@LaunchedEffect
        }
        while (true) {
            analogOverlay.ingest(
                StickChannelLink.merge(
                    latestLeftLink.samples(
                        x = latestLeftXy.first,
                        y = latestLeftXy.second,
                        axis = latestLeftMode.axis,
                        restX = latestLeftMode.initialPositionNormalized().first,
                        restY = latestLeftMode.initialPositionNormalized().second,
                    ),
                    latestRightLink.samples(
                        x = latestRightXy.first,
                        y = latestRightXy.second,
                        axis = latestRightMode.axis,
                        restX = latestRightMode.initialPositionNormalized().first,
                        restY = latestRightMode.initialPositionNormalized().second,
                    ),
                    latestLeftKnobLink.sample(latestLeftKnob),
                    latestRightKnobLink.sample(latestRightKnob),
                ),
            )
            overlayChannels = analogOverlay.snapshot()
            delay(80)
        }
    }
    val plotSeries = remember(series, channelRouting, overlayChannels) {
        overlayStickOnPlotSeries(series, channelRouting, overlayChannels)
    }
    fun updatePlotDisplay(next: ControlPanelPlotDisplaySettings) {
        val exclusive = next.withExclusiveChannels()
        if (displaySettings == null) {
            localDisplay = exclusive
        }
        onDisplaySettingsChange(exclusive)
    }
    var showStickSettings by remember { mutableStateOf(false) }
    LaunchedEffect(centerMode, centerModeUnlocked) {
        if (centerMode != ControlPanelCenterMode.STICK || !centerModeUnlocked) {
            showStickSettings = false
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        ControlPanelDisplayFrame(
            modifier = Modifier.fillMaxSize(),
            bezelWidth = CenterDisplayBezelWidth,
            outerCornerRadius = CenterDisplayOuterCornerRadius,
            innerCornerRadius = CenterDisplayInnerCornerRadius,
            chinHeight = CenterDisplayChinHeight,
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 1.dp, top = 2.dp, bottom = 2.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    topStartOverlay()
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        EmitterBrandLogo(size = CenterDisplayHeaderLogoSize)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.home_title),
                            fontFamily = titanOneRegular,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Normal,
                            color = brandPrimary(),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        when (centerMode) {
                            ControlPanelCenterMode.PLOTS -> {
                                CartesianPlot(
                                    modifier = Modifier.fillMaxSize(),
                                    series = plotSeries,
                                    plotRevision = plotRevision,
                                    calibrations = plotCalibrations,
                                    channelRouting = channelRouting,
                                    onPlotLabelChange = onPlotLabelChange,
                                    onPlotChannelChange = onPlotChannelChange,
                                    onPlotCalibrationChange = onPlotCalibrationChange,
                                    plotVisible = plotVisible,
                                    graphModes = graphModes,
                                    lineStyles = lineStyles,
                                    plotOnTop = plotOnTop,
                                    onPlotVisibleChange = {
                                        updatePlotDisplay(plotDisplay.copy(plotVisible = it))
                                    },
                                    onGraphModesChange = {
                                        updatePlotDisplay(plotDisplay.copy(graphModes = it))
                                    },
                                    onLineStylesChange = {
                                        updatePlotDisplay(plotDisplay.copy(lineStyles = it))
                                    },
                                    onPlotOnTopChange = {
                                        updatePlotDisplay(plotDisplay.copy(plotOnTop = it))
                                    },
                                )
                            }
                            ControlPanelCenterMode.STICK -> {
                                if (centerModeUnlocked) {
                                    ControlPanelStickDisplay(
                                        leftStickXy = leftStickXy,
                                        rightStickXy = rightStickXy,
                                        leftLink = leftStickLink,
                                        rightLink = rightStickLink,
                                        occupiedChannels = plotDisplay.occupiedChannels(
                                            exceptLeftStick = true,
                                            exceptRightStick = true,
                                        ),
                                        onLeftLinkChange = { next ->
                                            updatePlotDisplay(plotDisplay.copy(leftStickLink = next))
                                        },
                                        onRightLinkChange = { next ->
                                            updatePlotDisplay(plotDisplay.copy(rightStickLink = next))
                                        },
                                        showSettings = showStickSettings,
                                        onShowSettingsChange = { showStickSettings = it },
                                        showSettingsChip = false,
                                        u8Series = radarSeries.ifEmpty { series },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    ControlPanelCenterLockedPane(
                                        mode = centerMode,
                                        onUnlockClick = onUnlockCenterMode,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            ControlPanelCenterMode.CAMERA -> {
                                if (centerModeUnlocked) {
                                    ControlPanelCameraDisplay(modifier = Modifier.fillMaxSize())
                                } else {
                                    ControlPanelCenterLockedPane(
                                        mode = centerMode,
                                        onUnlockClick = onUnlockCenterMode,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            ControlPanelCenterMode.RADAR -> {
                                if (centerModeUnlocked) {
                                    val radarSources = radarSeries.ifEmpty { series }
                                    ControlPanelRadarDisplay(
                                        series = radarSources,
                                        settings = radarSettings.copy(
                                            angleSeriesIndex = channelRouting
                                                .sourceFor(TelemetrySink.RADAR_ANGLE)
                                                .u8Index(),
                                            rangeSeriesIndex = channelRouting
                                                .sourceFor(TelemetrySink.RADAR_RANGE)
                                                .u8Index(),
                                        ),
                                        onSettingsChange = { updated ->
                                            updatePlotDisplay(plotDisplay.copy(radarSettings = updated))
                                            val angle = TelemetryChannel.u8At(updated.angleSeriesIndex)
                                            val range = TelemetryChannel.u8At(updated.rangeSeriesIndex)
                                            if (angle != channelRouting.sourceFor(TelemetrySink.RADAR_ANGLE)) {
                                                onRadarSourceChange(TelemetrySink.RADAR_ANGLE, angle)
                                            }
                                            if (range != channelRouting.sourceFor(TelemetrySink.RADAR_RANGE)) {
                                                onRadarSourceChange(TelemetrySink.RADAR_RANGE, range)
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    ControlPanelCenterLockedPane(
                                        mode = centerMode,
                                        onUnlockClick = onUnlockCenterMode,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                        }
                        if (centerMode == ControlPanelCenterMode.STICK && centerModeUnlocked) {
                            StickGraphSettingsChip(
                                onClick = { showStickSettings = true },
                                modifier = Modifier.align(Alignment.TopEnd),
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class PlotType {
    CARTESIAN,
    COMPLEX_CIRCULAR,
    BAR_GRAPH
}

private val CartesianPlotHorizontalPadding = 0.dp
private val CartesianPlotPaneGap = 12.dp
private val CartesianPlotYAxisWidth = 22.dp

@Composable
fun CartesianPlot(
    modifier: Modifier,
    series: List<PlotData> = emptyList(),
    plotRevision: Long = 0L,
    calibrations: List<PlotCalibration> = PlotCalibration.defaults(),
    channelRouting: ChannelRouting = ChannelRouting.defaults(),
    onPlotLabelChange: (Int, String) -> Unit = { _, _ -> },
    onPlotChannelChange: (Int, TelemetryChannel) -> Unit = { _, _ -> },
    onPlotCalibrationChange: (Int, PlotCalibration) -> Unit = { _, _ -> },
    plotVisible: List<Boolean>,
    graphModes: List<PlotGraphMode>,
    lineStyles: List<PlotLineStyle>,
    plotOnTop: List<Boolean>,
    onPlotVisibleChange: (List<Boolean>) -> Unit,
    onGraphModesChange: (List<PlotGraphMode>) -> Unit,
    onLineStylesChange: (List<PlotLineStyle>) -> Unit,
    onPlotOnTopChange: (List<Boolean>) -> Unit,
) {
    // Four channels: top pane = series 0–1, bottom pane = series 2–3.
    val paddedCalibrations = PlotCalibration.padded(calibrations)
    val topSeries = series.take(2)
    val bottomSeries = series.drop(2).take(2)
    val topCalibrations = paddedCalibrations.take(2)
    val bottomCalibrations = paddedCalibrations.drop(2).take(2)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = CartesianPlotHorizontalPadding),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            CartesianPlotPane(
                modifier = Modifier.weight(1f),
                series = topSeries,
                calibrations = topCalibrations,
                plotRevision = plotRevision,
                paneStart = 0,
                channelRouting = channelRouting,
                visible = plotVisible.take(2),
                graphModes = graphModes.take(2),
                lineStyles = lineStyles.take(2),
                onTop = plotOnTop.take(2),
                onVisibleChange = { index, visible ->
                    onPlotVisibleChange(plotVisible.toMutableList().also { it[index] = visible })
                },
                onGraphModeChange = { index, mode ->
                    onGraphModesChange(graphModes.toMutableList().also { it[index] = mode })
                },
                onLineStyleChange = { index, style ->
                    onLineStylesChange(lineStyles.toMutableList().also { it[index] = style })
                },
                onOnTopSelected = { index ->
                    onPlotOnTopChange(
                        exclusivePlotOnTop(plotOnTop, selectedIndex = index, paneStart = 0),
                    )
                },
                onPlotLabelChange = onPlotLabelChange,
                onPlotChannelChange = onPlotChannelChange,
                onPlotCalibrationChange = onPlotCalibrationChange,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CartesianPlotPaneGap),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.18f)),
                )
            }
            CartesianPlotPane(
                modifier = Modifier.weight(1f),
                series = bottomSeries,
                calibrations = bottomCalibrations,
                plotRevision = plotRevision,
                paneStart = 2,
                channelRouting = channelRouting,
                visible = plotVisible.drop(2).take(2),
                graphModes = graphModes.drop(2).take(2),
                lineStyles = lineStyles.drop(2).take(2),
                onTop = plotOnTop.drop(2).take(2),
                onVisibleChange = { index, visible ->
                    onPlotVisibleChange(
                        plotVisible.toMutableList().also { it[index + 2] = visible },
                    )
                },
                onGraphModeChange = { index, mode ->
                    onGraphModesChange(
                        graphModes.toMutableList().also { it[index + 2] = mode },
                    )
                },
                onLineStyleChange = { index, style ->
                    onLineStylesChange(
                        lineStyles.toMutableList().also { it[index + 2] = style },
                    )
                },
                onOnTopSelected = { index ->
                    onPlotOnTopChange(
                        exclusivePlotOnTop(plotOnTop, selectedIndex = index, paneStart = 2),
                    )
                },
                onPlotLabelChange = onPlotLabelChange,
                onPlotChannelChange = onPlotChannelChange,
                onPlotCalibrationChange = onPlotCalibrationChange,
            )
        }
        Text(
            text = stringResource(R.string.rc_plot_axis_label),
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp),
        )
    }
}

private fun PlotLineStyle.labelRes(): Int = when (this) {
    PlotLineStyle.LINE -> R.string.rc_plot_style_line
    PlotLineStyle.STAIR -> R.string.rc_plot_style_stair
    PlotLineStyle.TRIANGLE -> R.string.rc_plot_style_triangle
}

/** Only one trace in a two-plot pane can be on top. */
internal fun exclusivePlotOnTop(
    onTop: List<Boolean>,
    selectedIndex: Int,
    paneStart: Int,
    paneSize: Int = 2,
): List<Boolean> {
    val paneEnd = paneStart + paneSize
    val globalIndex = paneStart + selectedIndex
    return onTop.mapIndexed { index, wasOnTop ->
        if (index in paneStart until paneEnd) index == globalIndex else wasOnTop
    }
}

@Composable
private fun PlotLegendItem(
    plotData: PlotData,
    calibration: PlotCalibration,
    sink: TelemetrySink,
    selectedChannel: TelemetryChannel,
    textStyle: TextStyle,
    characterThreshold: Int,
    visible: Boolean,
    graphMode: PlotGraphMode,
    lineStyle: PlotLineStyle,
    onTop: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    onGraphModeChange: (PlotGraphMode) -> Unit,
    onLineStyleChange: (PlotLineStyle) -> Unit,
    onOnTopSelected: () -> Unit,
    onLabelChange: (String) -> Unit,
    onChannelSelected: (TelemetryChannel) -> Unit,
    onCalibrationChange: (PlotCalibration) -> Unit,
) {
    val lastNormalized = plotData.dataPoints.lastOrNull()
    val valueText = lastNormalized?.let { calibration.formatEngineering(it) }
    var menuExpanded by remember { mutableStateOf(false) }
    val (yMin, yMax) = calibration.resolvedYRange()
    var yMinText by remember(menuExpanded) {
        mutableStateOf(yMin.toCalibrationDraftText())
    }
    var yMaxText by remember(menuExpanded) {
        mutableStateOf(yMax.toCalibrationDraftText())
    }
    var unitText by remember(menuExpanded) { mutableStateOf(calibration.unit) }
    val focusManager = LocalFocusManager.current
    val commitCalibration = {
        onCalibrationChange(calibrationFromRangeDraft(yMinText, yMaxText, unitText, calibration))
    }
    val modeLabel = stringResource(
        if (graphMode == PlotGraphMode.CONTINUOUS) {
            R.string.rc_plot_graph_continuous
        } else {
            R.string.rc_plot_graph_on_change
        },
    )
    val styleLabel = stringResource(lineStyle.labelRes())
    val visibilityLabel = stringResource(
        if (visible) R.string.rc_plot_legend_visible else R.string.rc_plot_legend_hidden,
    )
    val optionsDescription = stringResource(
        R.string.rc_plot_legend_options_content_description,
        plotData.name,
        modeLabel,
        styleLabel,
        visibilityLabel,
    )
    val fieldColors = hudMenuOutlinedFieldColors()
    Box {
        Row(
            modifier = Modifier
                .alpha(if (visible) 1f else 0.4f)
                .clickable(role = Role.Button) { menuExpanded = true }
                .semantics {
                    role = Role.Button
                    contentDescription = optionsDescription
                }
                .padding(vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(2.dp)
                    .background(Color(plotData.colorArgb).copy(alpha = if (visible) 1f else 0.45f)),
            )
            Spacer(modifier = Modifier.width(4.dp))
            HorizontalTextAnimation(
                text = plotData.name,
                style = textStyle,
                color = Color.LightGray,
                characterThreshold = characterThreshold,
            )
            if (valueText != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = valueText,
                    style = textStyle,
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
        TelemetryWidgetOptionsMenu(
            expanded = menuExpanded,
            sink = sink,
            selectedChannel = selectedChannel,
            widgetLabel = plotData.name,
            onChannelSelected = onChannelSelected,
            onLabelChange = onLabelChange,
            onDismiss = {
                commitCalibration()
                menuExpanded = false
            },
        ) { closeMenu ->
            HorizontalDivider(color = Neo.TextSecondary.copy(alpha = 0.3f))
            PlotHudTextField(
                value = yMinText,
                onValueChange = { yMinText = it },
                label = stringResource(R.string.rc_plot_settings_y_min),
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
                colors = fieldColors,
            )
            PlotHudTextField(
                value = yMaxText,
                onValueChange = { yMaxText = it },
                label = stringResource(R.string.rc_plot_settings_y_max),
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
                colors = fieldColors,
            )
            PlotHudTextField(
                value = unitText,
                onValueChange = { unitText = it },
                label = stringResource(R.string.rc_controller_settings_plot_unit),
                placeholder = stringResource(R.string.rc_controller_settings_plot_unit_hint),
                imeAction = ImeAction.Done,
                colors = fieldColors,
                onDone = {
                    commitCalibration()
                    focusManager.clearFocus()
                },
            )
            HorizontalDivider(color = Neo.TextSecondary.copy(alpha = 0.3f))
            PlotLegendMenuItem(
                label = stringResource(R.string.rc_plot_graph_continuous),
                selected = graphMode == PlotGraphMode.CONTINUOUS,
                onClick = {
                    onGraphModeChange(PlotGraphMode.CONTINUOUS)
                    closeMenu()
                },
            )
            PlotLegendMenuItem(
                label = stringResource(R.string.rc_plot_graph_on_change),
                selected = graphMode == PlotGraphMode.ON_CHANGE,
                onClick = {
                    onGraphModeChange(PlotGraphMode.ON_CHANGE)
                    closeMenu()
                },
            )
            HorizontalDivider(color = Neo.TextSecondary.copy(alpha = 0.3f))
            PlotLineStyle.entries.forEach { style ->
                PlotLegendMenuItem(
                    label = stringResource(style.labelRes()),
                    selected = lineStyle == style,
                    onClick = {
                        onLineStyleChange(style)
                        closeMenu()
                    },
                )
            }
            HorizontalDivider(color = Neo.TextSecondary.copy(alpha = 0.3f))
            PlotLegendMenuItem(
                label = stringResource(R.string.rc_plot_on_top),
                selected = onTop,
                onClick = {
                    onOnTopSelected()
                    closeMenu()
                },
            )
            PlotLegendMenuItem(
                label = stringResource(
                    if (visible) R.string.rc_plot_hide else R.string.rc_plot_show,
                ),
                selected = false,
                onClick = {
                    onVisibleChange(!visible)
                    closeMenu()
                },
            )
        }
    }
}

@Composable
private fun PlotLegendMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = if (selected) Neo.Accent else Neo.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        onClick = onClick,
        trailingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Neo.Accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            null
        },
        colors = MenuDefaults.itemColors(
            textColor = Neo.TextPrimary,
            trailingIconColor = Neo.Accent,
        ),
    )
}

@Composable
private fun PlotHudTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    colors: TextFieldColors,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    placeholder: String? = null,
    onDone: (() -> Unit)? = null,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                color = Neo.TextPrimary,
            )
        },
        placeholder = placeholder?.let { hint ->
            {
                Text(
                    text = hint,
                    color = Neo.TextMuted,
                )
            }
        },
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Neo.TextPrimary),
        singleLine = true,
        colors = colors,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardController?.hide()
                focusManager.clearFocus()
                onDone?.invoke()
            },
        ),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .width(220.dp),
    )
}

private fun calibrationFromRangeDraft(
    minText: String,
    maxText: String,
    unitText: String,
    current: PlotCalibration,
): PlotCalibration {
    val min = parseCalibrationFloat(minText, current.offset)
    val max = parseCalibrationFloat(maxText, current.offset + current.span)
    val span = if (max > min) max - min else 1f
    return PlotCalibration(
        offset = min,
        span = span,
        unit = unitText.trim(),
    )
}

@Composable
private fun CartesianPlotPane(
    modifier: Modifier = Modifier,
    series: List<PlotData>,
    calibrations: List<PlotCalibration>,
    plotRevision: Long,
    paneStart: Int,
    channelRouting: ChannelRouting,
    visible: List<Boolean>,
    graphModes: List<PlotGraphMode>,
    lineStyles: List<PlotLineStyle>,
    onTop: List<Boolean>,
    onVisibleChange: (Int, Boolean) -> Unit,
    onGraphModeChange: (Int, PlotGraphMode) -> Unit,
    onLineStyleChange: (Int, PlotLineStyle) -> Unit,
    onOnTopSelected: (Int) -> Unit,
    onPlotLabelChange: (Int, String) -> Unit,
    onPlotChannelChange: (Int, TelemetryChannel) -> Unit,
    onPlotCalibrationChange: (Int, PlotCalibration) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val paneMaxHeight = maxHeight
        val paneMaxWidth = maxWidth
        val leftVisible = visible.getOrElse(0) { true } && series.isNotEmpty()
        val rightVisible = visible.getOrElse(1) { true } && series.size > 1
        Row(modifier = Modifier.fillMaxSize()) {
            if (leftVisible) {
                val (min, max) = calibrations.getOrElse(0) { PlotCalibration.DEFAULT }.resolvedYRange()
                PlotYAxisScale(
                    min = min,
                    max = max,
                    color = Color(series[0].colorArgb),
                    axisOnEnd = true,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(CartesianPlotYAxisWidth),
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
        RealTimePlot(
            modifier = Modifier.fillMaxSize(),
            series = series,
            plotRevision = plotRevision,
            visible = visible,
            graphModes = graphModes,
            lineStyles = lineStyles,
            onTop = onTop,
            calibrations = calibrations,
        )
        // Scale legend so two labels never stack-overflow on short landscape panes.
        val compact = paneMaxHeight < 72.dp
        val legendStyle = MaterialTheme.typography.labelSmall.copy(
            fontSize = if (compact) 8.sp else 10.sp,
            lineHeight = if (compact) 10.sp else 12.sp,
        )
        val characterThreshold = when {
            paneMaxWidth < 100.dp -> 5
            paneMaxWidth < 160.dp -> 7
            else -> 10
        }
        val legendSpacing = if (compact) 0.dp else 2.dp
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.72f)
                .padding(start = 4.dp, top = 10.dp, end = 2.dp),
            verticalArrangement = Arrangement.spacedBy(legendSpacing),
            horizontalAlignment = Alignment.Start,
        ) {
            series.forEachIndexed { index, plotData ->
                val plotIndex = paneStart + index
                val sink = TelemetrySink.plotAt(plotIndex)
                PlotLegendItem(
                    plotData = plotData,
                    calibration = calibrations.getOrElse(index) { PlotCalibration.DEFAULT },
                    sink = sink,
                    selectedChannel = channelRouting.sourceFor(sink),
                    textStyle = legendStyle,
                    characterThreshold = characterThreshold,
                    visible = visible.getOrElse(index) { true },
                    graphMode = graphModes.getOrElse(index) { PlotGraphMode.CONTINUOUS },
                    lineStyle = lineStyles.getOrElse(index) { PlotLineStyle.LINE },
                    onTop = onTop.getOrElse(index) { false },
                    onVisibleChange = { onVisibleChange(index, it) },
                    onGraphModeChange = { onGraphModeChange(index, it) },
                    onLineStyleChange = { onLineStyleChange(index, it) },
                    onOnTopSelected = { onOnTopSelected(index) },
                    onLabelChange = { onPlotLabelChange(plotIndex, it) },
                    onChannelSelected = { onPlotChannelChange(plotIndex, it) },
                    onCalibrationChange = { onPlotCalibrationChange(plotIndex, it) },
                )
            }
        }
            }
            if (rightVisible) {
                val (min, max) = calibrations.getOrElse(1) { PlotCalibration.DEFAULT }.resolvedYRange()
                PlotYAxisScale(
                    min = min,
                    max = max,
                    color = Color(series[1].colorArgb),
                    axisOnEnd = false,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(CartesianPlotYAxisWidth),
                )
            }
        }
    }
}

data class ComplexPlotData(
    val real: Float,
    val imaginary: Float,
    val color: Color
)

@Composable
fun ComplexCircularPlot(
    modifier: Modifier = Modifier,
    points: List<ComplexPlotData>,
    gridColor: Color = Color.White.copy(alpha = 0.3f)
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2

        (1..4).forEach { i ->
            drawCircle(
                color = gridColor,
                radius = radius * (i / 4f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        (0 until 360 step 45).forEach { angle ->
            val angleInRadians = Math.toRadians(angle.toDouble()).toFloat()
            val start = Offset(centerX, centerY)
            val end = Offset(
                x = centerX + radius * cos(angleInRadians),
                y = centerY + radius * sin(angleInRadians)
            )
            drawLine(gridColor, start, end, strokeWidth = 1.dp.toPx())
        }

        drawLine(
            gridColor,
            Offset(centerX, 0f),
            Offset(centerX, size.height), strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            gridColor,
            Offset(0f, centerY),
            Offset(size.width, centerY), strokeWidth = 1.5.dp.toPx()
        )

        points.forEach { point ->
            // Calculate magnitude and phase
            val magnitude = sqrt(point.real * point.real + point.imaginary * point.imaginary)
            val phase = atan2(point.imaginary, point.real)

            val pointRadius = magnitude.coerceAtMost(1f) * radius

            val x = centerX + pointRadius * cos(phase)
            val y = centerY + pointRadius * sin(phase) // Y-axis is standard here, not inverted

            drawLine(
                color = point.color.copy(alpha = 0.7f),
                start = Offset(centerX, centerY),
                end = Offset(x, y),
                strokeWidth = 2.dp.toPx()
            )
            drawCircle(color = point.color, radius = 8f, center = Offset(x, y))
        }
    }
}


@Composable
fun BarGraph(
    modifier: Modifier = Modifier,
    series: List<PlotData>,
) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        series.forEach { plotData ->
            val currentValue = plotData.dataPoints.lastOrNull() ?: 0f
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    GlowingBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(currentValue),
                        color = Color(plotData.colorArgb)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plotData.name,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun GlowingBar(
    modifier: Modifier = Modifier,
    color: Color
) {
    val glowColor = color.copy(alpha = 0.3f)
    val cornerRadius = CornerRadius(x = 2.dp.value, y = 2.dp.value)

    Box(
        modifier = modifier
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        this.color = glowColor.toArgb()
                        maskFilter = BlurMaskFilter(12.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.nativeCanvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        cornerRadius.x,
                        cornerRadius.y,
                        paint
                    )
                }
            }
            .background(color, shape = RoundedCornerShape(2.dp))
    )
}

@Composable
fun HistogramPlot(
    modifier: Modifier = Modifier,
    binCounts: List<Int>, // The processed counts for each bin
    barColor: Color = Color(0xFF4682B4)// A good color for statistical plots
) {
    val maxCount = binCounts.maxOrNull() ?: 1

    Row(
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp), // Bars are close together
        verticalAlignment = Alignment.Bottom
    ) {
        binCounts.forEach { count ->
            val barHeight = (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)

            GlowingBar(
                modifier = Modifier
                    .weight(1f) // Each bar takes equal width
                    .fillMaxHeight(barHeight),
                color = barColor
            )
        }
    }
}


@Composable
fun ButtonColumn(
    modifier: Modifier = Modifier,
    onTopPress: () -> Unit,
    onBottomPress: () -> Unit,
    side: ButtonSide,
    buttonSize: Dp = 50.dp
) {
    Column(
        modifier = modifier.padding(bottom = 8.dp),
        horizontalAlignment =
            if (side == ButtonSide.RIGHT) Alignment.Start else Alignment.End,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        PushButtonSide(
            modifier = Modifier.size(buttonSize * 0.8f),
            side = side,
            onPress = onTopPress
        )
        PushButtonSide(modifier = Modifier.size(buttonSize), side = side, onPress = onBottomPress)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun HistogramPlotPreview() {
    val sampleBinCounts = listOf(
        10,
        25,
        40,
        80,
        110,
        75,
        35,
        20,
        12,
        5,
        10,
        1,
        5,
        7,
        65,
        77,
        88,
        99,
        100,
        120,
        70,
        5,
        64
    )

    HistogramPlot(
        modifier = Modifier
            .width(400.dp)
            .height(250.dp)
            .padding(16.dp),
        binCounts = sampleBinCounts
    )
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
fun DynamicHistogramPreview() {
    val historicalData = remember { mutableStateListOf<Float>() }
    val binCounts = remember { mutableStateListOf<Int>() }

    val numBins = 20 // The number of bars to display in the histogram
    val maxHistorySize = 1000 // We'll analyze the last 1000 samples

    LaunchedEffect(Unit) {
        while (true) {
            val newDataPoint =
                (sin(System.currentTimeMillis() / 2000f * 2 * PI.toFloat()) + Random.nextFloat() * 0.5f).coerceIn(
                    -1f,
                    1f
                )
            historicalData.add((newDataPoint + 1f) / 2f) // Normalize to 0-1 range

            if (historicalData.size > maxHistorySize) {
                historicalData.removeFirst()
            }

            if (historicalData.size == maxHistorySize) {
                val newBins = IntArray(numBins) { 0 } // Create an array of zeros

                historicalData.forEach { value ->
                    val binIndex = (value * (numBins - 1)).toInt().coerceIn(0, numBins - 1)
                    newBins[binIndex]++
                }

                binCounts.clear()
                binCounts.addAll(newBins.toList())
            }

            delay(16L) // Generate new data at ~60fps
        }
    }

    HistogramPlot(
        modifier = Modifier
            .width(400.dp)
            .height(250.dp)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.dp),
        binCounts = binCounts
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF444444)
@Composable
fun ComplexCircularPlotPreview() {
    var complexPoint1 by remember { mutableStateOf(ComplexPlotData(0f, 0f, Color.Cyan)) }
    var complexPoint2 by remember { mutableStateOf(ComplexPlotData(0f, 0f, Color.Yellow)) }
    var time by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            val magnitude1 = 0.8f
            complexPoint1 = complexPoint1.copy(
                real = magnitude1 * cos(time * 1.5f),
                imaginary = magnitude1 * sin(time * 1.5f)
            )

            val magnitude2 = 0.3f + (sin(time * 0.5f) * 0.2f)
            complexPoint2 = complexPoint2.copy(
                real = magnitude2 * cos(time * 3f),
                imaginary = magnitude2 * sin(time * 3f)
            )

            time += 0.02f
            delay(16L) // ~60fps
        }
    }

    ComplexCircularPlot(
        modifier = Modifier
            .size(300.dp)
            .background(Color.Black.copy(alpha = 0.5f)),
        points = listOf(complexPoint1, complexPoint2)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF444444)
@Composable
fun BarGraphPreview() {
    val sampleSeries = listOf(
        PlotData(name = "Volts", dataPoints = listOf(0.75f), colorArgb = 0xFF00FFFF.toInt()),
        PlotData(name = "Amps", dataPoints = listOf(0.40f), colorArgb = 0xFFFF0000.toInt()),
        PlotData(name = "RPM", dataPoints = listOf(0.90f), colorArgb = 0xFF00FF00.toInt()),
        PlotData(name = "Temp", dataPoints = listOf(0.60f), colorArgb = 0xFFFFFF00.toInt())
    )
    BarGraph(
        modifier = Modifier
            .width(400.dp)
            .height(250.dp)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.dp),
        series = sampleSeries,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF444444)
@Composable
fun InteractiveCenterDisplayPreview() {

    val voltsData = remember { mutableStateListOf<Float>() }
    val ampsData = remember { mutableStateListOf<Float>() }
    val rpmData = remember { mutableStateListOf<Float>() }
    val tempData = remember { mutableStateListOf<Float>() }

    val maxDataPoints = 100
    var time by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            val voltsPoint = (sin(time * 1.5f * PI.toFloat()) * 0.4f) + 0.6f
            voltsData.add(voltsPoint)
            if (voltsData.size > maxDataPoints) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    voltsData.removeFirst()
                }
            }

            val ampsPoint = (cos(time * 3f * PI.toFloat()) * 0.2f) + 0.25f
            ampsData.add(ampsPoint)
            if (ampsData.size > maxDataPoints) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    ampsData.removeFirst()
                }
            }

            val rpmSignal = sin(time * 2f * PI.toFloat()) // Use a different frequency
            val rpmPoint = if (rpmSignal >= 0) 0.9f else 0.1f
            rpmData.add(rpmPoint / 2 + 0.25f)
            if (rpmData.size > maxDataPoints) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    rpmData.removeFirst()
                }
            }
            val period = 1f // The wave will repeat every 2 "time" units
            val tempPoint = (time % period) / period
            tempData.add(tempPoint / 2 + 0.25f)
            if (tempData.size > maxDataPoints) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    tempData.removeFirst()
                }
            }

            time += 0.02f
            delay(16L)
        }
    }

    CenterDisplay(
        modifier = Modifier.size(width = 800.dp, height = 400.dp),
        series = listOf(
            PlotData(name = "Volts", dataPoints = voltsData, colorArgb = 0xFF00FFFF.toInt()),
            PlotData(name = "Amps", dataPoints = ampsData, colorArgb = 0xFFFF0000.toInt()),
            PlotData(name = "RMP", dataPoints = rpmData, colorArgb = 0xFF00FF00.toInt()),
            PlotData(name = "Temp", dataPoints = tempData, colorArgb = 0xFFFFFF00.toInt())
        )
    )
}

@Composable
fun ScrollingBarPlot(
    modifier: Modifier = Modifier,
    dataPoints: List<Float>, // Now takes a list of raw data points
    barColor: Color = Color(0xFF4682B4) // SteelBlue
) {
    Row(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp), // Bars are very close
        verticalAlignment = Alignment.Bottom
    ) {
        val last100Points = dataPoints.takeLast(100)

        last100Points.forEach { point ->
            val barHeight = point.coerceIn(0f, 1f)

            GlowingBar(
                modifier = Modifier
                    .weight(1f) // Each bar takes equal width
                    .fillMaxHeight(barHeight),
                color = barColor
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
fun ScrollingBarPlotPreview() {
    val dataStream = remember { mutableStateListOf<Float>() }
    val maxSamples = 100 // We want to display 100 bars
    var time by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            val newDataPoint = (sin(time * 2 * PI.toFloat()) + 1f) / 2f
            dataStream.add(newDataPoint)

            while (dataStream.size > maxSamples) {
                dataStream.removeFirst()
            }
            time += 0.05f
            delay(50L) // Add a new data point every 50ms
        }
    }

    ScrollingBarPlot(
        modifier = Modifier
            .width(400.dp)
            .height(250.dp)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.dp),
        dataPoints = dataStream
    )
}