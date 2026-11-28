package com.robloxbuilder

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.SweepGradient
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

class GlowFrame(
    c: Context,
    private val radiusDp: Int,
    padDp: Int,
    private val ripple: Boolean = false
) : FrameLayout(c) {
    private val colorsA = intArrayOf(
        Color.parseColor("#A855F7"),
        Color.parseColor("#3B82F6"),
        Color.parseColor("#22D3EE"),
        Color.parseColor("#EC4899"),
        Color.parseColor("#A855F7")
    )
    private val colorsB = intArrayOf(
        Color.parseColor("#22D3EE"),
        Color.parseColor("#EC4899"),
        Color.parseColor("#A855F7"),
        Color.parseColor("#3B82F6"),
        Color.parseColor("#22D3EE")
    )
    private val sparkColors = intArrayOf(
        Color.parseColor("#22D3EE"),
        Color.parseColor("#EC4899"),
        Color.parseColor("#A855F7")
    )
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lineA = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lineB = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spark = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sparkGlow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rotA = Matrix()
    private val rotB = Matrix()
    private val rect = RectF()
    private val path = Path()
    private val measure = PathMeasure()
    private val pos = FloatArray(2)
    private val pad = Views.dp(c, padDp)
    private val sparkR = Views.dp(c, 3).toFloat()
    private val anim = ValueAnimator.ofFloat(0f, 1f)
    private var shaderA: SweepGradient? = null
    private var shaderB: SweepGradient? = null
    private var f = 0f

    init {
        setWillNotDraw(false)
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        setPadding(pad, pad, pad, pad)
        glow.style = Paint.Style.STROKE
        glow.maskFilter = BlurMaskFilter(pad * 0.9f, BlurMaskFilter.Blur.NORMAL)
        lineA.style = Paint.Style.STROKE
        lineA.strokeWidth = Views.dp(c, 2).toFloat()
        lineB.style = Paint.Style.STROKE
        lineB.strokeWidth = Views.dp(c, 1).toFloat()
        lineB.alpha = 160
        spark.style = Paint.Style.FILL
        sparkGlow.style = Paint.Style.FILL
        sparkGlow.maskFilter = BlurMaskFilter(sparkR * 2f, BlurMaskFilter.Blur.NORMAL)
        ring.style = Paint.Style.STROKE
        ring.strokeWidth = Views.dp(c, 2).toFloat()
        anim.duration = 6000
        anim.repeatCount = ValueAnimator.INFINITE
        anim.interpolator = LinearInterpolator()
        anim.addUpdateListener {
            f = it.animatedValue as Float
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
        val a = SweepGradient(w / 2f, h / 2f, colorsA, null)
        val b = SweepGradient(w / 2f, h / 2f, colorsB, null)
        shaderA = a
        shaderB = b
        glow.shader = a
        lineA.shader = a
        lineB.shader = b
        rect.set(pad.toFloat(), pad.toFloat(), (w - pad).toFloat(), (h - pad).toFloat())
        val r = Views.dp(context, radiusDp).toFloat()
        path.reset()
        path.addRoundRect(rect, r, r, Path.Direction.CW)
        measure.setPath(path, false)
    }

    override fun dispatchDraw(canvas: Canvas) {
        val a = shaderA
        val b = shaderB
        if (a != null && b != null) {
            val cx = width / 2f
            val cy = height / 2f
            rotA.setRotate(f * 360f, cx, cy)
            a.setLocalMatrix(rotA)
            rotB.setRotate(-f * 720f, cx, cy)
            b.setLocalMatrix(rotB)
            val breath = ((sin(f * 2.0 * PI * 3.0) + 1.0) / 2.0).toFloat()
            glow.alpha = (110 + 120 * breath).toInt()
            glow.strokeWidth = pad * (0.55f + 0.35f * breath)
            val r = Views.dp(context, radiusDp).toFloat()
            canvas.drawRoundRect(rect, r, r, glow)
            canvas.drawRoundRect(rect, r, r, lineA)
            canvas.drawRoundRect(rect, r, r, lineB)
            drawSparks(canvas)
            if (ripple) drawRipple(canvas)
        }
        super.dispatchDraw(canvas)
    }

    private fun drawSparks(canvas: Canvas) {
        val len = measure.length
        if (len <= 0f) return
        for (i in 0 until 3) {
            val dir = if (i % 2 == 0) 1 else -1
            val k = if (i == 1) 2 else 1
            for (t in 0 until 6) {
                val raw = f * k * dir + i / 3f - t * 0.012f * dir
                val frac = ((raw % 1f) + 1f) % 1f
                measure.getPosTan(frac * len, pos, null)
                val fade = 1f - t / 6f
                spark.color = Color.WHITE
                spark.alpha = (255 * fade).toInt()
                canvas.drawCircle(pos[0], pos[1], sparkR * fade, spark)
                if (t == 0) {
                    sparkGlow.color = sparkColors[i]
                    sparkGlow.alpha = 200
                    canvas.drawCircle(pos[0], pos[1], sparkR * 2.6f, sparkGlow)
                }
            }
        }
    }

    private fun drawRipple(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val base = min(rect.width(), rect.height()) / 2f
        for (j in 0 until 2) {
            val p = (f * 2f + j * 0.5f) % 1f
            ring.color = Color.parseColor("#C4B5FD")
            ring.alpha = (150 * (1f - p)).toInt()
            canvas.drawCircle(cx, cy, base + pad * 0.95f * p, ring)
        }
    }
}
