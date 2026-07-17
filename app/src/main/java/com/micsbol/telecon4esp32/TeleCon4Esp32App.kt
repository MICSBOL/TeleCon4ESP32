package com.micsbol.telecon4esp32

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import com.google.android.gms.ads.MobileAds
import com.micsbol.telecon4esp32.ui.ads.InterstitialAdManager
import com.micsbol.telecon4esp32.ui.ads.RewardedAdManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TeleCon4Esp32App : Application() {

    @Inject
    lateinit var interstitialAdManager: InterstitialAdManager

    @Inject
    lateinit var rewardedAdManager: RewardedAdManager

    override fun onCreate() {
        super.onCreate()
        clearLegacyForcedAppLocaleIfNeeded()
        MobileAds.initialize(this)
        interstitialAdManager.preload()
        rewardedAdManager.preload()
    }

    /**
     * Older installs could end up with a stuck per-app locale (e.g. English) that
     * overrides the system language. Clear it once so the UI follows the device
     * language; users can still pick English/Spanish later in system App Language.
     */
    private fun clearLegacyForcedAppLocaleIfNeeded() {
        val prefs = getSharedPreferences(LOCALE_MIGRATION_PREFS, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_CLEARED_FORCED_APP_LOCALE, false)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = getSystemService(LocaleManager::class.java)
            if (!localeManager.applicationLocales.isEmpty) {
                localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
            }
        }

        prefs.edit().putBoolean(KEY_CLEARED_FORCED_APP_LOCALE, true).apply()
    }

    private companion object {
        const val LOCALE_MIGRATION_PREFS = "locale_migration"
        const val KEY_CLEARED_FORCED_APP_LOCALE = "cleared_forced_app_locale"
    }
}
