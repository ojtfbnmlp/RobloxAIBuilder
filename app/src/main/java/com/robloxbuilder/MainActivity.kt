package com.robloxbuilder

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var ctrl: ChatController
    private val prefs by lazy { getSharedPreferences("cfg", Context.MODE_PRIVATE) }

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

    private fun askKey() {
        val et = EditText(this)
        et.hint = "Dán API key Gemini"
        et.setText(prefs.getString("key", ""))
        et.setSingleLine()
        AlertDialog.Builder(this)
            .setTitle("API key Gemini")
            .setMessage("Lấy key miễn phí ở aistudio.google.com/apikey. Key chỉ lưu trên máy bạn.")
            .setView(et)
            .setPositiveButton("Lưu") { _, _ ->
                prefs.edit().putString("key", et.text.toString().trim()).apply()
                if (!OverlayService.running) startOverlay()
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Hãy bật Hiển thị trên ứng dụng khác cho Roblox AI Builder, rồi quay lại bấm nút Bong bóng",
                Toast.LENGTH_LONG
            ).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName))
            )
            return
        }
        startService(Intent(this, OverlayService::class.java))
        moveTaskToBack(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.fitsSystemWindows = true

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setBackgroundColor(Color.parseColor("#1F2937"))
        val p = Views.dp(this, 8)
        bar.setPadding(p + p / 2, p, p, p)

        val title = TextView(this)
        title.text = "Roblox AI"
        title.textSize = 18f
        title.setTextColor(Color.WHITE)
        title.setTypeface(null, Typeface.BOLD)
        bar.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        bar.addView(smallBtn("Bong bóng") { startOverlay() })
        bar.addView(smallBtn("Ảnh") { ctrl.pickImage() })
        bar.addView(smallBtn("API key") { askKey() })
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
        input.hint = "Mô tả map hoặc vật thể..."
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        input.maxLines = 5
        val sendBtn = Button(this)
        sendBtn.text = "Gửi"
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(sendBtn, LinearLayout.LayoutParams(-2, -2))
        root.addView(row, LinearLayout.LayoutParams(-1, -2))

        val frame = FrameLayout(this)
        frame.addView(AuraView(this), FrameLayout.LayoutParams(-1, -1))
        frame.addView(root, FrameLayout.LayoutParams(-1, -1))
        setContentView(frame)

        ctrl = ChatController(this, chat, scroll, input, sendBtn) { askKey() }
        sendBtn.setOnClickListener { ctrl.send() }
        ctrl.bubble("Xin chào! Mình build map và tạo vật thể cho Roblox. Mô tả thứ bạn muốn hoặc bấm Ảnh để gửi ảnh tham khảo.", false)

        if (!ctrl.hasKey()) {
            askKey()
        } else if (savedInstanceState == null && !OverlayService.running) {
            startOverlay()
        }
    }
}
