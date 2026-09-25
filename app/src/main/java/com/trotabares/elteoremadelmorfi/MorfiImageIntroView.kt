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

        // Independent objects: background -> road -> locations -> Matías -> controls.
        addObject(master, c, d, ease((t - 5.55f) / 1.25f), ::astilleroPath, diagonal = true)
        addObject(master, c, d, ease((t - 6.85f) / 1.15f), ::pizzeriaPath, diagonal = false)
        addObject(master, c, d, ease((t - 8.05f) / 1.15f), ::clinicPath, diagonal = false)
        addObject(master, c, d, ease((t - 9.25f) / 1.25f), ::schoolPath, diagonal = false)
        addObject(master, c, d, ease((t - 10.60f) / 1.55f), ::matiasPath, diagonal = true)
        addObject(master, c, d, ease((t - 12.35f) / 1.10f), ::titlePath, diagonal = false)
        addObject(master, c, d, ease((t - 13.45f) / 1.00f), ::enterPath, diagonal = true)

        drawMasked(c, d)

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

    private fun addObject(master: Canvas,c: Canvas,d: RectF,amount: Float,pathFactory: (RectF)->Path,diagonal: Boolean) {
        if (amount <= 0f) return
        val path=pathFactory(d); val box=RectF(); path.computeBounds(box,true); val a=amount.coerceIn(0f,1f)
        val alpha=(255f*ease(a)).toInt().coerceIn(0,255)
        val save=c.saveLayer(box,null)
        imagePaint.alpha=alpha; c.drawBitmap(bitmap,null,d,imagePaint); imagePaint.alpha=255
        maskPaint.reset(); maskPaint.isAntiAlias=true; maskPaint.color=Color.WHITE
        maskPaint.xfermode=PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        c.drawPath(path,maskPaint); maskPaint.xfermode=null; c.restoreToCount(save)
        drawMovingContour(c,path,a,box)
        val brush=brushCanvas ?: return
        clear(brush)
        brush.drawPath(path,Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE;style=Paint.Style.FILL;this.alpha=alpha})
        addBrushToMaster(master,brush)
    }

    private fun paintStroke(
        canvas: Canvas,
        d: RectF,
        x0: Float,
        y: Float,
        x1: Float,
        amount: Float,
        strokeWidth: Float,
        wobble: Float
    ) {
        val yy = d.top + d.height() * y
        val p = Path()
        p.moveTo(d.left + d.width() * x0, yy + wobble)
        p.cubicTo(
            d.left + d.width() * (x0 + (x1 - x0) * .32f), yy - wobble,
            d.left + d.width() * (x0 + (x1 - x0) * .68f), yy + wobble,
            d.left + d.width() * (x0 + (x1 - x0) * amount), yy
        )
        val q = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.strokeWidth = strokeWidth
            maskFilter = BlurMaskFilter(maxOf(1f, strokeWidth * .16f), BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(p, q)
    }

    private fun paintDiagonalStroke(canvas: Canvas, box: RectF, index: Int, amount: Float) {
        val u = index / 17f
        val p = Path()
        val x0 = box.left - box.width() * .12f + box.width() * u * .10f
        val y0 = box.top + box.height() * (u + .10f)
        val x1 = box.right + box.width() * .08f
        val y1 = box.bottom - box.height() * (.12f - u * .08f)
        p.moveTo(x0, y0)
        p.cubicTo(
            x0 + box.width() * .28f, y0 + box.height() * .10f,
            x1 - box.width() * .28f, y1 - box.height() * .10f,
            x0 + (x1 - x0) * amount, y0 + (y1 - y0) * amount
        )
        val q = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = maxOf(2f, box.height() * (.028f + (index % 3) * .008f))
            maskFilter = BlurMaskFilter(maxOf(1f, strokeWidth * .14f), BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(p, q)
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

    private fun astilleroPath(d: RectF): Path { val p=Path(); p.moveTo(d.left,d.top+d.height()*.456f); p.lineTo(d.left,d.top+d.height()*.352f); p.lineTo(d.left+d.width()*.035f,d.top+d.height()*.352f); p.lineTo(d.left+d.width()*.059f,d.top+d.height()*.297f); p.cubicTo(d.left+d.width()*.078f,d.top+d.height()*.263f,d.left+d.width()*.096f,d.top+d.height()*.263f,d.left+d.width()*.100f,d.top+d.height()*.315f); p.cubicTo(d.left+d.width()*.121f,d.top+d.height()*.315f,d.left+d.width()*.127f,d.top+d.height()*.277f,d.left+d.width()*.146f,d.top+d.height()*.277f); p.cubicTo(d.left+d.width()*.150f,d.top+d.height()*.306f,d.left+d.width()*.172f,d.top+d.height()*.306f,d.left+d.width()*.172f,d.top+d.height()*.339f); p.cubicTo(d.left+d.width()*.195f,d.top+d.height()*.339f,d.left+d.width()*.199f,d.top+d.height()*.358f,d.left+d.width()*.234f,d.top+d.height()*.358f); p.cubicTo(d.left+d.width()*.266f,d.top+d.height()*.377f,d.left+d.width()*.289f,d.top+d.height()*.397f,d.left+d.width()*.289f,d.top+d.height()*.441f); p.lineTo(d.left+d.width()*.270f,d.top+d.height()*.456f); p.close(); return p }
    private fun pizzeriaPath(d: RectF): Path { val p=Path(); p.moveTo(d.left,d.top+d.height()*.651f); p.lineTo(d.left,d.top+d.height()*.482f); p.cubicTo(d.left+d.width()*.08f,d.top+d.height()*.477f,d.left+d.width()*.17f,d.top+d.height()*.473f,d.left+d.width()*.217f,d.top+d.height()*.473f); p.cubicTo(d.left+d.width()*.235f,d.top+d.height()*.487f,d.left+d.width()*.262f,d.top+d.height()*.487f,d.left+d.width()*.262f,d.top+d.height()*.510f); p.cubicTo(d.left+d.width()*.282f,d.top+d.height()*.510f,d.left+d.width()*.293f,d.top+d.height()*.535f,d.left+d.width()*.293f,d.top+d.height()*.617f); p.lineTo(d.left+d.width()*.271f,d.top+d.height()*.651f); p.close(); return p }
    private fun clinicPath(d: RectF): Path { val p=Path(); p.moveTo(d.left+d.width()*.625f,d.top+d.height()*.517f); p.lineTo(d.left+d.width()*.625f,d.top+d.height()*.457f); p.cubicTo(d.left+d.width()*.645f,d.top+d.height()*.453f,d.left+d.width()*.675f,d.top+d.height()*.452f,d.left+d.width()*.697f,d.top+d.height()*.453f); p.cubicTo(d.left+d.width()*.716f,d.top+d.height()*.455f,d.left+d.width()*.732f,d.top+d.height()*.459f,d.left+d.width()*.732f,d.top+d.height()*.497f); p.cubicTo(d.left+d.width()*.715f,d.top+d.height()*.503f,d.left+d.width()*.700f,d.top+d.height()*.508f,d.left+d.width()*.625f,d.top+d.height()*.517f); p.close(); return p }
    private fun schoolPath(d: RectF): Path { val p=Path(); p.moveTo(d.left+d.width()*.758f,d.top+d.height()*.647f); p.lineTo(d.left+d.width()*.758f,d.top+d.height()*.594f); p.cubicTo(d.left+d.width()*.752f,d.top+d.height()*.574f,d.left+d.width()*.746f,d.top+d.height()*.559f,d.left+d.width()*.746f,d.top+d.height()*.547f); p.cubicTo(d.left+d.width()*.752f,d.top+d.height()*.530f,d.left+d.width()*.760f,d.top+d.height()*.512f,d.left+d.width()*.766f,d.top+d.height()*.492f); p.cubicTo(d.left+d.width()*.825f,d.top+d.height()*.490f,d.left+d.width()*.930f,d.top+d.height()*.491f,d.right,d.top+d.height()*.492f); p.lineTo(d.right,d.top+d.height()*.647f); p.close(); return p }
    private fun matiasPath(d: RectF): Path { val p=Path(); p.moveTo(d.left+d.width()*.145f,d.top+d.height()*.866f); p.cubicTo(d.left+d.width()*.130f,d.top+d.height()*.842f,d.left+d.width()*.139f,d.top+d.height()*.813f,d.left+d.width()*.168f,d.top+d.height()*.773f); p.cubicTo(d.left+d.width()*.205f,d.top+d.height()*.732f,d.left+d.width()*.252f,d.top+d.height()*.684f,d.left+d.width()*.295f,d.top+d.height()*.630f); p.cubicTo(d.left+d.width()*.326f,d.top+d.height()*.565f,d.left+d.width()*.365f,d.top+d.height()*.521f,d.left+d.width()*.424f,d.top+d.height()*.483f); p.cubicTo(d.left+d.width()*.474f,d.top+d.height()*.448f,d.left+d.width()*.518f,d.top+d.height()*.438f,d.left+d.width()*.574f,d.top+d.height()*.466f); p.cubicTo(d.left+d.width()*.619f,d.top+d.height()*.521f,d.left+d.width()*.660f,d.top+d.height()*.579f,d.left+d.width()*.660f,d.top+d.height()*.611f); p.cubicTo(d.left+d.width()*.650f,d.top+d.height()*.650f,d.left+d.width()*.617f,d.top+d.height()*.680f,d.left+d.width()*.588f,d.top+d.height()*.733f); p.cubicTo(d.left+d.width()*.559f,d.top+d.height()*.792f,d.left+d.width()*.528f,d.top+d.height()*.861f,d.left+d.width()*.488f,d.top+d.height()*1.0f); p.lineTo(d.left+d.width()*.229f,d.top+d.height()*1.0f); p.cubicTo(d.left+d.width()*.205f,d.top+d.height()*.950f,d.left+d.width()*.170f,d.top+d.height()*.911f,d.left+d.width()*.145f,d.top+d.height()*.866f); p.close(); return p }
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
