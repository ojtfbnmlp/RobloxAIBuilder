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
        val text = if (raw.isEmpty()) "Hãy build map chi tiết và đẹp theo ảnh này." else raw
        input.setText("")
        pendingImage = null
        bubble(text, true)
        val msg = JSONObject().put("role", "user").put("content", text)
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
