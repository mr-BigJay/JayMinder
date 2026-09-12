package com.offlinejournal.service.market

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class MarketPrices(
    val usdToman: Long? = null,
    val goldToman: Long? = null
)

class MarketPriceRepository {
    suspend fun fetchPrices(): MarketPrices = withContext(Dispatchers.IO) {
        try {
            val connection = URL(TGJU_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val current = JSONObject(body).getJSONObject("current")
            MarketPrices(
                usdToman = parseTomanPrice(current.optJSONObject("price_dollar_rl")),
                goldToman = parseTomanPrice(current.optJSONObject("geram18"))
            )
        } catch (_: Exception) {
            MarketPrices()
        }
    }

    private fun parseTomanPrice(item: JSONObject?): Long? {
        if (item == null) return null
        val raw = item.optString("p").replace(",", "").trim()
        val rial = raw.toLongOrNull() ?: return null
        return rial / 10
    }

    companion object {
        private const val TGJU_URL = "https://call4.tgju.org/ajax.json"
    }
}
