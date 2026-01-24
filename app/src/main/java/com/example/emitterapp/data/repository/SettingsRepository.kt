package com.example.emitterapp.data.repository
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.emitterapp.ui.rc_screen.components.JoystickMode
import com.example.emitterapp.ui.rc_screen.components.JoystickMode.Companion.toStringRepresentation
import com.example.emitterapp.ui.rc_settings.SettingsState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private object PreferencesKeys {
    val LEFT_STICK_MODE = stringPreferencesKey("left_stick_mode")
    val RIGHT_STICK_MODE = stringPreferencesKey("right_stick_mode")
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rc_settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    val settingsFlow: Flow<SettingsState> = context.dataStore.data.map { preferences ->
        val leftModeString = preferences[PreferencesKeys.LEFT_STICK_MODE]
        val rightModeString = preferences[PreferencesKeys.RIGHT_STICK_MODE]

        SettingsState(
            leftStickMode = JoystickMode.fromString(leftModeString),
            rightStickMode = JoystickMode.fromString(rightModeString)
        )
    }

    suspend fun saveLeftStickMode(mode: JoystickMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LEFT_STICK_MODE] = mode.toStringRepresentation()
        }
    }

    suspend fun saveRightStickMode(mode: JoystickMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RIGHT_STICK_MODE] = mode.toStringRepresentation()
        }
    }
}