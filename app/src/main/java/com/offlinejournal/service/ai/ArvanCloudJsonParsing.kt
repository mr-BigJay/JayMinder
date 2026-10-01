package com.offlinejournal.service.ai

import org.json.JSONArray
import org.json.JSONObject

internal object ArvanCloudJsonParsing {

    fun parseModelsResponse(json: String): List<ArvanModelInfo> {
        val root = JSONObject(json)
        val data = root.optJSONArray("data") ?: JSONArray()
        return buildList {
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val id = item.optString("id").trim()
                if (id.isEmpty()) continue
                add(
                    ArvanModelInfo(
                        id = id,
                        ownedBy = item.optString("owned_by").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }

    fun parseChatCompletionResponse(json: String): String {
        val root = JSONObject(json)
        val choices = root.optJSONArray("choices")
            ?: throw ArvanCloudApiException(
                httpStatus = 200,
                message = "پاسخ chat/completions فاقد choices است",
                responseBody = json
            )
        if (choices.length() == 0) {
            throw ArvanCloudApiException(200, "choices خالی است", json)
        }
        val message = choices.getJSONObject(0).optJSONObject("message")
            ?: throw ArvanCloudApiException(200, "message در choices یافت نشد", json)
        return message.optString("content").trim()
    }

    fun parseBackendCleanupResponse(json: String): String {
        val root = JSONObject(json)
        val text = root.optString("text").trim()
        if (text.isNotEmpty()) return text
        val cleaned = root.optString("cleanedText").trim()
        if (cleaned.isNotEmpty()) return cleaned
        throw ArvanCloudApiException(200, "پاسخ backend proxy فاقد text است", json)
    }

    fun parseErrorMessage(body: String): String? {
        if (body.isBlank()) return null
        return try {
            val obj = JSONObject(body)
            obj.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                ?: obj.optString("error").takeIf { it.isNotBlank() }
                ?: obj.optString("message").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            body.take(200)
        }
    }
}
