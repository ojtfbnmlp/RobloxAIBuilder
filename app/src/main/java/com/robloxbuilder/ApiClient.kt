package com.robloxbuilder

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    private val MODELS = arrayOf(
        "gemini-2.5-flash-lite",
        "gemini-2.5-flash",
        "gemini-2.0-flash"
    )

    class ApiError(val code: Int, msg: String) : RuntimeException(msg)

    private val SYSTEM_PROMPT = """
Bạn là trợ lý AI cho game Roblox, hoạt động giống trợ lý AI của Roblox Studio, chạy trên Android. Trả lời bằng tiếng Việt.
CHỨC NĂNG CHÍNH (khi người dùng hỏi bạn làm được gì thì trả lời đúng các mục này):
1. Build map: địa hình, đảo, thành phố, dungeon, sân chơi, cây, hồ, núi...
2. Tạo và spawn vật thể: nhà, xe, vũ khí, đồ nội thất, cổng, bẫy, NPC đơn giản, vật phẩm...
3. Nếu không xác định được vật thể hoặc cách làm: tra cứu trên web để tìm module hoặc mã mẫu rồi gộp nội dung vào script. Nếu không tìm được thì tự viết từ các Part.
QUY TẮC BẮT BUỘC:
1. Viết code Luau chạy được ngay trong Roblox Studio. TUYỆT ĐỐI KHÔNG dùng require().
2. Mỗi script tối đa 300 dòng. Nếu cần dài hơn, chia thành nhiều nhóm độc lập: Nhóm 1, Nhóm 2, Nhóm 3... (không giới hạn số nhóm). Mỗi nhóm nằm trong MỘT khối code riêng và chạy độc lập, không phụ thuộc nhóm khác.
3. Trước mỗi khối code ghi rõ: tên nhóm, loại script (Script, LocalScript hoặc Command Bar) và đặt ở đâu (ServerScriptService, Workspace...).
4. Ưu tiên code đơn giản, ít lỗi, dễ dán trên điện thoại. Cuối câu trả lời, hướng dẫn ngắn cách chạy.
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

    private fun call(model: String, apiKey: String, history: JSONArray, useSearch: Boolean): String {
        val body = JSONObject()
        val sysPart = JSONObject().put("text", SYSTEM_PROMPT)
        body.put("systemInstruction", JSONObject().put("parts", JSONArray().put(sysPart)))
        body.put("contents", toContents(history))
        if (useSearch) {
            val tool = JSONObject().put("google_search", JSONObject())
            body.put("tools", JSONArray().put(tool))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
            model + ":generateContent"
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 90000
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
                throw ApiError(code, msg)
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

    private fun friendly(e: Exception?): String {
        if (e is ApiError) {
            return when (e.code) {
                429 -> "Hết lượt miễn phí hoặc gửi quá nhanh. Chờ 1-2 phút rồi thử lại."
                500, 502, 503, 504 -> "Máy chủ Gemini đang quá tải. Thử lại sau ít phút."
                400, 401, 403 -> "API key sai hoặc chưa dùng được. Bấm API key để nhập lại. (" + e.message + ")"
                else -> "Lỗi " + e.code + ": " + e.message
            }
        }
        return "Lỗi mạng: " + (e?.message ?: "không xác định")
    }

    fun send(apiKey: String, history: JSONArray): String {
        var last: Exception? = null
        var useSearch = true
        for (model in MODELS) {
            for (attempt in 0 until 2) {
                try {
                    return call(model, apiKey, history, useSearch)
                } catch (e: ApiError) {
                    last = e
                    if (e.code == 401 || e.code == 403) {
                        throw RuntimeException(friendly(e))
                    }
                    if (useSearch) {
                        useSearch = false
                        continue
                    }
                    if (e.code == 400) throw RuntimeException(friendly(e))
                    if (e.code == 404) break
                    Thread.sleep(2000)
                } catch (e: Exception) {
                    last = e
                    useSearch = false
                    Thread.sleep(2000)
                }
            }
        }
        throw RuntimeException(friendly(last))
    }
}
