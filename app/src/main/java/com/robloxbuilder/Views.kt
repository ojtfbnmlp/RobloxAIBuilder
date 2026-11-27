package com.robloxbuilder

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

object Views {
    fun dp(c: Context, v: Int): Int =
        (v * c.resources.displayMetrics.density).toInt()

    fun rounded(c: Context, color: String, r: Int): GradientDrawable {
        val bg = GradientDrawable()
        bg.cornerRadius = dp(c, r).toFloat()
        bg.setColor(Color.parseColor(color))
        return bg
    }

    fun bubble(c: Context, t: String, mine: Boolean): TextView {
        val tv = TextView(c)
        tv.text = t
        tv.textSize = 15f
        tv.setTextIsSelectable(true)
        tv.setTextColor(Color.parseColor(if (mine) "#FFFFFF" else "#111827"))
        tv.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8))
        tv.background = rounded(c, if (mine) "#2563EB" else "#FFFFFF", 12)
        val lp = LinearLayout.LayoutParams(-2, -2)
        lp.topMargin = dp(c, 6)
        lp.gravity = if (mine) Gravity.END else Gravity.START
        tv.layoutParams = lp
        return tv
    }

    fun codeBox(c: Context, code: String): LinearLayout {
        val box = LinearLayout(c)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(c, 8), dp(c, 8), dp(c, 8), dp(c, 8))
        box.background = rounded(c, "#111827", 10)
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.topMargin = dp(c, 6)
        box.layoutParams = lp

        val info = TextView(c)
        info.text = (code.count { it == '\n' } + 1).toString() + " dòng"
        info.textSize = 11f
        info.setTextColor(Color.parseColor("#9CA3AF"))
        box.addView(info)

        val body = TextView(c)
        body.text = code
        body.textSize = 12f
        body.typeface = Typeface.MONOSPACE
        body.setTextColor(Color.parseColor("#E5E7EB"))
        body.setTextIsSelectable(true)
        val hs = HorizontalScrollView(c)
        hs.addView(body)
        box.addView(hs)

        val btn = Button(c)
        btn.text = "Copy"
        btn.isAllCaps = false
        btn.setOnClickListener {
            val cm = c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("lua", code))
            Toast.makeText(c, "Đã copy", Toast.LENGTH_SHORT).show()
        }
        box.addView(btn)
        return box
    }
}
