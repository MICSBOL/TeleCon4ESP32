@file:Suppress("DEPRECATION")
package com.example.emitterapp.data.repository

/**
 * @deprecated Moved to [com.example.emitterapp.domain.repository.ISettingsRepository].
 * This typealias exists only for backward-compatibility during migration.
 */
@Deprecated(
    message = "Use com.example.emitterapp.domain.repository.ISettingsRepository instead",
    replaceWith = ReplaceWith(
        "ISettingsRepository",
        "com.example.emitterapp.domain.repository.ISettingsRepository"
    )
)
typealias ISettingsRepository = com.example.emitterapp.domain.repository.ISettingsRepository
