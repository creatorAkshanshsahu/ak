package com.akshansh.aktv

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object LiveTvApi {
    private const val BASE = "https://livetgtv.lovable.app/api/public/v2/channels"

    suspend fun search(query: String = ""): List<Channel> {
        val url = if (query.isBlank()) BASE else "$BASE?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
        val text = httpGet(url)
        return parseChannels(text)
    }

    private fun httpGet(urlString: String): String {
        val c = URL(urlString).openConnection() as HttpURLConnection
        c.connectTimeout = 12000
        c.readTimeout = 15000
        c.requestMethod = "GET"
        c.setRequestProperty("Accept", "application/json")
        return c.inputStream.bufferedReader().use { it.readText() }.also { c.disconnect() }
    }

    private fun parseChannels(text: String): List<Channel> {
        val root = JSONObject(text)
        val array = findArray(root) ?: return emptyList()
        val result = mutableListOf<Channel>()

        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val id = first(o, "id", "channel_id", "channelId", "uuid") ?: i.toString()
            val name = first(o, "name", "title", "channel_name", "channelName") ?: "Channel $id"
            val logo = first(o, "logo", "logo_url", "logoUrl", "image", "image_url", "poster")
            val stream = first(
                o, "stream_url", "streamUrl", "stream", "url", "play_url", "playUrl",
                "hls_url", "hlsUrl", "m3u8", "source"
            )
            val category = first(o, "category", "category_name", "categoryName", "group", "genre")
            result.add(Channel(id, name, logo, stream, category))
        }
        return result.distinctBy { it.id }
    }

    private fun findArray(o: JSONObject): JSONArray? {
        listOf("channels", "data", "results", "items").forEach { key ->
            val a = o.optJSONArray(key)
            if (a != null) return a
        }
        val dataObj = o.optJSONObject("data")
        if (dataObj != null) {
            listOf("channels", "results", "items").forEach { key ->
                val a = dataObj.optJSONArray(key)
                if (a != null) return a
            }
        }
        return null
    }

    private fun first(o: JSONObject, vararg keys: String): String? {
        for (key in keys) {
            val v = o.opt(key)
            if (v != null && v != JSONObject.NULL) {
                val s = v.toString().trim()
                if (s.isNotEmpty() && s != "null") return s
            }
        }
        return null
    }
}
