package com.micsbol.telecon4esp32.util

import com.micsbol.telecon4esp32.BuildConfig

/** Appends a version query so PDF viewers and CDNs fetch the latest hosted document. */
fun hostedPdfUrl(baseUrl: String): String {
    val separator = if ('?' in baseUrl) "&" else "?"
    return "$baseUrl${separator}v=${BuildConfig.VERSION_CODE}"
}
