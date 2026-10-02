package com.fushengce.tasks

import java.net.URI

internal fun hasNavigableLocation(text: String): Boolean = text.isNotBlank() && text.length <= 2000 &&
    (mapLink(text) != null || !text.contains(Regex("[a-zA-Z][a-zA-Z0-9+.-]*://")))

/** Preserve supplier coordinates inside a known map URL; never infer a coordinate system. */
internal fun mapLink(text: String): String? {
    if (text.length > 2000) return null
    val links = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE).findAll(text).toList()
    if (links.size != 1) return null
    val link = links.single().value.trimEnd('。', '，', ')', '）')
    val uri = runCatching { URI(link) }.getOrNull() ?: return null
    val host = uri.host?.lowercase() ?: return null
    if (uri.rawUserInfo != null || uri.port !in listOf(-1, 80, 443)) return null
    val allowed = setOf("map.qq.com", "apis.map.qq.com", "uri.amap.com",
        "surl.amap.com", "amap.com", "map.baidu.com", "j.map.baidu.com")
    return link.takeIf { host in allowed && uri.scheme.lowercase() in setOf("https", "http") }
}
