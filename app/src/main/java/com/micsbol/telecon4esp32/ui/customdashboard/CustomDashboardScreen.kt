package com.micsbol.telecon4esp32.ui.customdashboard

import android.content.pm.ActivityInfo
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.applications.ApplicationSettingsIconButton
import com.micsbol.telecon4esp32.ui.control_panel.LockScreenOrientation
import com.micsbol.telecon4esp32.ui.customdashboard.components.CustomDashboardEditorContent
import com.micsbol.telecon4esp32.ui.customdashboard.components.CustomDashboardHomeContent
import com.micsbol.telecon4esp32.ui.customdashboard.displayDashboardName
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme

@Composable
fun CustomDashboardScreen(
    navController: NavController,
    viewModel: CustomDashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

    if (uiState.phase == CustomDashboardScreenPhase.EDITOR) {
        LaunchedEffect(Unit) {
            viewModel.onEditorVisible()
        }
        DisposableEffect(Unit) {
            onDispose {
                viewModel.onEditorHidden()
            }
        }
    }

    when (uiState.phase) {
        CustomDashboardScreenPhase.HOME -> {
            CustomDashboardHomeContent(
                savedDashboards = uiState.savedDashboards,
                pendingDeleteLayoutId = uiState.pendingDeleteLayoutId,
                onBackClick = { navController.navigateUp() },
                onCreateDashboardClick = viewModel::onCreateNewDashboard,
                onOpenDashboardClick = viewModel::onOpenDashboard,
                onDeleteDashboardClick = viewModel::onDeleteDashboardRequested,
                onDeleteDashboardConfirm = viewModel::onDeleteDashboardConfirmed,
                onDeleteDashboardDismiss = viewModel::onDeleteDashboardDismiss,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                topBarActions = {
                    ApplicationSettingsIconButton(
                        applicationId = ApplicationId.CUSTOM_DASHBOARD,
                        navController = navController,
                    )
                },
            )
        }

        CustomDashboardScreenPhase.EDITOR -> {
            BackHandler(onBack = viewModel::onEditorBackPressed)

            val editor = uiState.editor
            if (editor != null) {
                val untitledLabel = stringResource(R.string.custom_dashboard_untitled_layout)
                val layoutName = displayDashboardName(editor.layoutName, untitledLabel)
                CustomDashboardEditorContent(
                    editorState = editor,
                    layoutName = layoutName,
                    onBackClick = viewModel::onBackToHome,
                    onToggleEditMode = viewModel::onToggleEditMode,
                    onSaveLayoutRequested = viewModel::onSaveLayoutRequested,
                    onRenameLayoutRequested = viewModel::onRenameLayoutRequested,
                    onSaveNameChanged = viewModel::onSaveNameChanged,
                    onSaveLayoutConfirmed = viewModel::onSaveLayoutConfirmed,
                    onSaveNameDialogDismiss = viewModel::onSaveNameDialogDismiss,
                    onWidgetSelected = viewModel::onWidgetSelected,
                    onWidgetMoved = viewModel::onWidgetMoved,
                    onWidgetDeleted = viewModel::onWidgetDeleted,
                    onWidgetAdded = viewModel::onWidgetAdded,
                    onJoystickMove = viewModel::onJoystickMove,
                    onJoystickModeChanged = viewModel::onJoystickModeChanged,
                    onJoystickConfigRequest = viewModel::onJoystickConfigRequest,
                    onJoystickConfigDismiss = viewModel::onJoystickConfigDismiss,
                    onRelayToggle = viewModel::onRelayToggle,
                    onOutputLevelChange = viewModel::onOutputLevelChange,
                    onSliderChange = viewModel::onSliderChange,
                    onPushButtonPress = viewModel::onPushButtonPress,
                    onLedToggle = viewModel::onLedToggle,
                    onBluetoothDisconnectedClick = { navController.navigate(Screen.Bluetooth.route) },
                    modifier = Modifier.fillMaxSize(),
                    topBarActions = {
                        ApplicationSettingsIconButton(
                            applicationId = ApplicationId.CUSTOM_DASHBOARD,
                            navController = navController,
                        )
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CustomDashboardHomePreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        CustomDashboardHomeContent(
            savedDashboards = emptyList(),
            pendingDeleteLayoutId = null,
            onBackClick = {},
            onCreateDashboardClick = {},
            onOpenDashboardClick = {},
            onDeleteDashboardClick = {},
            onDeleteDashboardConfirm = {},
            onDeleteDashboardDismiss = {},
        )
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, widthDp = 900, heightDp = 420)
@Composable
private fun CustomDashboardEditorPreview() {
    TeleCon4Esp32Theme(darkTheme = true) {
        CustomDashboardEditorContent(
            editorState = CustomDashboardEditorState(
                layoutId = "preview",
                layoutName = "Workshop Layout",
                plotSeries = demoPlotSeries(
                    List(60) { index ->
                        ((kotlin.math.sin(index / 8.0) * 0.4 + 0.5)).toFloat()
                    },
                ),
            ),
            layoutName = "Workshop Layout",
            onBackClick = {},
            onToggleEditMode = {},
            onSaveLayoutRequested = {},
            onRenameLayoutRequested = {},
            onSaveNameChanged = {},
            onSaveLayoutConfirmed = {},
            onSaveNameDialogDismiss = {},
            onWidgetSelected = {},
            onWidgetMoved = { _, _, _ -> },
            onWidgetDeleted = {},
            onWidgetAdded = { _, _, _ -> },
            onJoystickMove = { _, _, _ -> },
            onJoystickModeChanged = { _, _ -> },
            onJoystickConfigRequest = {},
            onJoystickConfigDismiss = {},
            onRelayToggle = {},
            onOutputLevelChange = { _, _ -> },
            onSliderChange = { _, _ -> },
            onPushButtonPress = {},
            onLedToggle = {},
            onBluetoothDisconnectedClick = {},
        )
    }
}
