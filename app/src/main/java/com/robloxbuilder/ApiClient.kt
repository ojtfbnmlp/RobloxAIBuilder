package com.robloxbuilder

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    private const val MODEL = "gemini-2.5-flash"

    private val SYSTEM_PROMPT = """
Bạn là trợ lý build map Roblox Studio (dùng trên Android). Trả lời bằng tiếng Việt.
QUY TẮC BẮT BUỘC:
1. Viết code Luau chạy được ngay trong Roblox Studio. TUYỆT ĐỐI KHÔNG dùng require().
2. Mỗi script tối đa 300 dòng. Nếu cần dài hơn, chia thành nhiều nhóm độc lập: Nhóm 1, Nhóm 2, Nhóm 3... (không giới hạn số nhóm). Mỗi nhóm nằm trong MỘT khối code riêng và chạy độc lập, không phụ thuộc nhóm khác.
3. Trước mỗi khối code ghi rõ: tên nhóm, loại script (Script, LocalScript hoặc Command Bar) và đặt ở đâu (ServerScriptService, Workspace...).
4. Khi cần thông tin mới hoặc không chắc về API Roblox, hãy tìm trên web trước khi trả lời.
5. Ưu tiên code đơn giản, ít lỗi, dễ dán trên điện thoại. Cuối câu trả lời, hướng dẫn ngắn cách chạy.
""".trimIndent()

    private fun toContents(history: JSONArray): JSONArray {
        val out = JSONArray()
        for (i in 0 until history.length()) {
            val m = history.getJSONObject(i)
            val role = if (m.getString("role") == "assistant") "model" else "user"
            val part = JSONObject().put("text", m.getString("content"))
            val item = JSONObject()
            item.put("role", role)
            item.put("parts", JSONArray().put(part))
            out.put(item)
        }
        return out
    }

    private fun call(apiKey: String, history: JSONArray, useSearch: Boolean): String {
        val body = JSONObject()
        val sysPart = JSONObject().put("text", SYSTEM_PROMPT)
        body.put("systemInstruction", JSONObject().put("parts", JSONArray().put(sysPart)))
        body.put("contents", toContents(history))
        if (useSearch) {
            val tool = JSONObject().put("google_search", JSONObject())
            body.put("tools", JSONArray().put(tool))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
            MODEL + ":generateContent"
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 30000
            conn.readTimeout = 180000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-goog-api-key", apiKey)
            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                val msg = try {
                    JSONObject(text).getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    text
                }
                throw RuntimeException("Lỗi " + code + ": " + msg)
            }
            val cands = JSONObject(text).optJSONArray("candidates")
            if (cands == null || cands.length() == 0) {
                return "(AI không trả về nội dung, hãy thử hỏi lại)"
            }
            val parts = cands.getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts")
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                sb.append(parts.getJSONObject(i).optString("text", ""))
            }
            val result = sb.toString()
            return if (result.isBlank()) "(Không có nội dung trả về)" else result
        } finally {
            conn.disconnect()
        }
    }

    fun send(apiKey: String, history: JSONArray): String {
        return try {
            call(apiKey, history, true)
        } catch (e: Exception) {
            call(apiKey, history, false)
        }
    }
}
