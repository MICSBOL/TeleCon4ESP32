package com.micsbol.telecon4esp32.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.micsbol.telecon4esp32.domain.model.DashboardWidgetPlacement
import com.micsbol.telecon4esp32.domain.model.DashboardWidgetType
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.JoystickMode.Companion.toStringRepresentation
import com.micsbol.telecon4esp32.domain.model.SavedDashboardLayout
import com.micsbol.telecon4esp32.domain.repository.ICustomDashboardRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.customDashboardDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "custom_dashboards",
)

private object CustomDashboardPreferencesKeys {
    val SAVED_LAYOUTS = stringPreferencesKey("saved_layouts_json")
}

@Singleton
class CustomDashboardRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : ICustomDashboardRepository {

    override val savedDashboardsFlow: Flow<List<SavedDashboardLayout>> =
        context.customDashboardDataStore.data.map { preferences ->
            decodeLayouts(preferences[CustomDashboardPreferencesKeys.SAVED_LAYOUTS])
        }

    override suspend fun saveDashboards(layouts: List<SavedDashboardLayout>) {
        context.customDashboardDataStore.edit { preferences ->
            preferences[CustomDashboardPreferencesKeys.SAVED_LAYOUTS] = encodeLayouts(layouts)
        }
    }
}

private fun encodeLayouts(layouts: List<SavedDashboardLayout>): String {
    val array = JSONArray()
    layouts.forEach { layout -> array.put(layout.toJson()) }
    return array.toString()
}

private fun decodeLayouts(raw: String?): List<SavedDashboardLayout> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                add(array.getJSONObject(index).toSavedDashboardLayout())
            }
        }
    }.getOrDefault(emptyList())
}

private fun SavedDashboardLayout.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("widgets", JSONArray().apply { widgets.forEach { put(it.toJson()) } })
}

private fun DashboardWidgetPlacement.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("type", type.name)
    put("column", column)
    put("row", row)
    put("columnSpan", columnSpan)
    put("rowSpan", rowSpan)
    put("joystickMode", joystickMode.toStringRepresentation())
    put("isOn", isOn)
    put("level", level.toDouble())
    put("isPressed", isPressed)
    put("readoutValue", readoutValue.toDouble())
}

private fun JSONObject.toSavedDashboardLayout(): SavedDashboardLayout {
    val widgetsArray = getJSONArray("widgets")
    val widgets = buildList {
        for (index in 0 until widgetsArray.length()) {
            add(widgetsArray.getJSONObject(index).toWidgetPlacement())
        }
    }
    return SavedDashboardLayout(
        id = getString("id"),
        name = optString("name", "").trim(),
        widgets = widgets,
    )
}

private fun JSONObject.toWidgetPlacement(): DashboardWidgetPlacement {
    val type = runCatching { DashboardWidgetType.valueOf(getString("type")) }
        .getOrDefault(DashboardWidgetType.JOYSTICK)
    return DashboardWidgetPlacement(
        id = getString("id"),
        type = type,
        column = getInt("column"),
        row = getInt("row"),
        columnSpan = optInt("columnSpan", type.defaultColumnSpan()),
        rowSpan = optInt("rowSpan", type.defaultRowSpan()),
        joystickMode = JoystickMode.fromString(optString("joystickMode")),
        isOn = optBoolean("isOn", type.defaultIsOn()),
        level = optDouble("level", type.defaultLevel().toDouble()).toFloat(),
        isPressed = optBoolean("isPressed", false),
        readoutValue = optDouble("readoutValue", 3.30).toFloat(),
    )
}

private fun DashboardWidgetType.defaultColumnSpan(): Int = when (this) {
    DashboardWidgetType.JOYSTICK -> 3
    DashboardWidgetType.SLIDER -> 3
    DashboardWidgetType.WAVEFORM -> 4
    DashboardWidgetType.VALUE_READOUT -> 3
    DashboardWidgetType.RELAY -> 2
    DashboardWidgetType.OUTPUT_KNOB -> 3
    DashboardWidgetType.PUSH_BUTTON -> 2
    DashboardWidgetType.LED_INDICATOR -> 2
}

private fun DashboardWidgetType.defaultIsOn(): Boolean = when (this) {
    DashboardWidgetType.LED_INDICATOR -> true
    DashboardWidgetType.RELAY -> false
    else -> false
}

private fun DashboardWidgetType.defaultLevel(): Float = when (this) {
    DashboardWidgetType.SLIDER -> 0.45f
    DashboardWidgetType.OUTPUT_KNOB -> 0.65f
    else -> 0.5f
}

private fun DashboardWidgetType.defaultRowSpan(): Int = when (this) {
    DashboardWidgetType.JOYSTICK -> 3
    DashboardWidgetType.SLIDER -> 2
    DashboardWidgetType.WAVEFORM -> 3
    DashboardWidgetType.VALUE_READOUT -> 2
    DashboardWidgetType.RELAY -> 1
    DashboardWidgetType.OUTPUT_KNOB -> 3
    DashboardWidgetType.PUSH_BUTTON -> 1
    DashboardWidgetType.LED_INDICATOR -> 1
}
