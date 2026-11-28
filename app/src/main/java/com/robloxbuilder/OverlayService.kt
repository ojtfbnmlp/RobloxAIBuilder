package com.robloxbuilder

import android.animation.ValueAnimator
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.IBinder
import android.provider.Settings
import android.text.InputType
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.abs

class OverlayService : Service() {
    companion object {
        var running = false
    }

    private lateinit var wm: WindowManager
    private lateinit var ui: Context
    private lateinit var bubble: View
    private lateinit var panel: View
    private lateinit var bubbleLp: WindowManager.LayoutParams
    private lateinit var panelLp: WindowManager.LayoutParams
    private lateinit var ctrl: ChatController
    private var bubbleShown = false
    private var panelShown = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running) {
            if (!Settings.canDrawOverlays(this)) {
                stopSelf()
                return START_NOT_STICKY
            }
            running = true
            setup()
        }
        return START_STICKY
    }

    private fun setup() {
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        ui = ContextThemeWrapper(this, android.R.style.Theme_Material_Light)
        val dm = resources.displayMetrics
        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val size = Views.dp(ui, 80)
        bubbleLp = WindowManager.LayoutParams(
            size, size, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        bubbleLp.gravity = Gravity.TOP or Gravity.START
        bubbleLp.x = Views.dp(ui, 4)
        bubbleLp.y = Views.dp(ui, 200)

        val pw = (dm.widthPixels * 0.96f).toInt()
        val ph = (dm.heightPixels * 0.55f).toInt()
        panelLp = WindowManager.LayoutParams(
            pw, ph, type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        panelLp.gravity = Gravity.TOP or Gravity.START
        panelLp.x = (dm.widthPixels - pw) / 2
        panelLp.y = Views.dp(ui, 40)

        bubble = buildBubble()
        panel = buildPanel()
        showPanel()
    }

    private fun buildBubble(): View {
        val glow = GlowFrame(ui, 30, 10, true)
        val b = TextView(ui)
        b.text = "AI"
        b.textSize = 18f
        b.setTextColor(Color.WHITE)
        b.setTypeface(null, Typeface.BOLD)
        b.gravity = Gravity.CENTER
        b.background = Views.rounded(ui, "#4C1D95", 30)
        glow.addView(b, FrameLayout.LayoutParams(-1, -1))
        makeDraggable(glow, bubbleLp, glow, true) { showPanel() }
        return glow
    }

    private fun smallBtn(t: String, action: () -> Unit): Button {
        val b = Button(ui)
        b.text = t
        b.textSize = 12f
        b.isAllCaps = false
        b.minWidth = 0
        b.minimumWidth = 0
        b.setOnClickListener { action() }
        return b
    }

    private fun buildPanel(): View {
        val root = GlowFrame(ui, 16, 10, false)
        val p = LinearLayout(ui)
        p.orientation = LinearLayout.VERTICAL
        p.background = Views.rounded(ui, "#F4F5F7", 16)

        val header = LinearLayout(ui)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.background = Views.rounded(ui, "#1F2937", 16)
        val pad = Views.dp(ui, 8)
        header.setPadding(pad, pad / 2, pad / 2, pad / 2)
        val title = TextView(ui)
        title.text = "Roblox AI"
        title.textSize = 15f
        title.setTextColor(Color.WHITE)
        title.setTypeface(null, Typeface.BOLD)
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(smallBtn("🛠") { ctrl.startFix() })
        header.addView(smallBtn("📷") { ctrl.pickImage() })
        header.addView(smallBtn("Xoá") { ctrl.reset() })
        header.addView(smallBtn("—") { showBubble() })
        header.addView(smallBtn("✕") { stopSelf() })
        p.addView(header, LinearLayout.LayoutParams(-1, -2))
        makeDraggable(header, panelLp, root, false, null)

        val scroll = ScrollView(ui)
        val chat = LinearLayout(ui)
        chat.orientation = LinearLayout.VERTICAL
        val q = Views.dp(ui, 8)
        chat.setPadding(q, q, q, q)
        scroll.addView(chat)
        p.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val row = LinearLayout(ui)
        row.orientation = LinearLayout.HORIZONTAL
        row.setBackgroundColor(Color.WHITE)
        row.setPadding(q, q, q, q)
        val input = EditText(ui)
        input.hint = "Mô tả map hoặc vật thể..."
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        input.maxLines = 3
        val sendBtn = Button(ui)
        sendBtn.text = "Gửi"
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(sendBtn, LinearLayout.LayoutParams(-2, -2))
        p.addView(row, LinearLayout.LayoutParams(-1, -2))

        root.addView(p, FrameLayout.LayoutParams(-1, -1))

        ctrl = ChatController(ui, chat, scroll, input, sendBtn) {
            ctrl.bubble("Chưa có API key. Hãy mở app Roblox AI Builder để nhập key.", false)
        }
        sendBtn.setOnClickListener { ctrl.send() }
        ctrl.bubble("Xin chào! Mình build map và tạo vật thể cho Roblox. Mô tả thứ bạn muốn, bấm nút ảnh để gửi ảnh tham khảo, hoặc bấm nút 🛠 để sửa lỗi script.", false)
        return root
    }

    private fun popIn(v: View) {
        v.scaleX = 0.6f
        v.scaleY = 0.6f
        v.alpha = 0f
        v.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun snapToEdge(lp: WindowManager.LayoutParams, target: View) {
        val dm = resources.displayMetrics
        val margin = Views.dp(ui, 4)
        val w = lp.width
        val goal = if (lp.x + w / 2 < dm.widthPixels / 2) margin
        else dm.widthPixels - w - margin
        val top = Views.dp(ui, 24)
        val bottom = dm.heightPixels - lp.height - Views.dp(ui, 48)
        lp.y = lp.y.coerceIn(top, bottom)
        val anim = ValueAnimator.ofInt(lp.x, goal)
        anim.duration = 260
        anim.interpolator = DecelerateInterpolator()
        anim.addUpdateListener {
            lp.x = it.animatedValue as Int
            try {
                wm.updateViewLayout(target, lp)
            } catch (e: Exception) {
                anim.cancel()
            }
        }
        anim.start()
    }

    private fun makeDraggable(
        handle: View,
        lp: WindowManager.LayoutParams,
        target: View,
        snap: Boolean,
        onTap: (() -> Unit)?
    ) {
        val thr = Views.dp(ui, 6).toFloat()
        var sx = 0f
        var sy = 0f
        var ox = 0
        var oy = 0
        var moved = false
        handle.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = e.rawX
                    sy = e.rawY
                    ox = lp.x
                    oy = lp.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - sx
                    val dy = e.rawY - sy
                    if (abs(dx) > thr || abs(dy) > thr) moved = true
                    if (moved) {
                        lp.x = ox + dx.toInt()
                        lp.y = oy + dy.toInt()
                        wm.updateViewLayout(target, lp)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        onTap?.invoke()
                    } else if (snap) {
                        snapToEdge(lp, target)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun showPanel() {
        if (bubbleShown) {
            wm.removeView(bubble)
            bubbleShown = false
        }
        if (!panelShown) {
            wm.addView(panel, panelLp)
            panelShown = true
            popIn(panel)
        }
    }

    private fun showBubble() {
        if (panelShown) {
            wm.removeView(panel)
            panelShown = false
        }
        if (!bubbleShown) {
            wm.addView(bubble, bubbleLp)
            bubbleShown = true
            snapToEdge(bubbleLp, bubble)
            popIn(bubble)
        }
    }

    override fun onDestroy() {
        if (::wm.isInitialized) {
            if (bubbleShown) wm.removeView(bubble)
            if (panelShown) wm.removeView(panel)
        }
        running = false
        super.onDestroy()
    }
}
