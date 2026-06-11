package com.micsbol.emitterapp.ui.entitlement

import androidx.compose.runtime.compositionLocalOf
import com.micsbol.emitterapp.domain.model.Entitlement

/**
 * Current user entitlement for Compose trees under [AppNavGraph][com.micsbol.emitterapp.ui.navigation.AppNavGraph].
 *
 * Defaults to [Entitlement.Free] so previews and tests work without a provider.
 */
val LocalEntitlement = compositionLocalOf<Entitlement> { Entitlement.Free }
