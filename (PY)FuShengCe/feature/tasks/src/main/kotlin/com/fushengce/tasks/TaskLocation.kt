package com.fushengce.tasks

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

internal fun openTaskLocation(context: Context, address: String): String? {
    if (address.isBlank()) return "请先填写或粘贴地点"
    val link = mapLink(address)
    if (link == null && address.contains(Regex("[a-zA-Z][a-zA-Z0-9+.-]*://"))) {
        return "暂不支持这个链接，请从微信复制地点名称和完整地址"
    }
    val destination = link?.let(Uri::parse) ?: Uri.parse("geo:0,0?q=${Uri.encode(address.trim())}")
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, destination))
        null
    } catch (_: ActivityNotFoundException) {
        "未找到可打开此地点的地图应用，请安装地图后重试"
    } catch (_: SecurityException) {
        "地图暂时无法打开，请复制地址到地图中查找"
    }
}
