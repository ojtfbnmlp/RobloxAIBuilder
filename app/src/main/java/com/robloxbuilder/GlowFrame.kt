package com.robloxbuilder

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import kotlin.math.sin

class GlowFrame(c: Context, private val radiusDp: Int, padDp: Int) : FrameLayout(c) {
    private val colors = intArrayOf(
        Color.parseColor("#A855F7"),
        Color.parseColor("#3B82F6"),
        Color.parseColor("#22D3EE"),
        Color.parseColor("#EC4899"),
        Color.parseColor("#A855F7")
    )
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rot = Matrix()
    private val rect = RectF()
    private val pad = Views.dp(c, padDp)
    private val anim = ValueAnimator.ofFloat(0f, 360f)
    private var shader: SweepGradient? = null
    private var angle = 0f

    init {
        setWillNotDraw(false)
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        setPadding(pad, pad, pad, pad)
        glow.style = Paint.Style.STROKE
        glow.strokeWidth = pad * 0.7f
        glow.maskFilter = BlurMaskFilter(pad * 0.9f, BlurMaskFilter.Blur.NORMAL)
        line.style = Paint.Style.STROKE
        line.strokeWidth = Views.dp(c, 2).toFloat()
        anim.duration = 3500
        anim.repeatCount = ValueAnimator.INFINITE
        anim.interpolator = LinearInterpolator()
        anim.addUpdateListener {
            angle = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        anim.start()
    }

    override fun onDetachedFromWindow() {
        anim.cancel()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val s = SweepGradient(w / 2f, h / 2f, colors, null)
        shader = s
        glow.shader = s
        line.shader = s
        rect.set(pad.toFloat(), pad.toFloat(), (w - pad).toFloat(), (h - pad).toFloat())
    }

    override fun dispatchDraw(canvas: Canvas) {
        val s = shader
        if (s != null) {
            rot.setRotate(angle, width / 2f, height / 2f)
            s.setLocalMatrix(rot)
            val pulse = sin(Math.toRadians(angle.toDouble() * 2.0))
            glow.alpha = (150 + 90 * pulse).toInt()
            val r = Views.dp(context, radiusDp).toFloat()
            canvas.drawRoundRect(rect, r, r, glow)
            canvas.drawRoundRect(rect, r, r, line)
        }
        super.dispatchDraw(canvas)
    }
}
