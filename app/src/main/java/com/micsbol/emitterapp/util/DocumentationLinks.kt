package com.micsbol.emitterapp.util

import com.micsbol.emitterapp.BuildConfig

/** Appends a version query so PDF viewers and CDNs fetch the latest hosted document. */
fun hostedPdfUrl(baseUrl: String): String {
    val separator = if ('?' in baseUrl) "&" else "?"
    return "$baseUrl${separator}v=${BuildConfig.VERSION_CODE}"
}
