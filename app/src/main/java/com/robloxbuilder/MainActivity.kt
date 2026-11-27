package com.robloxbuilder

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
