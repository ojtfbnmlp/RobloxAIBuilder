package com.robloxbuilder

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

class ChatController(
    private val act: Activity,
    private val chat: LinearLayout,
    private val scroll: ScrollView,
    private val input: EditText,
    private val sendBtn: Button
) {
    private var history = JSONArray()
    private var busy = false
    private val prefs = act.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    fun hasKey(): Boolean = !prefs.getString("key", "").isNullOrBlank()

    private fun scrollDown() {
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    fun bubble(t: String, mine: Boolean): View {
        val v = Views.bubble(act, t, mine)
        chat.addView(v)
        scrollDown()
        return v
    }

    fun askKey() {
        val et = EditText(act)
        et.hint = "sk-ant-..."
        et.setText(prefs.getString("key", ""))
        et.setSingleLine()
        AlertDialog.Builder(act)
            .setTitle("Anthropic API key")
            .setMessage("Lấy key ở console.anthropic.com. Key chỉ lưu trên máy bạn.")
            .setView(et)
            .setPositiveButton("Lưu") { _, _ ->
                prefs.edit().putString("key", et.text.toString().trim()).apply()
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    fun reset() {
        history = JSONArray()
        chat.removeAllViews()
        bubble("Đã xoá hội thoại. Hãy nhập yêu cầu mới.", false)
    }

    fun send() {
        val text = input.text.toString().trim()
        val key = prefs.getString("key", "") ?: ""
        if (text.isEmpty() || busy) return
        if (key.isBlank()) {
            askKey()
            return
        }
        input.setText("")
        bubble(text, true)
        history.put(JSONObject().put("role", "user").put("content", text))
        busy = true
        sendBtn.isEnabled = false
        val loading = bubble("Đang suy nghĩ và tìm kiếm...", false)
        thread {
            try {
                val reply = ApiClient.send(key, history)
                history.put(JSONObject().put("role", "assistant").put("content", reply))
                act.runOnUiThread {
                    chat.removeView(loading)
                    render(reply)
                }
            } catch (e: Exception) {
                history.remove(history.length() - 1)
                act.runOnUiThread {
                    chat.removeView(loading)
                    bubble("Lỗi: " + (e.message ?: "không xác định"), false)
                }
            } finally {
                act.runOnUiThread {
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
            chat.addView(Views.codeBox(act, m.groupValues[1].trimEnd()))
            last = m.range.last + 1
        }
        val rest = reply.substring(last).trim()
        if (rest.isNotEmpty()) bubble(rest, false)
        scrollDown()
    }
}
