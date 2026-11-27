package com.robloxbuilder

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var ctrl: ChatController

    private fun smallBtn(t: String, action: () -> Unit): Button {
        val b = Button(this)
        b.text = t
        b.textSize = 12f
        b.isAllCaps = false
        b.minWidth = 0
        b.minimumWidth = 0
        b.setOnClickListener { action() }
        return b
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F4F5F7"))
        root.fitsSystemWindows = true

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setBackgroundColor(Color.parseColor("#1F2937"))
        val p = Views.dp(this, 8)
        bar.setPadding(p + p / 2, p, p, p)

        val title = TextView(this)
        title.text = "Roblox AI Builder"
        title.textSize = 18f
        title.setTextColor(Color.WHITE)
        title.setTypeface(null, Typeface.BOLD)
        bar.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        bar.addView(smallBtn("API key") { ctrl.askKey() })
        bar.addView(smallBtn("Xoá") { ctrl.reset() })
        root.addView(bar, LinearLayout.LayoutParams(-1, -2))

        val scroll = ScrollView(this)
        val chat = LinearLayout(this)
        chat.orientation = LinearLayout.VERTICAL
        val q = Views.dp(this, 10)
        chat.setPadding(q, q, q, q)
        scroll.addView(chat)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.setBackgroundColor(Color.WHITE)
        row.setPadding(p, p, p, p)
        val input = EditText(this)
        input.hint = "Mô tả map bạn muốn..."
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        input.maxLines = 5
        val sendBtn = Button(this)
        sendBtn.text = "Gửi"
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(sendBtn, LinearLayout.LayoutParams(-2, -2))
        root.addView(row, LinearLayout.LayoutParams(-1, -2))

        setContentView(root)

        ctrl = ChatController(this, chat, scroll, input, sendBtn)
        sendBtn.setOnClickListener { ctrl.send() }
        ctrl.bubble("Xin chào! Hãy mô tả map bạn muốn build. Mình sẽ viết script Luau (không dùng require, tối đa 300 dòng/nhóm).", false)
        if (!ctrl.hasKey()) ctrl.askKey()
    }
}    
            
