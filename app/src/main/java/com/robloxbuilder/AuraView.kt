package com.robloxbuilder

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class AuraView(c: Context) : View(c) {
    private val hexes = arrayOf("#7C3AED", "#2563EB", "#EC4899", "#06B6D4")
    private val paints = Array(hexes.size) { Paint(Paint.ANTI_ALIAS_FLAG) }
    private var radius = 0f
    private var t = 0f
    private val animator = ValueAnimator.ofFloat(0f, (2.0 * Math.PI).toFloat())

    init {
        animator.duration = 16000
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener {
            t = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        radius = max(w, h) * 0.55f
        for (i in hexes.indices) {
            val base = Color.parseColor(hexes[i])
            val center = Color.argb(120, Color.red(base), Color.green(base), Color.blue(base))
            val edge = Color.argb(0, Color.red(base), Color.green(base), Color.blue(base))
            paints[i].shader = RadialGradient(0f, 0f, radius, center, edge, Shader.TileMode.CLAMP)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawColor(Color.parseColor("#F4F5F7"))
        for (i in hexes.indices) {
            val cx = w * (0.5f + 0.4f * sin(t + i * 1.7f))
            val cy = h * (0.5f + 0.4f * cos(t + i * 2.3f))
            canvas.save()
            canvas.translate(cx, cy)
            canvas.drawCircle(0f, 0f, radius, paints[i])
            canvas.restore()
        }
    }
}
