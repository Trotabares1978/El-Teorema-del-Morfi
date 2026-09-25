package com.trotabares1978.elteoremadelmorfi

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Opening animation for El Teorema del Morfi.
 *
 * The final illustration is kept as the source of truth, but it is NOT simply
 * faded in or uncovered with rectangles. The image is revealed through many
 * irregular brush/pencil strokes, in the same order a person would construct
 * the illustration on a white sheet.
 */
class MorfiImageIntroView(context: Context) : View(context) {

    private val bitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.morfi_intro)
            ?: error("No se pudo cargar la ilustración del Morfi")

    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var maskBitmap: Bitmap? = null
    private var brushBitmap: Bitmap? = null
    private var maskCanvas: Canvas? = null
    private var brushCanvas: Canvas? = null

    private var startedAt = 0L
    private var entered = false
    private var enteredAt = 0L

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.strokeJoin = Paint.Join.ROUND
        outlinePaint.color = Color.rgb(91, 72, 55)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        maskBitmap?.recycle()
        brushBitmap?.recycle()

        val ww = w.coerceAtLeast(1)
        val hh = h.coerceAtLeast(1)

        maskBitmap = Bitmap.createBitmap(ww, hh, Bitmap.Config.ARGB_8888)
        brushBitmap = Bitmap.createBitmap(ww, hh, Bitmap.Config.ARGB_8888)
        maskCanvas = Canvas(maskBitmap!!)
        brushCanvas = Canvas(brushBitmap!!)
    }

    override fun onDraw(c: Canvas) {
        val now = System.currentTimeMillis()
        if (startedAt == 0L) startedAt = now

        c.drawColor(Color.WHITE)

        if (!entered) {
            val t = (now - startedAt) / 1000f
            drawOpening(c, t)

            if (t < 15.8f) {
                postInvalidateOnAnimation()
            }
        } else {
            drawMainMenu(c, ease(((now - enteredAt) / 900f).coerceIn(0f, 1f)))
        }
    }

    private fun drawOpening(c: Canvas, t: Float) {
        val d = fitRect(width.toFloat(), height.toFloat())

        // 1 — White paper -> sky, river and land.
        revealLandscape(c, d, ease((t - 0.00f) / 4.80f))

        // 2 — The sun is sketched separately.
        revealObject(c, d, ease((t - 2.15f) / 1.20f), ::sunPath, diagonal = false)

        // 3 — The road is drawn from the horizon toward the viewer.
        revealRoad(c, d, ease((t - 3.70f) / 2.20f))

        // 4 — Places arrive as little illustrated constructions.
        revealObject(c, d, ease((t - 5.70f) / 1.35f), ::astilleroPath, diagonal = true)
        revealObject(c, d, ease((t - 6.85f) / 1.25f), ::pizzeriaPath, diagonal = false)
        revealObject(c, d, ease((t - 7.90f) / 1.20f), ::clinicPath, diagonal = true)
        revealObject(c, d, ease((t - 8.90f) / 1.35f), ::schoolPath, diagonal = false)

        // 5 — Matías is deliberately the last illustrated element.
        revealObject(c, d, ease((t - 10.15f) / 2.15f), ::matiasPath, diagonal = true)

        // 6 — Identity appears after the whole world exists.
        revealObject(c, d, ease((t - 12.10f) / 1.15f), ::titlePath, diagonal = false)
        revealObject(c, d, ease((t - 13.15f) / 1.00f), ::enterPath, diagonal = true)

        if (t >= 14.45f) {
            // A very short settling period leaves the exact original artwork.
            c.drawBitmap(bitmap, null, d, imagePaint)
        }
    }

    /**
     * Broad but irregular strokes. The stroke edges overlap, so the scene
     * looks painted onto paper instead of being revealed by horizontal bars.
     */
    private fun revealLandscape(c: Canvas, d: RectF, amount: Float) {
        if (amount <= 0f) return
        val base = maskCanvas ?: return
        val brush = brushCanvas ?: return

        clear(base)
        clear(brush)

        maskPaint.color = Color.WHITE
        maskPaint.alpha = 255
        base.drawRect(d, maskPaint)

        // Later elements stay hidden while the landscape is being painted.
        clearPath(base, sunPath(d))
        clearPath(base, roadPath(d))
        clearPath(base, astilleroPath(d))
        clearPath(base, pizzeriaPath(d))
        clearPath(base, clinicPath(d))
        clearPath(base, schoolPath(d))
        clearPath(base, matiasPath(d))
        clearPath(base, titlePath(d))
        clearPath(base, enterPath(d))

        val rows = 30
        val progress = amount.coerceIn(0f, 1f) * rows

        for (i in 0 until rows) {
            val local = (progress - i).coerceIn(0f, 1f)
            if (local <= 0f) continue

            val y = i / (rows - 1f)
            val wobble = sin(i * 1.71f) * d.height() * .006f
            val start = -.10f + sin(i * 2.31f) * .025f
            val end = 1.08f + cos(i * 1.27f) * .025f

            paintStroke(
                brush,
                d,
                start,
                y,
                end,
                local,
                d.height() * (.025f + (i % 4) * .006f),
                wobble
            )
        }

        intersect(base, brush)
        drawMasked(c, d)
    }

    /**
     * The road uses converging strokes, giving the impression that the
     * illustrator is pulling it out of the horizon toward the viewer.
     */
    private fun revealRoad(c: Canvas, d: RectF, amount: Float) {
        if (amount <= 0f) return

        val base = maskCanvas ?: return
        val brush = brushCanvas ?: return

        clear(base)
        clear(brush)

        maskPaint.color = Color.WHITE
        base.drawPath(roadPath(d), maskPaint)

        val a = amount.coerceIn(0f, 1f)
        val strokes = 16

        for (i in 0 until strokes) {
            val local = ((a * strokes) - i).coerceIn(0f, 1f)
            if (local <= 0f) continue

            val u = i / (strokes - 1f)
            val horizonX = .5f + (u - .5f) * .035f
            val bottomX = .5f + (u - .5f) * .70f
            val y0 = .405f + u * .03f
            val y1 = .48f + u * .52f

            val p = Path()
            p.moveTo(d.left + d.width() * horizonX, d.top + d.height() * y0)
            p.cubicTo(
                d.left + d.width() * (horizonX + (bottomX - horizonX) * .28f),
                d.top + d.height() * (y0 + (y1 - y0) * .28f),
                d.left + d.width() * (horizonX + (bottomX - horizonX) * .70f),
                d.top + d.height() * (y0 + (y1 - y0) * .70f),
                d.left + d.width() * (horizonX + (bottomX - horizonX) * local),
                d.top + d.height() * (y0 + (y1 - y0) * local)
            )

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeWidth = d.height() * .035f
                maskFilter = BlurMaskFilter(d.height() * .004f + 1f, BlurMaskFilter.Blur.NORMAL)
            }
            brush.drawPath(p, paint)
        }

        intersect(base, brush)
        drawMasked(c, d)
    }

    /**
     * Objects are built in two passes: a faint hand-drawn contour first,
     * followed by the colored illustration being brushed into existence.
     */
    private fun revealObject(
        c: Canvas,
        d: RectF,
        amount: Float,
        pathFactory: (RectF) -> Path,
        diagonal: Boolean
    ) {
        if (amount <= 0f) return

        val path = pathFactory(d)
        val base = maskCanvas ?: return
        val brush = brushCanvas ?: return

        clear(base)
        clear(brush)

        maskPaint.color = Color.WHITE
        base.drawPath(path, maskPaint)

        val box = RectF()
        path.computeBounds(box, true)

        // A pencil-like contour gives the eye the feeling that the object is
        // actually being drawn, instead of simply appearing.
        drawSketchContour(c, path, box, amount)

        val rows = 12
        val progress = amount.coerceIn(0f, 1f) * rows

        for (i in 0 until rows) {
            val local = (progress - i).coerceIn(0f, 1f)
            if (local <= 0f) continue

            val y = box.top + box.height() * (i / (rows - 1f))
            val drift = sin(i * 2.4f) * box.width() * .025f

            if (!diagonal) {
                paintStroke(
                    brush,
                    RectF(box.left, box.top, box.right, box.bottom),
                    -.08f,
                    (y - box.top) / box.height(),
                    1.08f,
                    local,
                    maxOf(box.height() * .055f, 2.5f),
                    drift
                )
            } else {
                paintDiagonalStroke(brush, box, i, local)
            }
        }

        intersect(base, brush)
        drawMasked(c, d)
    }

    private fun drawSketchContour(c: Canvas, path: Path, box: RectF, amount: Float) {
        if (amount <= 0f) return

        outlinePaint.alpha = (72f * ease(amount)).toInt().coerceIn(0, 72)
        outlinePaint.strokeWidth = maxOf(1.2f, box.width() * .006f)

        // The full contour is deliberately faint; the final colored reveal
        // remains the dominant visual.
        c.drawPath(path, outlinePaint)
        outlinePaint.alpha = 0
    }

    private fun paintDiagonalStroke(canvas: Canvas, box: RectF, index: Int, amount: Float) {
        val p = Path()
        val count = 12f
        val y = box.top + box.height() * (index / (count - 1f))
        val x0 = box.left - box.width() * .20f
        val x1 = box.left + box.width() * 1.20f

        p.moveTo(x0, y - box.height() * .20f)
        p.lineTo(
            x0 + (x1 - x0) * amount,
            y - box.height() * .20f + box.height() * .40f * amount
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = maxOf(box.height() * .08f, 3f)
            maskFilter = BlurMaskFilter(maxOf(box.height() * .008f, 1f), BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(p, paint)
    }

    private fun paintStroke(
        canvas: Canvas,
        d: RectF,
        startX: Float,
        y: Float,
        endX: Float,
        amount: Float,
        strokeWidth: Float,
        wobble: Float
    ) {
        val p = Path()
        val yy = d.top + d.height() * y
        val x0 = d.left + d.width() * startX
        val total = d.width() * (endX - startX)
        val x1 = x0 + total * amount

        p.moveTo(x0, yy)
        p.cubicTo(
            x0 + total * .18f,
            yy - wobble,
            x0 + total * .40f,
            yy + wobble,
            x0 + total * .60f,
            yy
        )
        p.cubicTo(
            x0 + total * .78f,
            yy - wobble,
            x0 + total * .92f,
            yy + wobble,
            x1,
            yy + wobble * .5f
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.strokeWidth = strokeWidth
            maskFilter = BlurMaskFilter(maxOf(strokeWidth * .16f, 1f), BlurMaskFilter.Blur.NORMAL)
        }

        canvas.drawPath(p, paint)
    }

    private fun drawMasked(c: Canvas, d: RectF) {
        val mask = maskBitmap ?: return
        val save = c.saveLayer(d, null)

        c.drawBitmap(bitmap, null, d, imagePaint)

        maskPaint.reset()
        maskPaint.isAntiAlias = true
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        c.drawBitmap(mask, 0f, 0f, maskPaint)
        maskPaint.xfermode = null

        c.restoreToCount(save)
    }

    private fun intersect(base: Canvas, brush: Canvas) {
        maskPaint.reset()
        maskPaint.isAntiAlias = true
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        base.drawBitmap(brushBitmap!!, 0f, 0f, maskPaint)
        maskPaint.xfermode = null
    }

    private fun clear(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

    private fun clearPath(canvas: Canvas, path: Path) {
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        })
    }

    private fun sunPath(d: RectF): Path = ellipsePath(
        d.left + d.width() * .610f,
        d.top + d.height() * .318f,
        d.width() * .061f,
        d.height() * .041f
    )

    private fun roadPath(d: RectF): Path = polygonPath(d, arrayOf(
        .493f to .402f, .507f to .402f,
        .535f to .505f, .585f to .640f, .805f to 1.0f,
        .195f to 1.0f, .415f to .640f, .465f to .505f
    ))

    private fun astilleroPath(d: RectF): Path = polygonPath(d, arrayOf(
        0f to .352f, .035f to .352f, .035f to .336f, .059f to .336f,
        .059f to .297f, .076f to .297f, .076f to .263f, .096f to .263f,
        .100f to .315f, .121f to .315f, .127f to .277f, .146f to .277f,
        .150f to .306f, .172f to .306f, .172f to .339f, .195f to .339f,
        .199f to .358f, .234f to .358f, .234f to .377f, .266f to .377f,
        .266f to .397f, .289f to .397f, .289f to .441f, .270f to .441f,
        .270f to .456f, 0f to .456f
    ))

    private fun pizzeriaPath(d: RectF): Path = polygonPath(d, arrayOf(
        0f to .482f, .035f to .482f, .035f to .473f, .217f to .473f,
        .217f to .487f, .262f to .487f, .262f to .510f, .293f to .510f,
        .293f to .617f, .271f to .617f, .271f to .651f, 0f to .651f
    ))

    private fun clinicPath(d: RectF): Path = polygonPath(d, arrayOf(
        .627f to .457f, .646f to .453f, .697f to .453f, .697f to .459f,
        .732f to .459f, .732f to .497f, .711f to .497f, .711f to .517f,
        .625f to .517f
    ))

    private fun schoolPath(d: RectF): Path = polygonPath(d, arrayOf(
        .766f to .492f, 1f to .492f, 1f to .647f, .758f to .647f,
        .758f to .594f, .746f to .594f, .746f to .547f, .766f to .547f
    ))

    private fun matiasPath(d: RectF): Path = polygonPath(d, arrayOf(
        .145f to .866f, .133f to .846f, .141f to .815f, .152f to .797f,
        .168f to .773f, .188f to .753f, .205f to .732f, .230f to .710f,
        .252f to .684f, .277f to .658f, .295f to .630f, .303f to .607f,
        .313f to .583f, .324f to .559f, .344f to .540f, .365f to .521f,
        .395f to .502f, .424f to .483f, .455f to .463f, .486f to .448f,
        .518f to .438f, .549f to .448f, .574f to .466f, .600f to .491f,
        .619f to .521f, .638f to .549f, .656f to .579f, .660f to .611f,
        .650f to .635f, .633f to .658f, .617f to .680f, 0.604f to .704f,
        .588f to .733f, .573f to .761f, .559f to .792f, .543f to .825f,
        .528f to .861f, .518f to .892f, .508f to .929f, .488f to 1.0f,
        .229f to 1.0f, .217f to .974f, .199f to .949f, .187f to .930f,
        .170f to .911f, .153f to .888f
    ))

    private fun titlePath(d: RectF): Path = polygonPath(d, arrayOf(
        .082f to .027f, .896f to .027f, .930f to .242f,
        .812f to .275f, .184f to .274f, .078f to .230f
    ))

    private fun enterPath(d: RectF): Path = roundedRectPath(
        RectF(
            d.left + d.width() * .201f,
            d.top + d.height() * .828f,
            d.left + d.width() * .801f,
            d.top + d.height() * .971f
        ),
        d.width() * .025f
    )

    private fun polygonPath(d: RectF, points: Array<Pair<Float, Float>>): Path {
        val p = Path()
        points.forEachIndexed { i, q ->
            val x = d.left + d.width() * q.first
            val y = d.top + d.height() * q.second
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }

    private fun ellipsePath(cx: Float, cy: Float, rx: Float, ry: Float): Path {
        val p = Path()
        p.addOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), Path.Direction.CW)
        return p
    }

    private fun roundedRectPath(r: RectF, radius: Float): Path {
        val p = Path()
        p.addRoundRect(r, radius, radius, Path.Direction.CW)
        return p
    }

    private fun fitRect(w: Float, h: Float): RectF {
        val s = min(w / bitmap.width, h / bitmap.height)
        val rw = bitmap.width * s
        val rh = bitmap.height * s
        return RectF((w - rw) / 2f, (h - rh) / 2f, (w + rw) / 2f, (h + rh) / 2f)
    }

    private fun drawMainMenu(c: Canvas, a: Float) {
        val d = fitRect(width.toFloat(), height.toFloat())
        c.drawBitmap(bitmap, null, d, imagePaint)

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
                width * (xs[i] - .20f),
                height * (ys[i] - .025f),
                width * (xs[i] + .20f),
                height * (ys[i] + .018f),
                18f,
                18f,
                textPaint
            )
            textPaint.color = Color.WHITE
            textPaint.textSize = width * .021f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            c.drawText(labels[i], width * xs[i], height * (ys[i] + .002f), textPaint)
        }

        textPaint.alpha = 255
    }

    private fun ease(x: Float): Float {
        val v = x.coerceIn(0f, 1f)
        return v * v * (3f - 2f * v)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP && !entered) {
            val t = (System.currentTimeMillis() - startedAt) / 1000f
            if (t >= 12.9f) {
                entered = true
                enteredAt = System.currentTimeMillis()
                postInvalidateOnAnimation()
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        maskBitmap?.recycle()
        brushBitmap?.recycle()
        maskBitmap = null
        brushBitmap = null
        maskCanvas = null
        brushCanvas = null
        super.onDetachedFromWindow()
    }
}
