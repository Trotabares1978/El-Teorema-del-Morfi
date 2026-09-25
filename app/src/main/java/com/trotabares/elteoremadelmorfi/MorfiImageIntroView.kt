package com.trotabares.elteoremadelmorfi

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class MorfiImageIntroView(context: Context) : View(context) {
    private val bitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.morfi_intro)
        ?: error("No se pudo cargar la ilustración real")

    private val started = System.currentTimeMillis()
    private var entered = false
    private var enteredAt = 0L

    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        // Needed for soft, feathered organic masks.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(c: Canvas) {
        val now = System.currentTimeMillis()
        val t = (now - started) / 1000f
        c.drawColor(Color.WHITE)

        if (!entered) {
            drawIntro(c, t)
            if (t < 10.5f) postInvalidateOnAnimation()
        } else {
            drawMainMenu(c, (now - enteredAt) / 900f)
        }
    }

    private fun drawIntro(c: Canvas, t: Float) {
        val d = fitRect(width.toFloat(), height.toFloat())

        // 0.0–1.2: cielo, but with a loose hand-drawn lower edge.
        organicBand(c, d, 0.00f, 0.00f, 1.00f, 0.19f, ease((t - 0.10f) / 1.10f), 0.0f)

        // 0.8–2.0: the sun blooms independently.
        organicBlob(c, d, 0.47f, 0.27f, 0.23f, 0.17f, ease((t - 0.80f) / 1.20f), 0f, 0f)

        // 1.5–2.8: river opens as a soft, uneven horizontal sweep.
        organicBand(c, d, 0.00f, 0.33f, 1.00f, 0.48f, ease((t - 1.50f) / 1.30f), 0.018f)

        // 2.2–3.8: road grows out of the horizon, with curved rather than straight sides.
        organicRoad(c, d, ease((t - 2.20f) / 1.60f))

        // 3.1–6.8: each place arrives as its own soft, irregular reveal.
        organicBlob(c, d, 0.13f, 0.49f, 0.34f, 0.25f, ease((t - 3.10f) / 0.95f), -0.03f, 0.01f) // Astillero
        organicBlob(c, d, 0.15f, 0.62f, 0.30f, 0.19f, ease((t - 3.90f) / 0.90f), -0.02f, 0.02f) // Pizzería
        organicBlob(c, d, 0.80f, 0.52f, 0.28f, 0.22f, ease((t - 4.70f) / 0.90f), 0.03f, 0.01f) // Clínica
        organicBlob(c, d, 0.82f, 0.62f, 0.30f, 0.23f, ease((t - 5.50f) / 0.95f), 0.02f, 0.02f) // Escuela

        // 6.0–7.8: Matías appears last, from the foreground, through a loose silhouette-like blob.
        organicBlob(c, d, 0.43f, 0.79f, 0.64f, 0.45f, ease((t - 6.00f) / 1.80f), 0f, 0.10f)

        // 7.3–8.7: title and subtitle arrive as a soft ink-like reveal.
        organicBlob(c, d, 0.50f, 0.14f, 0.94f, 0.29f, ease((t - 7.30f) / 1.40f), 0f, -0.015f)

        // 8.4–9.6: wooden ENTRAR sign appears last, with a small organic settle.
        organicBlob(c, d, 0.50f, 0.90f, 0.78f, 0.18f, ease((t - 8.40f) / 1.20f), 0f, 0.03f)

        // Once the sequence is complete, remove every temporary reveal boundary:
        // the exact original illustration becomes the stable screen.
        if (t >= 9.65f) {
            c.drawBitmap(bitmap, null, d, imagePaint)
        }
    }

    private fun organicBlob(
        c: Canvas,
        d: RectF,
        cx: Float,
        cy: Float,
        rw: Float,
        rh: Float,
        amount: Float,
        dx: Float,
        dy: Float
    ) {
        if (amount <= 0f) return

        val a = amount.coerceIn(0f, 1f)
        val x = d.left + d.width() * (cx + dx * (1f - a))
        val y = d.top + d.height() * (cy + dy * (1f - a))
        val w = d.width() * rw * (0.22f + 0.78f * a)
        val h = d.height() * rh * (0.22f + 0.78f * a)

        val p = Path()
        organicEllipsePath(p, x, y, w, h, 1.0f)

        drawMasked(c, d, p, 10f)
    }

    private fun organicBand(
        c: Canvas,
        d: RectF,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        amount: Float,
        wobble: Float
    ) {
        if (amount <= 0f) return
        val a = amount.coerceIn(0f, 1f)
        val y0 = d.top + d.height() * top
        val y1 = y0 + d.height() * (bottom - top) * a

        val p = Path()
        val l = d.left + d.width() * left
        val r = d.left + d.width() * right
        val w = d.width()
        p.moveTo(l, y0)
        p.cubicTo(
            l + w * .18f, y0 - w * wobble,
            l + w * .34f, y0 + w * wobble,
            l + w * .50f, y0
        )
        p.cubicTo(
            l + w * .67f, y0 - w * wobble,
            l + w * .84f, y0 + w * wobble,
            r, y0
        )
        p.lineTo(r, y1)
        p.cubicTo(
            l + w * .83f, y1 + w * wobble,
            l + w * .67f, y1 - w * wobble,
            l + w * .50f, y1
        )
        p.cubicTo(
            l + w * .32f, y1 + w * wobble,
            l + w * .16f, y1 - w * wobble,
            l, y1
        )
        p.close()
        drawMasked(c, d, p, 12f)
    }

    private fun organicRoad(c: Canvas, d: RectF, amount: Float) {
        if (amount <= 0f) return
        val a = amount.coerceIn(0f, 1f)
        val horizonY = d.top + d.height() * .47f
        val bottomY = d.top + d.height() * (.49f + .36f * a)
        val cx = d.left + d.width() * .515f

        val p = Path()
        p.moveTo(cx - d.width() * .012f, horizonY)
        p.cubicTo(
            cx - d.width() * .018f, horizonY + d.height() * .07f,
            cx - d.width() * (.04f + .08f * a), bottomY - d.height() * .05f,
            cx - d.width() * (.07f + .22f * a), bottomY
        )
        p.cubicTo(
            cx - d.width() * .13f, bottomY + d.height() * .015f,
            cx - d.width() * .02f, bottomY + d.height() * .025f,
            cx, bottomY + d.height() * .01f
        )
        p.cubicTo(
            cx + d.width() * .04f, bottomY + d.height() * .025f,
            cx + d.width() * (.13f + .10f * a), bottomY + d.height() * .01f,
            cx + d.width() * (.07f + .22f * a), bottomY
        )
        p.cubicTo(
            cx + d.width() * (.04f + .08f * a), bottomY - d.height() * .05f,
            cx + d.width() * .018f, horizonY + d.height() * .07f,
            cx + d.width() * .012f, horizonY
        )
        p.close()

        drawMasked(c, d, p, 9f)
    }

    private fun organicEllipsePath(p: Path, cx: Float, cy: Float, w: Float, h: Float, seed: Float) {
        val rx = w / 2f
        val ry = h / 2f
        // Deliberately asymmetric cubic curves: no rectangle/circle edge.
        p.moveTo(cx - rx * .86f, cy - ry * .10f)
        p.cubicTo(cx - rx * .95f, cy - ry * .62f, cx - rx * .48f, cy - ry * 1.02f, cx - rx * .05f, cy - ry * .91f)
        p.cubicTo(cx + rx * .40f, cy - ry * 1.05f, cx + rx * .92f, cy - ry * .62f, cx + rx * .82f, cy - ry * .12f)
        p.cubicTo(cx + rx * 1.00f, cy + ry * .35f, cx + rx * .48f, cy + ry * .96f, cx + rx * .02f, cy + ry * .82f)
        p.cubicTo(cx - rx * .42f, cy + ry * 1.04f, cx - rx * 1.00f, cy + ry * .58f, cx - rx * .86f, cy - ry * .10f)
        p.close()
    }

    private fun drawMasked(c: Canvas, d: RectF, shape: Path, blur: Float) {
        val layer = c.saveLayer(d, null)

        // Source image.
        c.drawBitmap(bitmap, null, d, imagePaint)

        // Soft mask: the edge is feathered instead of a hard geometric cut.
        maskPaint.reset()
        maskPaint.isAntiAlias = true
        maskPaint.color = Color.WHITE
        maskPaint.style = Paint.Style.FILL
        maskPaint.maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        c.drawPath(shape, maskPaint)
        maskPaint.xfermode = null
        maskPaint.maskFilter = null

        c.restoreToCount(layer)
    }

    private fun drawMainMenu(c: Canvas, elapsed: Float) {
        val d = fitRect(width.toFloat(), height.toFloat())
        c.drawBitmap(bitmap, null, d, imagePaint)

        val a = ease(elapsed.coerceIn(0f, 1f))
        textPaint.color = Color.rgb(55, 43, 32)
        textPaint.alpha = (255 * a).toInt()
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = width * .043f
        c.drawText("EL MUNDO DE MATÍAS", width / 2f, height * .735f, textPaint)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = width * .027f
        c.drawText("Elegí por dónde empezar", width / 2f, height * .775f, textPaint)

        val labels = arrayOf("MORFI", "¡EUREKA!", "DETECTIVES", "LABORATORIO")
        val xs = floatArrayOf(.27f, .27f, .73f, .73f)
        val ys = floatArrayOf(.81f, .855f, .81f, .855f)
        for (i in labels.indices) {
            textPaint.color = Color.rgb(82, 111, 66)
            textPaint.alpha = (235 * a).toInt()
            c.drawRoundRect(
                width * (xs[i] - .20f), height * (ys[i] - .025f),
                width * (xs[i] + .20f), height * (ys[i] + .018f),
                18f, 18f, textPaint
            )
            textPaint.color = Color.WHITE
            textPaint.textSize = width * .021f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            c.drawText(labels[i], width * xs[i], height * (ys[i] + .002f), textPaint)
        }
        textPaint.alpha = 255
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
            if (t >= 9.2f) {
                entered = true
                enteredAt = System.currentTimeMillis()
                postInvalidateOnAnimation()
            }
        }
        return true
    }
}
