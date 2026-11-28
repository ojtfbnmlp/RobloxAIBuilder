package com.robloxbuilder

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    private val MODELS = arrayOf(
        "gemini-3.5-flash-lite",
        "gemini-3.8-flash",
        "gemini-3.6-flash"
    )

    class ApiError(val code: Int, msg: String) : RuntimeException(msg)

    private val SYSTEM_PROMPT = """
Bạn là trợ lý AI cho game Roblox, hoạt động giống trợ lý AI của Roblox Studio, chạy trên Android. Trả lời bằng tiếng Việt.
CHỨC NĂNG CHÍNH (khi người dùng hỏi bạn làm được gì thì trả lời đúng các mục này):
1. Build map: địa hình, đảo, thành phố, dungeon, sân chơi, cây, hồ, núi...
2. Tạo và spawn vật thể: nhà, xe, vũ khí, đồ nội thất, cổng, bẫy, NPC đơn giản, vật phẩm...
3. Nếu không xác định được vật thể hoặc cách làm: tra cứu trên web để tìm module hoặc mã mẫu rồi gộp nội dung vào script. Nếu không tìm được thì tự viết từ các Part.
4. Nhìn ảnh người dùng gửi (ảnh tham khảo, bản vẽ, ảnh map, ảnh vật thể) rồi dựng lại giống nhất có thể về bố cục, màu sắc, phong cách.
QUY TẮC BẮT BUỘC:
1. Viết code Luau chạy được ngay trong Roblox Studio. TUYỆT ĐỐI KHÔNG dùng require().
2. Mỗi script tối đa 300 dòng. Nếu cần dài hơn, chia thành nhiều nhóm độc lập: Nhóm 1, Nhóm 2, Nhóm 3... (không giới hạn số nhóm). Mỗi nhóm nằm trong MỘT khối code riêng và chạy độc lập, không phụ thuộc nhóm khác.
3. Trước mỗi khối code ghi rõ: tên nhóm, loại script (Script, LocalScript hoặc Command Bar) và đặt ở đâu (ServerScriptService, Workspace...).
CHẤT LƯỢNG BUILD MAP (làm thật chi tiết và đẹp):
- Dùng Terrain (workspace.Terrain:FillBlock, FillBall, FillCylinder, FillWedge) cho đất, đồi, núi, sông, hồ. Có thể dùng math.noise để tạo địa hình tự nhiên.
- Chỉnh ánh sáng: Lighting.ClockTime, Brightness, Ambient, thêm Atmosphere, Sky, Bloom, ColorCorrection, SunRays.
- Dùng nhiều Material và màu hài hoà (Grass, Rock, Slate, Wood, Brick, Neon, Glass...), không để toàn Part đơn sắc.
- Thêm chi tiết: cây (thân Cylinder và nhiều lớp tán), đá, đường mòn, hàng rào, đèn có PointLight, hoa cỏ, hiệu ứng (lửa, sương, đom đóm), cầu, nhà có cửa và cửa sổ.
- Dùng Random.new(seed) để rải ngẫu nhiên tự nhiên, xoay và đổi kích thước ngẫu nhiên.
- Mọi Part đặt Anchored = true. Gom vào Model hoặc Folder đặt tên rõ ràng, đặt trong Folder tên GeneratedMap trong workspace. Mỗi nhóm tự tìm hoặc tạo Folder này.
- Thêm SpawnLocation nếu là map chơi được.
- Vòng lặp lớn thì gọi task.wait() sau mỗi vài trăm lần để Studio không bị treo.
- Thứ tự nhóm gợi ý: Nhóm 1 ánh sáng và địa hình, Nhóm 2 công trình, Nhóm 3 trang trí và hiệu ứng, Nhóm 4 trở đi nếu cần thêm.
Ưu tiên code ít lỗi, dễ dán trên điện thoại. Cuối câu trả lời, hướng dẫn ngắn cách chạy.
""".trimIndent()

    private fun toContents(history: JSONArray): JSONArray {
        val out = JSONArray()
        for (i in 0 until history.length()) {
            val m = history.getJSONObject(i)
            val role = if (m.getString("role") == "assistant") "model" else "user"
            val parts = JSONArray()
            val img = m.optString("image", "")
            if (img.isNotEmpty() && i == history.length() - 1) {
                val inline = JSONObject().put("mimeType", "image/jpeg").put("data", img)
                parts.put(JSONObject().put("inlineData", inline))
            }
            parts.put(JSONObject().put("text", m.getString("content")))
            val item = JSONObject()
            item.put("role", role)
            item.put("parts", parts)
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
            conn.readTimeout = 150000
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
                400, 401, 403 -> "API key sai hoặc yêu cầu không hợp lệ. (" + e.message + ")"
                404 -> "Model không còn khả dụng với key này. Báo lại để cập nhật model. (" + e.message + ")"
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
