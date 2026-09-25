package com.trotabares1978.elteoremadelmorfi

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

/**
 * Intro built from the real illustration.
 *
 * The important difference from the first prototypes is that the scene is NOT
 * revealed with arbitrary rectangles/blobs. Each important element has its own
 * traced mask, and a soft moving brush reveals that mask. The background is
 * drawn first, then the road, buildings one by one, Matias, title and button.
 */
class MorfiImageIntroView(context: Context) : View(context) {

    private val bitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.morfi_intro)
            ?: error("No se pudo cargar la ilustracion real")

    private var startedAt = 0L
    private var entered = false
    private var enteredAt = 0L

    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var maskBitmap: Bitmap? = null
    private var maskCanvas: Canvas? = null
    private var brushBitmap: Bitmap? = null
    private var brushCanvas: Canvas? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        clearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        maskBitmap?.recycle()
        brushBitmap?.recycle()
        maskBitmap = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        brushBitmap = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        maskCanvas = Canvas(maskBitmap!!)
        brushCanvas = Canvas(brushBitmap!!)
    }

    override fun onDraw(c: Canvas) {
        val now = System.currentTimeMillis()
        if (startedAt == 0L) startedAt = now
        c.drawColor(Color.WHITE)

        if (!entered) {
            val t = (now - startedAt) / 1000f
            drawIntro(c, t)
            if (t < 13.0f) postInvalidateOnAnimation()
        } else {
            drawMainMenu(c, ease(((now - enteredAt) / 900f).coerceIn(0f, 1f)))
        }
    }

    private fun drawIntro(c: Canvas, t: Float) {
        val d = fitRect(width.toFloat(), height.toFloat())

        // 0.0-4.1: the landscape is "drawn" in broad, irregular brush strokes.
        revealBackground(c, d, ease((t - 0.00f) / 4.10f))

        // 1.9-3.7: sun.
        revealObject(c, d, ease((t - 1.90f) / 1.80f), ::sunPath, .30f)

        // 3.1-5.1: the road grows from the horizon towards the viewer.
        revealObject(c, d, ease((t - 3.10f) / 2.00f), ::roadPath, .55f)

        // Individual places, one at a time.
        revealObject(c, d, ease((t - 5.00f) / 1.15f), ::astilleroPath, .38f)
        revealObject(c, d, ease((t - 6.00f) / 1.15f), ::pizzeriaPath, .70f)
        revealObject(c, d, ease((t - 7.00f) / 1.05f), ::clinicPath, .35f)
        revealObject(c, d, ease((t - 7.85f) / 1.15f), ::schoolPath, .78f)

        // Matias is deliberately last among the scene elements.
        revealObject(c, d, ease((t - 8.85f) / 1.90f), ::matiasPath, .46f)

        // Only after the scene exists do the title and the wooden sign appear.
        revealObject(c, d, ease((t - 10.35f) / 1.25f), ::titlePath, .18f)
        revealObject(c, d, ease((t - 11.25f) / 1.15f), ::enterPath, .55f)

        // Once complete, restore the exact original pixels. No mask remains.
        if (t >= 12.55f) c.drawBitmap(bitmap, null, d, imagePaint)
    }

    private fun revealBackground(c: Canvas, d: RectF, amount: Float) {
        if (amount <= 0f) return
        val base = maskCanvas ?: return
        val brush = brushCanvas ?: return
        clearMask(base)
        clearMask(brush)

        // Full background, then cut out every element that must appear later.
        maskPaint.color = Color.WHITE
        maskPaint.alpha = 255
        base.drawRect(d, maskPaint)
        clearPath(base, titlePath(d))
        clearPath(base, sunPath(d))
        clearPath(base, roadPath(d))
        clearPath(base, astilleroPath(d))
        clearPath(base, pizzeriaPath(d))
        clearPath(base, clinicPath(d))
        clearPath(base, schoolPath(d))
        clearPath(base, matiasPath(d))
        clearPath(base, enterPath(d))

        // A set of irregular, overlapping "brush strokes" makes the landscape
        // look drawn rather than uncovered by rectangular geometry.
        val ys = floatArrayOf(.025f, .105f, .19f, .285f, .385f, .49f, .60f, .72f, .84f, .95f)
        val starts = floatArrayOf(-.10f, -.04f, -.12f, -.02f, -.09f, -.03f, -.11f, -.05f, -.10f, -.02f)
        val ends = floatArrayOf(1.06f, 1.12f, 1.04f, 1.10f, 1.08f, 1.13f, 1.05f, 1.12f, 1.07f, 1.14f)
        val n = ys.size
        val scaled = amount.coerceIn(0f, 1f) * n
        for (i in 0 until n) {
            val local = (scaled - i).coerceIn(0f, 1f)
            if (local <= 0f) continue
            drawBrushStroke(
                brush,
                d,
                starts[i],
                ys[i],
                ends[i],
                local,
                widthFactor = .085f + (i % 3) * .012f,
                tilt = if (i % 2 == 0) -.012f else .010f
            )
        }

        intersectMask(base, brush)
        drawMasked(c, d, maskBitmap!!)
    }

    private fun revealObject(
        c: Canvas,
        d: RectF,
        amount: Float,
        pathFactory: (RectF) -> Path,
        direction: Float
    ) {
        if (amount <= 0f) return
        val base = maskCanvas ?: return
        val brush = brushCanvas ?: return
        clearMask(base)
        clearMask(brush)

        maskPaint.color = Color.WHITE
        maskPaint.alpha = 255
        base.drawPath(pathFactory(d), maskPaint)

        // Soft directional "paint" passing over the object's own silhouette.
        val rawBox = RectF()
        pathFactory(d).computeBounds(rawBox, true)
        val box = RectF(
            rawBox.left - d.width()*.02f,
            rawBox.top - d.height()*.02f,
            rawBox.right + d.width()*.02f,
            rawBox.bottom + d.height()*.02f
        )
        val sweep = when {
            direction < .45f -> directionSweep(brush, box, amount, true)
            direction < .60f -> directionSweep(brush, box, amount, false)
            else -> directionSweep(brush, box, amount, true)
        }
        if (!sweep) return

        intersectMask(base, brush)
        drawMasked(c, d, maskBitmap!!)
    }

    private fun directionSweep(brush: Canvas, box: RectF, amount: Float, leftToRight: Boolean): Boolean {
        val p = Path()
        val a = amount.coerceIn(0f, 1f)
        val w = box.width()
        val h = box.height()
        val edge = if (leftToRight) box.left + w * a else box.right - w * a

        if (leftToRight) {
            p.moveTo(box.left - w*.15f, box.top - h*.15f)
            p.lineTo(edge, box.top - h*.15f)
            p.lineTo(edge + w*.05f, box.bottom + h*.15f)
            p.lineTo(box.left - w*.15f, box.bottom + h*.15f)
        } else {
            p.moveTo(edge, box.top - h*.15f)
            p.lineTo(box.right + w*.15f, box.top - h*.15f)
            p.lineTo(box.right + w*.15f, box.bottom + h*.15f)
            p.lineTo(edge - w*.05f, box.bottom + h*.15f)
        }
        p.close()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter((w + h) * .008f + 2f, BlurMaskFilter.Blur.NORMAL)
        }
        brush.drawPath(p, paint)
        return true
    }

    private fun drawBrushStroke(
        canvas: Canvas,
        d: RectF,
        startX: Float,
        y: Float,
        endX: Float,
        amount: Float,
        widthFactor: Float,
        tilt: Float
    ) {
        val p = Path()
        val yy = d.top + d.height() * y
        val x0 = d.left + d.width() * startX
        val full = d.width() * (endX - startX)
        val x1 = x0 + full * amount
        val wobble = d.height() * .008f
        p.moveTo(x0, yy)
        p.cubicTo(
            x0 + full*.20f, yy - wobble,
            x0 + full*.38f, yy + wobble,
            x0 + full*.58f, yy
        )
        p.cubicTo(
            x0 + full*.75f, yy - wobble*.7f,
            x0 + full*.90f, yy + wobble*.7f,
            x1, yy + d.height()*tilt*amount
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = d.height() * widthFactor
            maskFilter = BlurMaskFilter(d.height()*.008f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(p, paint)
    }

    private fun drawMasked(c: Canvas, d: RectF, mask: Bitmap) {
        val save = c.saveLayer(d, null)
        c.drawBitmap(bitmap, null, d, imagePaint)
        maskPaint.reset()
        maskPaint.isAntiAlias = true
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        c.drawBitmap(mask, 0f, 0f, maskPaint)
        maskPaint.xfermode = null
        c.restoreToCount(save)
    }

    private fun intersectMask(base: Canvas, brush: Canvas) {
        val save = base.save()
        maskPaint.reset()
        maskPaint.isAntiAlias = true
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        base.drawBitmap(brushBitmap!!, 0f, 0f, maskPaint)
        maskPaint.xfermode = null
        base.restoreToCount(save)
    }

    private fun clearMask(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

    private fun clearPath(canvas: Canvas, path: Path) {
        canvas.drawPath(path, clearPaint)
    }

    private fun sunPath(d: RectF): Path = ellipsePath(
        d.left + d.width()*.610f,
        d.top + d.height()*.318f,
        d.width()*.061f,
        d.height()*.041f
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
            d.left + d.width()*.201f,
            d.top + d.height()*.828f,
            d.left + d.width()*.801f,
            d.top + d.height()*.971f
        ),
        d.width()*.025f
    )

    private fun polygonPath(d: RectF, points: Array<Pair<Float, Float>>): Path {
        val p = Path()
        points.forEachIndexed { i, q ->
            val x = d.left + d.width()*q.first
            val y = d.top + d.height()*q.second
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }

    private fun ellipsePath(cx: Float, cy: Float, rx: Float, ry: Float): Path {
        val p = Path()
        p.addOval(RectF(cx-rx, cy-ry, cx+rx, cy+ry), Path.Direction.CW)
        return p
    }

    private fun roundedRectPath(r: RectF, radius: Float): Path {
        val p = Path()
        p.addRoundRect(r, radius, radius, Path.Direction.CW)
        return p
    }

    private fun fitRect(w: Float, h: Float): RectF {
        val s = min(w/bitmap.width, h/bitmap.height)
        val rw = bitmap.width*s
        val rh = bitmap.height*s
        return RectF((w-rw)/2f, (h-rh)/2f, (w+rw)/2f, (h+rh)/2f)
    }

    private fun drawMainMenu(c: Canvas, a: Float) {
        val d = fitRect(width.toFloat(), height.toFloat())
        c.drawBitmap(bitmap, null, d, imagePaint)
        textPaint.color = Color.rgb(55,43,32)
        textPaint.alpha = (255*a).toInt()
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textSize = width*.043f
        c.drawText("EL MUNDO DE MATÍAS", width/2f, height*.735f, textPaint)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = width*.027f
        c.drawText("Elegí por dónde empezar", width/2f, height*.775f, textPaint)

        val labels = arrayOf("MORFI","¡EUREKA!","DETECTIVES","LABORATORIO")
        val xs = floatArrayOf(.27f,.27f,.73f,.73f)
        val ys = floatArrayOf(.81f,.855f,.81f,.855f)
        for (i in labels.indices) {
            textPaint.color = Color.rgb(82,111,66)
            textPaint.alpha = (235*a).toInt()
            c.drawRoundRect(
                width*(xs[i]-.20f), height*(ys[i]-.025f),
                width*(xs[i]+.20f), height*(ys[i]+.018f),
                18f,18f,textPaint
            )
            textPaint.color = Color.WHITE
            textPaint.textSize = width*.021f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            c.drawText(labels[i], width*xs[i], height*(ys[i]+.002f), textPaint)
        }
        textPaint.alpha = 255
    }

    private fun ease(x: Float): Float {
        val v = x.coerceIn(0f, 1f)
        return v*v*(3f-2f*v)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP && !entered) {
            val t = (System.currentTimeMillis() - startedAt) / 1000f
            if (t >= 11.0f) {
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
