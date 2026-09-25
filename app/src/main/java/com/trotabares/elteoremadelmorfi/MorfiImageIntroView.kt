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
        val master = maskCanvas ?: return

        // Rebuild one cumulative drawing mask every frame. This is important:
        // nothing is "revealed" as a separate sticker; everything already drawn
        // remains on the paper while the next strokes are added to it.
        clear(master)

        addLandscape(master, d, ease((t - 0.00f) / 5.30f), t)
        addRoad(master, d, ease((t - 3.60f) / 2.70f))

        addObject(master, c, d, ease((t - 5.55f) / 1.55f), ::astilleroPath, diagonal = true)
        addObject(master, c, d, ease((t - 6.70f) / 1.45f), ::pizzeriaPath, diagonal = false)
        addObject(master, c, d, ease((t - 7.75f) / 1.40f), ::clinicPath, diagonal = true)
        addObject(master, c, d, ease((t - 8.75f) / 1.55f), ::schoolPath, diagonal = false)
        addObject(master, c, d, ease((t - 9.85f) / 2.45f), ::matiasPath, diagonal = true)
        addObject(master, c, d, ease((t - 12.15f) / 1.30f), ::titlePath, diagonal = false)
        addObject(master, c, d, ease((t - 13.20f) / 1.10f), ::enterPath, diagonal = true)

        if (t >= 14.55f) c.drawBitmap(bitmap, null, d, imagePaint)

        if (t < 15.8f) postInvalidateOnAnimation()
    }

    /** Adds the broad background strokes to the cumulative mask. */
    private fun addLandscape(master: Canvas, d: RectF, amount: Float, t: Float) {
        if (amount <= 0f) return
        val brush = brushCanvas ?: return
        clear(brush)

        val future = arrayOf(
            sunPath(d), roadPath(d), astilleroPath(d), pizzeriaPath(d),
            clinicPath(d), schoolPath(d), matiasPath(d), titlePath(d), enterPath(d)
        )

        val rows = 38
        val progress = amount.coerceIn(0f, 1f) * rows
        for (i in 0 until rows) {
            val local = (progress - i).coerceIn(0f, 1f)
            if (local <= 0f) continue
            val y = i / (rows - 1f)
            val wobble = sin(i * 1.71f + t * .55f) * d.height() * .0045f
            val widthJitter = .018f + ((i * 17) % 5) * .004f
            paintStroke(brush, d, -.08f, y, 1.08f, local,
                d.height() * widthJitter, wobble)
        }

        // Remove future foreground zones from the landscape brush so they
        // remain white until their own construction begins.
        maskPaint.reset()
        maskPaint.isAntiAlias = true
        maskPaint.color = Color.WHITE
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        for (p in future) brush.drawPath(p, maskPaint)
        maskPaint.xfermode = null

        // The sun gets a soft, incomplete pencil/paint pass of its own.
        val sunAmount = ease((t - 2.0f) / 1.35f)
        if (sunAmount > 0f) drawSoftObjectBrush(brush, d, sunPath(d), sunAmount, 8)

        addBrushToMaster(master, brush)
    }

    private fun addRoad(master: Canvas, d: RectF, amount: Float) {
        if (amount <= 0f) return
        val brush = brushCanvas ?: return
        clear(brush)
        val road = roadPath(d)
        val a = amount.coerceIn(0f, 1f)

        // Multiple slightly wandering strokes, with no hard polygon edge.
        val strokes = 20
        for (i in 0 until strokes) {
            val local = ((a * strokes) - i).coerceIn(0f, 1f)
            if (local <= 0f) continue
            val u = i / (strokes - 1f)
            val hx = .5f + (u - .5f) * .035f
            val bx = .5f + (u - .5f) * .74f
            val y0 = .402f + u * .025f
            val y1 = .49f + u * .51f
            val p = Path()
            p.moveTo(d.left + d.width() * hx, d.top + d.height() * y0)
            p.cubicTo(
                d.left + d.width() * (hx + (bx - hx) * .30f),
                d.top + d.height() * (y0 + (y1-y0)*.30f),
                d.left + d.width() * (hx + (bx - hx) * .68f),
                d.top + d.height() * (y0 + (y1-y0)*.68f),
                d.left + d.width() * (hx + (bx - hx) * local),
                d.top + d.height() * (y0 + (y1-y0) * local)
            )
            val pnt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeWidth = d.height() * (.018f + (i % 4) * .004f)
                maskFilter = BlurMaskFilter(d.height() * .006f + 1f, BlurMaskFilter.Blur.NORMAL)
            }
            brush.drawPath(p, pnt)
        }
        clipBrushToPath(brush, road)
        addBrushToMaster(master, brush)
    }

    private fun addObject(
        master: Canvas,
        c: Canvas,
        d: RectF,
        amount: Float,
        pathFactory: (RectF) -> Path,
        diagonal: Boolean
    ) {
        if (amount <= 0f) return
        val path = pathFactory(d)
        val box = RectF()
        path.computeBounds(box, true)

        // Instead of drawing a complete contour, only a moving pencil trace is
        // shown. It fades as color accumulates, avoiding the "sticker outline".
        drawMovingContour(c, path, amount, box)

        val brush = brushCanvas ?: return
        clear(brush)

        val rows = 18
        val progress = amount.coerceIn(0f, 1f) * rows
        for (i in 0 until rows) {
            val local = (progress - i).coerceIn(0f, 1f)
            if (local <= 0f) continue

            if (!diagonal) {
                val y = box.top + box.height() * (i / (rows - 1f))
                val drift = sin(i * 2.4f) * box.width() * .035f
                paintStroke(
                    brush,
                    RectF(box.left, box.top, box.right, box.bottom),
                    -.18f, (y-box.top)/box.height(), 1.18f, local,
                    maxOf(box.height() * (.035f + (i % 3) * .012f), 2.2f),
                    drift
                )
            } else {
                paintDiagonalStroke(brush, box, i, local)
            }
        }

        // A few translucent-looking dry-brush passes are simulated by narrow
        // offset strokes. They break up the unnaturally perfect boundary.
        if (amount > .18f) {
            drawSoftObjectBrush(brush, d, path, ease((amount-.18f)/.82f), 7)
        }

        clipBrushToPath(brush, path)
        addBrushToMaster(master, brush)
    }

    private fun drawMovingContour(c: Canvas, path: Path, amount: Float, box: RectF) {
        val pm = PathMeasure(path, false)
        val length = pm.length
        if (length <= 0f) return
        val segment = Path()
        pm.getSegment(0f, length * amount.coerceIn(0f, 1f), segment, true)

        outlinePaint.alpha = (34f * (1f - amount * .72f)).toInt().coerceIn(0, 34)
        outlinePaint.strokeWidth = maxOf(1f, box.width() * .0035f)
        c.drawPath(segment, outlinePaint)
        outlinePaint.alpha = 0
    }

    private fun drawSoftObjectBrush(
        canvas: Canvas,
        d: RectF,
        path: Path,
        amount: Float,
        count: Int
    ) {
        val box = RectF()
        path.computeBounds(box, true)
        for (i in 0 until count) {
            val y = box.top + box.height() * ((i + .5f) / count)
            val p = Path()
            val drift = sin(i * 1.9f) * box.width() * .045f
            p.moveTo(box.left - box.width()*.12f, y + drift)
            p.cubicTo(
                box.left + box.width()*.30f, y - drift,
                box.left + box.width()*.70f, y + drift,
                box.right + box.width()*.12f * amount, y
            )
            val q = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeWidth = maxOf(1.5f, box.height() * (.012f + (i%3)*.004f))
                alpha = (115 + (i%4)*25).coerceAtMost(210)
                maskFilter = BlurMaskFilter(maxOf(1f, box.height()*.004f), BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawPath(p, q)
        }
    }

    private fun clipBrushToPath(brush: Canvas, path: Path) {
        maskPaint.reset()
        maskPaint.isAntiAlias = true
        maskPaint.color = Color.WHITE
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        brush.drawPath(path, maskPaint)
        maskPaint.xfermode = null
    }

    private fun addBrushToMaster(master: Canvas, brush: Canvas) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        master.drawBitmap(brushBitmap!!, 0f, 0f, p)
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
