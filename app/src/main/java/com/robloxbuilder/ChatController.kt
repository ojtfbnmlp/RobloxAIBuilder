package com.robloxbuilder

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

class ChatController(
    private val ctx: Context,
    private val chat: LinearLayout,
    private val scroll: ScrollView,
    private val input: EditText,
    private val sendBtn: Button,
    private val onNeedKey: () -> Unit
) {
    private val FIX_HEAD = "[SỬA LỖI]"
    private val FIX_FIELD1 = "Lỗi (dán từ cửa sổ Output):\n"
    private val FIX_TEMPLATE = "[SỬA LỖI]\nLỗi (dán từ cửa sổ Output):\n\nScript bị lỗi (dán code):\n"
    private val FIX_RULES = "Bạn đang ở chế độ SỬA LỖI. Làm theo thứ tự: " +
        "1) Nói ngắn gọn nguyên nhân lỗi (1 đến 3 câu). " +
        "2) Đưa bản script ĐÃ SỬA ĐẦY ĐỦ trong khối code, không chỉ đoạn vá, không dùng require, " +
        "tối đa 300 dòng mỗi script, dài hơn thì chia Nhóm 1, Nhóm 2, Nhóm 3. " +
        "3) Ghi rõ đã sửa dòng nào và vì sao. " +
        "4) Nếu thiếu thông tin (chưa có script hoặc chưa có thông báo lỗi) thì hỏi lại đúng phần còn thiếu, đừng đoán bừa. " +
        "5) Nếu có ảnh chụp lỗi thì đọc lỗi trong ảnh.\n\n"

    private var history = JSONArray()
    private var busy = false
    private var pendingImage: String? = null
    private val ui = Handler(Looper.getMainLooper())
    private val prefs = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    fun hasKey(): Boolean = !prefs.getString("key", "").isNullOrBlank()

    private fun scrollDown() {
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    fun bubble(t: String, mine: Boolean): View {
        val clean = if (mine) t else t.replace("**", "")
        val v = Views.bubble(ctx, clean, mine)
        chat.addView(v)
        scrollDown()
        return v
    }

    fun startFix() {
        input.setText(FIX_TEMPLATE)
        input.setSelection(FIX_HEAD.length + 1 + FIX_FIELD1.length)
        input.requestFocus()
        bubble("Chế độ sửa lỗi: dán lỗi từ Output và code script vào ô nhập rồi bấm Gửi. Có thể bấm nút ảnh để gửi ảnh chụp lỗi.", false)
    }

    fun pickImage() {
        ImageHolder.listener = { b64 -> ui.post { setImage(b64) } }
        val i = Intent(ctx, PickImageActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    }

    private fun setImage(b64: String) {
        if (b64.isEmpty()) {
            bubble("Không đọc được ảnh, hãy thử ảnh khác.", false)
            return
        }
        pendingImage = b64
        chat.addView(ImageHolder.thumb(ctx, b64))
        bubble("Đã đính kèm ảnh. Nhập yêu cầu rồi bấm Gửi (hoặc bấm Gửi luôn để build theo ảnh).", false)
    }

    fun reset() {
        history = JSONArray()
        pendingImage = null
        chat.removeAllViews()
        bubble("Đã xoá hội thoại. Hãy nhập yêu cầu mới.", false)
    }

    fun send() {
        val raw = input.text.toString().trim()
        val img = pendingImage
        val key = prefs.getString("key", "") ?: ""
        if ((raw.isEmpty() && img == null) || busy) return
        if (key.isBlank()) {
            onNeedKey()
            return
        }
        if (raw == FIX_TEMPLATE.trim() && img == null) {
            bubble("Bạn chưa dán lỗi hoặc script. Hãy dán vào ô nhập rồi bấm Gửi.", false)
            return
        }
        val text = if (raw.isEmpty()) "Hãy build map chi tiết và đẹp theo ảnh này." else raw
        val sendText = if (text.startsWith(FIX_HEAD)) FIX_RULES + text else text
        input.setText("")
        pendingImage = null
        bubble(text, true)
        val msg = JSONObject().put("role", "user").put("content", sendText)
        if (img != null) msg.put("image", img)
        history.put(msg)
        busy = true
        sendBtn.isEnabled = false
        val loading = bubble("Đang suy nghĩ...", false)
        thread {
            try {
                val reply = ApiClient.send(key, history)
                history.put(JSONObject().put("role", "assistant").put("content", reply))
                ui.post {
                    chat.removeView(loading)
                    render(reply)
                }
            } catch (e: Exception) {
                history.remove(history.length() - 1)
                ui.post {
                    chat.removeView(loading)
                    bubble(e.message ?: "Lỗi không xác định", false)
                }
            } finally {
                ui.post {
                    busy = false
                    sendBtn.isEnabled = true
                }
            }
        }
    }

    private fun render(reply: String) {
        val fence = "\u0060\u0060\u0060"
        val re = Regex(fence + "[a-zA-Z0-9]*\\n([\\s\\S]*?)" + fence)
        var last = 0
        for (m in re.findAll(reply)) {
            val before = reply.substring(last, m.range.first).trim()
            if (before.isNotEmpty()) bubble(before, false)
            chat.addView(Views.codeBox(ctx, m.groupValues[1].trimEnd()))
            last = m.range.last + 1
        }
        val rest = reply.substring(last).trim()
        if (rest.isNotEmpty()) bubble(rest, false)
        scrollDown()
    }
}
