package com.trotabares.elteoremadelmorfi

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class MorfiImageIntroView(context: Context) : View(context) {
    private val bitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.morfi_intro)
        ?: error("No se pudo cargar la ilustración real")

    private val started = System.currentTimeMillis()
    private var entered = false
    private var enteredAt = 0L

    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun onDraw(c: Canvas) {
        val now = System.currentTimeMillis()
        val t = (now - started) / 1000f
        c.drawColor(Color.WHITE)

        if (!entered) {
            drawIntro(c, t)
            if (t < 9.7f) postInvalidateOnAnimation()
            return
        }

        drawMainMenu(c, (now - enteredAt) / 900f)
    }

    private fun drawIntro(c: Canvas, t: Float) {
        val d = fitRect(width.toFloat(), height.toFloat())

        // 0.00–0.90 — sky appears first.
        revealRect(c, d, 0f, 0f, 1f, 0.30f, ease((t - 0.10f) / 0.80f))

        // Keep the original title hidden until its own final reveal.
        if (t < 7.25f) {
            overlay(c, d, 0.02f, 0.015f, 0.98f, 0.34f, Color.WHITE, 255)
        }

        // 0.85–1.70 — sun emerges separately.
        val sun = ease((t - 0.85f) / 0.85f)
        if (sun > 0f) {
            val cx = d.left + d.width() * 0.58f
            val cy = d.top + d.height() * 0.315f
            val r = d.width() * (0.025f + 0.13f * sun)
            c.save()
            path.reset()
            path.addCircle(cx, cy, r, Path.Direction.CW)
            c.clipPath(path)
            c.drawBitmap(bitmap, null, d, imagePaint)
            c.restore()
        }

        // 1.45–2.45 — Río de la Plata appears as a horizontal band.
        revealRect(c, d, 0f, 0.31f, 1f, 0.48f, ease((t - 1.45f) / 1.00f))

        // 2.20–3.25 — central road grows from the horizon toward the viewer.
        val road = ease((t - 2.20f) / 1.05f)
        if (road > 0f) {
            c.save()
            path.reset()
            val topX = d.left + d.width() * 0.515f
            val topY = d.top + d.height() * 0.43f
            val bottomY = d.top + d.height() * (0.58f + 0.42f * road)
            val halfTop = d.width() * 0.018f
            val halfBottom = d.width() * (0.045f + 0.31f * road)
            path.moveTo(topX - halfTop, topY)
            path.lineTo(topX + halfTop, topY)
            path.lineTo(topX + halfBottom, bottomY)
            path.lineTo(topX - halfBottom, bottomY)
            path.close()
            c.clipPath(path)
            c.drawBitmap(bitmap, null, d, imagePaint)
            c.restore()
        }

        // 3.00–6.40 — the places appear one at a time from the real illustration.
        popRegion(c, d, 0.00f, 0.38f, 0.34f, 0.61f, t, 3.00f, 0.75f, -18f, 0f) // Astillero
        popRegion(c, d, 0.00f, 0.53f, 0.31f, 0.72f, t, 3.75f, 0.70f, -12f, 0f) // Pizzería
        popRegion(c, d, 0.63f, 0.43f, 0.93f, 0.61f, t, 4.45f, 0.70f, 14f, 0f) // Clínica
        popRegion(c, d, 0.70f, 0.51f, 1.00f, 0.70f, t, 5.15f, 0.75f, 14f, 0f) // Escuela

        // 5.90–7.20 — Matías enters last, from the foreground.
        val mat = ease((t - 5.90f) / 1.30f)
        if (mat > 0f) {
            val src = RectF(
                d.left + d.width() * 0.08f,
                d.top + d.height() * 0.49f,
                d.left + d.width() * 0.72f,
                d.bottom
            )
            val yShift = d.height() * 0.16f * (1f - mat)
            c.save()
            c.clipRect(src)
            c.translate(0f, yShift)
            c.drawBitmap(bitmap, null, d, imagePaint)
            c.restore()
        }

        // Keep the wooden ENTRAR sign hidden until the very end.
        if (t < 8.15f) {
            overlay(c, d, 0.10f, 0.78f, 0.90f, 1.00f, Color.WHITE, 255)
        }

        // 7.10–8.00 — original title/subtitle appear last, from the real image.
        val title = ease((t - 7.10f) / 0.90f)
        if (title > 0f) {
            revealRect(c, d, 0.00f, 0.00f, 1.00f, 0.38f, title)
        }

        // 8.00–8.80 — original wooden ENTRAR button appears.
        val button = ease((t - 8.00f) / 0.80f)
        if (button > 0f) {
            val src = RectF(
                d.left + d.width() * 0.12f,
                d.top + d.height() * 0.80f,
                d.left + d.width() * 0.88f,
                d.bottom
            )
            c.save()
            val s = 0.96f + 0.04f * button
            val cx = d.centerX()
            val cy = d.bottom - d.height() * 0.08f
            c.translate(cx, cy)
            c.scale(s, s)
            c.translate(-cx, -cy)
            path.reset()
            path.addRoundRect(src, 24f, 24f, Path.Direction.CW)
            c.clipPath(path)
            c.drawBitmap(bitmap, null, d, imagePaint)
            c.restore()
        }
    }

    private fun popRegion(
        c: Canvas,
        d: RectF,
        l: Float, top: Float, r: Float, bottom: Float,
        t: Float, start: Float, duration: Float,
        dx: Float, dy: Float
    ) {
        val a = ease((t - start) / duration)
        if (a <= 0f) return

        val region = RectF(
            d.left + d.width() * l,
            d.top + d.height() * top,
            d.left + d.width() * r,
            d.top + d.height() * bottom
        )
        c.save()
        val scale = 0.94f + 0.06f * a
        c.translate(region.centerX() + dx * (1f - a), region.centerY() + dy * (1f - a))
        c.scale(scale, scale)
        c.translate(-region.centerX(), -region.centerY())
        path.reset()
        path.addRoundRect(region, 18f, 18f, Path.Direction.CW)
        c.clipPath(path)
        c.drawBitmap(bitmap, null, d, imagePaint)
        c.restore()
    }

    private fun revealRect(c: Canvas, d: RectF, l: Float, top: Float, r: Float, bottom: Float, a: Float) {
        if (a <= 0f) return
        val h = d.height() * (bottom - top) * a
        val rect = RectF(
            d.left + d.width() * l,
            d.top + d.height() * top,
            d.left + d.width() * r,
            d.top + d.height() * top + h
        )
        c.save()
        c.clipRect(rect)
        c.drawBitmap(bitmap, null, d, imagePaint)
        c.restore()
    }

    private fun overlay(c: Canvas, d: RectF, l: Float, top: Float, r: Float, bottom: Float, color: Int, alpha: Int) {
        overlayPaint.color = color
        overlayPaint.alpha = alpha
        c.drawRect(
            d.left + d.width() * l,
            d.top + d.height() * top,
            d.left + d.width() * r,
            d.top + d.height() * bottom,
            overlayPaint
        )
        overlayPaint.alpha = 255
    }

    private fun drawMainMenu(c: Canvas, elapsed: Float) {
        val d = fitRect(width.toFloat(), height.toFloat())
        c.drawBitmap(bitmap, null, d, imagePaint)

        val a = ease(elapsed.coerceIn(0f, 1f))
        overlayPaint.color = Color.WHITE
        overlayPaint.alpha = (225 * a).toInt()
        val panel = RectF(width * .06f, height * .68f, width * .94f, height * .91f)
        c.drawRoundRect(panel, 28f, 28f, overlayPaint)

        overlayPaint.color = Color.rgb(55, 43, 32)
        overlayPaint.alpha = (255 * a).toInt()
        overlayPaint.textAlign = Paint.Align.CENTER
        overlayPaint.typeface = Typeface.DEFAULT_BOLD
        overlayPaint.textSize = width * .043f
        c.drawText("EL MUNDO DE MATÍAS", width / 2f, height * .735f, overlayPaint)
        overlayPaint.typeface = Typeface.DEFAULT
        overlayPaint.textSize = width * .027f
        c.drawText("Elegí por dónde empezar", width / 2f, height * .775f, overlayPaint)

        val labels = arrayOf("MORFI", "¡EUREKA!", "DETECTIVES", "LABORATORIO")
        val xs = floatArrayOf(.27f, .27f, .73f, .73f)
        val ys = floatArrayOf(.81f, .855f, .81f, .855f)
        for (i in labels.indices) {
            overlayPaint.color = Color.rgb(82, 111, 66)
            overlayPaint.alpha = (235 * a).toInt()
            c.drawRoundRect(
                width * (xs[i] - .20f), height * (ys[i] - .025f),
                width * (xs[i] + .20f), height * (ys[i] + .018f),
                18f, 18f, overlayPaint
            )
            overlayPaint.color = Color.WHITE
            overlayPaint.textSize = width * .021f
            overlayPaint.typeface = Typeface.DEFAULT_BOLD
            c.drawText(labels[i], width * xs[i], height * (ys[i] + .002f), overlayPaint)
        }
        overlayPaint.alpha = 255
    }

    private fun fitRect(w: Float, h: Float): RectF {
        val s = min(w / bitmap.width, h / bitmap.height)
        val rw = bitmap.width * s
        val rh = bitmap.height * s
        return RectF((w - rw) / 2f, (h - rh) / 2f, (w + rw) / 2f, (h + rh) / 2f)
    }

    private fun ease(x: Float): Float {
        val v = x.coerceIn(0f, 1f)
        return v * v * (3f - 2f * v)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP && !entered) {
            val t = (System.currentTimeMillis() - started) / 1000f
            if (t >= 8.65f) {
                entered = true
                enteredAt = System.currentTimeMillis()
                postInvalidateOnAnimation()
            }
        }
        return true
    }
}
