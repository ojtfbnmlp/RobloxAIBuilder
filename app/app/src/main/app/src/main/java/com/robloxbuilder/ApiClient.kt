package com.robloxbuilder

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    private const val MODEL = "claude-sonnet-4-6"

    val SYSTEM_PROMPT = """
Bạn là trợ lý build map Roblox Studio (dùng trên Android). Trả lời bằng tiếng Việt.
QUY TẮC BẮT BUỘC:
1. Viết code Luau chạy được ngay trong Roblox Studio. TUYỆT ĐỐI KHÔNG dùng require().
2. Mỗi script tối đa 300 dòng. Nếu cần dài hơn, chia thành nhiều nhóm độc lập: "Nhóm 1", "Nhóm 2", "Nhóm 3"... (không giới hạn số nhóm). Mỗi nhóm nằm trong MỘT khối code riêng ```lua ... ``` và chạy độc lập, không phụ thuộc nhóm khác qua require.
3. Trước mỗi khối code ghi rõ: tên nhóm, loại script (Script / LocalScript / Command Bar) và đặt ở đâu (ServerScriptService, Workspace...).
4. Khi cần thông tin mới hoặc không chắc về API Roblox, hãy dùng công cụ tìm kiếm web trước khi trả lời.
5. Ưu tiên code đơn giản, ít lỗi, dễ dán trên điện thoại. Cuối câu trả lời, hướng dẫn ngắn cách chạy.
""".trimIndent()

    fun send(apiKey: String, history: JSONArray): String {
        val body = JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", 8000)
            put("system", SYSTEM_PROMPT)
            put("messages", history)
            put("tools", JSONArray().put(JSONObject().apply {
                put("type", "web_search_20250305")
                put("name", "web_search")
                put("max_uses", 5)
            }))
        }
        val conn = URL("https://api.anthropic.com/v1/messages")
            .openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 30000
            conn.readTimeout = 180000
            conn.doOutput = true
            conn.setRequestProperty("content-type", "application/json")
            conn.setRequestProperty("x-api-key", apiKey)
            conn.setRequestProperty("anthropic-version", "2023-06-01")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                val msg = try {
                    JSONObject(text).getJSONObject("error").getString("message")
                } catch (e: Exception) { text }
                throw RuntimeException("Lỗi $code: $msg")
            }
            val content = JSONObject(text).getJSONArray("content")
            val sb = StringBuilder()
            for (i in 0 until content.length()) {
                val b = content.getJSONObject(i)
                if (b.getString("type") == "text") sb.append(b.getString("text"))
            }
            return sb.toString().ifBlank { "(Không có nội dung trả về)" }
        } finally {
            conn.disconnect()
        }
    }
}
