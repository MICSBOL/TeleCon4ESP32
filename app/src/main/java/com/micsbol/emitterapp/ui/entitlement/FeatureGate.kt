package com.micsbol.emitterapp.ui.entitlement

import androidx.compose.runtime.Composable
import com.micsbol.emitterapp.domain.model.Entitlement
import com.micsbol.emitterapp.domain.model.PremiumFeature
import com.micsbol.emitterapp.domain.model.has

/**
 * Renders [content] when [entitlement] includes [feature]; otherwise [locked].
 */
@Composable
fun FeatureGate(
    feature: PremiumFeature,
    entitlement: Entitlement,
    locked: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    if (entitlement.has(feature)) {
        content()
    } else {
        locked()
    }
}

/**
 * Convenience overload that reads [LocalEntitlement].
 */
@Composable
fun FeatureGate(
    feature: PremiumFeature,
    locked: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    FeatureGate(
        feature = feature,
        entitlement = LocalEntitlement.current,
        locked = locked,
        content = content,
    )
}
