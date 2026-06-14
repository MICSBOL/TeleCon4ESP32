package com.micsbol.telecon4esp32.ui.entitlement

import androidx.compose.runtime.compositionLocalOf
import com.micsbol.telecon4esp32.domain.model.Entitlement

/**
 * Current user entitlement for Compose trees under [AppNavGraph][com.micsbol.telecon4esp32.ui.navigation.AppNavGraph].
 *
 * Defaults to [Entitlement.Free] so previews and tests work without a provider.
 */
val LocalEntitlement = compositionLocalOf<Entitlement> { Entitlement.Free }
