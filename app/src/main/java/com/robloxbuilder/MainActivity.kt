    private fun smallBtn(t: String, click: () -> Unit) = Button(this).apply {
        text = t; textSize = 12f; isAllCaps = false
        minWidth = 0; minimumWidth = 0
        setOnClickListener { click() }
    }

    private fun askKey() {
        val et = EditText(this).apply {
            hint = "sk-ant-..."
            setText(prefs.getString("key", ""))
            setSingleLine()
        }
        AlertDialog.Builder(this)
            .setTitle("Anthropic API key")
            .setMessage("Lấy key ở console.anthropic.com. Key chỉ lưu trên máy bạn.")
            .setView(et)
            .setPositiveButton("Lưu") { _, _ ->
                prefs.edit().putString("key", et.text.toString().trim()).apply()
            }
            .setNegativeButton("Huỷ", null).show()
    }

    private fun resetChat() {
        history = JSONArray()
        chat.removeAllViews()
        addBubble("Đã xoá hội thoại. Hãy nhập yêu cầu mới.", false)
    }

    private fun onSend() {
        val text = input.text.toString().trim()
        val key = prefs.getString("key", "") ?: ""
        if (text.isEmpty() || busy) return
        if (key.isBlank()) { askKey(); return }
        input.setText("")
        addBubble(text, true)
        history.put(JSONObject().put("role", "user").put("content", text))
        setBusy(true)
        val loading = addBubble("Đang suy nghĩ và tìm kiếm...", false)
        thread {
            try {
                val reply = ApiClient.send(key, history)
                history.put(JSONObject().put("role", "assistant").put("content", reply))
                runOnUiThread { chat.removeView(loading); renderReply(reply) }
            } catch (e: Exception) {
                history.remove(history.length() - 1)
                runOnUiThread {
                    chat.removeView(loading)
                    addBubble("⚠ " + (e.message ?: "Lỗi không xác định"), false)
                }
            } finally {
                runOnUiThread { setBusy(false) }
            }
        }
    }

    private fun setBusy(b: Boolean) { busy = b; sendBtn.isEnabled = !b }    private fun renderReply(reply: String) {
        val re = Regex("```[a-zA-Z0-9]*\\n([\\s\\S]*?)```")
        var last = 0
        for (m in re.findAll(reply)) {
            val before = reply.substring(last, m.range.first).trim()
            if (before.isNotEmpty()) addBubble(before, false)
            addCode(m.groupValues[1].trimEnd())
            last = m.range.last + 1
        }
        val rest = reply.substring(last).trim()
        if (rest.isNotEmpty()) addBubble(rest, false)
    }

    private fun addBubble(t: String, mine: Boolean): View {
        val tv = TextView(this).apply {
            text = t; textSize = 15f
            setTextIsSelectable(true)
            setTextColor(if (mine) Color.WHITE else Color.parseColor("#111827"))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor(if (mine) "#2563EB" else "#FFFFFF"))
            }
        }
        val lp = LinearLayout.LayoutParams(-2, -2).apply {
            topMargin = dp(6)
            gravity = if (mine) Gravity.END else Gravity.START
        }
        chat.addView(tv, lp)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        return tv
    }

    private fun addCode(code: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor("#111827"))
            }
        }
        val lines = code.count { it == '\n' } + 1
        box.addView(TextView(this).apply {
            text = "$lines dòng"; textSize = 11f
            setTextColor(Color.parseColor("#9CA3AF"))
        })
        val hs = HorizontalScrollView(this)
        hs.addView(TextView(this).apply {
            text = code; textSize = 12f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#E5E7EB"))
            setTextIsSelectable(true)
        })
        box.addView(hs)
        box.addView(Button(this).apply {
            text = "Copy"; isAllCaps = false
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("lua", code))
                Toast.makeText(this@MainActivity, "Đã copy", Toast.LENGTH_SHORT).show()
            }
        })
        chat.addView(box, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }
}package com.robloxbuilder

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var chat: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var sendBtn: Button
    private var history = JSONArray()
    private var busy = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private val prefs by lazy { getSharedPreferences("cfg", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        addBubble("Xin chào! Hãy mô tả map bạn muốn build. Mình sẽ viết script Luau (không dùng require, tối đa 300 dòng/nhóm).", false)
        if (prefs.getString("key", "").isNullOrBlank()) askKey()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F4F5F7"))
            fitsSystemWindows = true
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#1F2937"))
            setPadding(dp(12), dp(8), dp(8), dp(8))
        }
        bar.addView(TextView(this).apply {
            text = "Roblox AI Builder"
            setTextColor(Color.WHITE); textSize = 18f
            setTypeface(null, Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        bar.addView(smallBtn("API key") { askKey() })
        bar.addView(smallBtn("Xoá") { resetChat() })
        root.addView(bar, LinearLayout.LayoutParams(-1, -2))

        scroll = ScrollView(this)
        chat = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        scroll.addView(chat)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setBackgroundColor(Color.WHITE)
        }
        input = EditText(this).apply {
            hint = "Mô tả map bạn muốn..."
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 5
        }
        sendBtn = Button(this).apply { text = "Gửi"; setOnClickListener { onSend() } }
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(sendBtn, LinearLayout.LayoutParams(-2, -2))
        root.addView(row, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
    }
