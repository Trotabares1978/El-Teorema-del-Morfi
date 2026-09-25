package com.trotabares1978.elteoremadelmorfi

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
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

    // Fondo derivado directamente de la ilustración original, con los
    // elementos de primer plano retirados mediante inpainting. No se inventan
    // colores ni formas nuevas: el fondo conserva cielo, río, horizonte y
    // paisaje de la imagen real.
    private val backgroundBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.morfi_background)
            ?: error("No se pudo cargar el fondo del Morfi")

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

        // 1) Fondo: una capa completa e independiente. No se revela la imagen
        // final acá, por lo tanto jamás aparecen "agujeros" blancos.
        clear(master)
        drawPastelBackground(c, d, ease((t - 0.00f) / 2.40f))

        // 2) Camino: es el primer elemento que se materializa sobre el fondo.
        addRoad(master, d, ease((t - 1.90f) / 2.10f))

        // 3) Los cuatro lugares aparecen juntos, como elementos independientes.
        val places = ease((t - 3.75f) / 1.65f)
        addObject(master, c, d, places, ::astilleroPath)
        addObject(master, c, d, places, ::pizzeriaPath)
        addObject(master, c, d, places, ::clinicPath)
        addObject(master, c, d, places, ::schoolPath)

        // 4) Matías aparece después de los lugares.
        addObject(master, c, d, ease((t - 5.55f) / 1.55f), ::matiasPath)

        // 5) Título y, por último, el botón Entrar.
        addObject(master, c, d, ease((t - 6.65f) / 0.80f), ::titlePath)
        addObject(master, c, d, ease((t - 7.45f) / 0.90f), ::enterPath)

        drawMasked(c, d)

        if (t >= 8.70f) c.drawBitmap(bitmap, null, d, imagePaint)

        if (t < 9.10f) postInvalidateOnAnimation()
    }

    /**
     * Fondo real de la escena: cielo, horizonte, río y campo.
     * Es una sola capa continua; los edificios, el camino y Matías NO forman
     * parte de ella. Los objetos se superponen después mediante sus máscaras.
     */
    private fun drawPastelBackground(c: Canvas, d: RectF, amount: Float) {
        val a = ease(amount.coerceIn(0f, 1f))
        if (a <= 0f) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        p.alpha = (255f * a).toInt().coerceIn(0, 255)
        c.drawBitmap(backgroundBitmap, null, d, p)
        p.alpha = 255
    }

    /**
     * BACKGROUND LAYER — one complete, continuous layer.
     * It is deliberately independent from every foreground object.
     * No building/person/path is cut out of it, so there can never be a white
     * hole waiting for a later object.
     */
    private fun addLandscape(master: Canvas, d: RectF, amount: Float, t: Float) {
        // El fondo ya no se construye dentro de la máscara. Se dibuja como una
        // capa completa e independiente en drawPastelBackground().
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

    /**
     * One object = one independent silhouette.
     *
     * The object is never revealed through its bounding rectangle. Only the
     * actual Path of that object contributes alpha to the master mask. This
     * keeps the buildings, Matías, etc. visually independent from one another.
     */
    private fun addObject(
        master: Canvas,
        c: Canvas,
        d: RectF,
        amount: Float,
        pathFactory: (RectF) -> Path
    ) {
        if (amount <= 0f) return

        val path = pathFactory(d)
        val box = RectF()
        path.computeBounds(box, true)
        val a = amount.coerceIn(0f, 1f)

        val brush = brushCanvas ?: return
        clear(brush)

        // La máscara coincide con la silueta del objeto, no con su rectángulo.
        // Así el bitmap final solo aparece dentro de ese objeto.
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            alpha = (255f * a).toInt()
        }
        brush.drawPath(path, fill)

        // Pequeño trazo que sigue el contorno: sensación de materialización,
        // sin barridos rectangulares ni diagonales geométricas.
        drawMovingContour(c, path, a, box)

        addBrushToMaster(master, brush)
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

    private fun astilleroPath(d: RectF): Path {
        val p=Path()
        val X={u:Float->d.left+d.width()*u}; val Y={u:Float->d.top+d.height()*u}
        p.moveTo(X(0f),Y(.456f)); p.lineTo(X(0f),Y(.352f)); p.cubicTo(X(.02f),Y(.350f),X(.03f),Y(.350f),X(.035f),Y(.336f))
        p.cubicTo(X(.045f),Y(.315f),X(.052f),Y(.298f),X(.059f),Y(.297f))
        p.cubicTo(X(.075f),Y(.255f),X(.095f),Y(.252f),X(.100f),Y(.315f))
        p.cubicTo(X(.112f),Y(.323f),X(.120f),Y(.302f),X(.127f),Y(.277f))
        p.cubicTo(X(.140f),Y(.265f),X(.149f),Y(.282f),X(.150f),Y(.306f))
        p.cubicTo(X(.168f),Y(.310f),X(.173f),Y(.326f),X(.172f),Y(.339f))
        p.cubicTo(X(.198f),Y(.340f),X(.207f),Y(.354f),X(.234f),Y(.358f))
        p.cubicTo(X(.268f),Y(.365f),X(.289f),Y(.398f),X(.289f),Y(.441f))
        p.cubicTo(X(.282f),Y(.451f),X(.275f),Y(.455f),X(.270f),Y(.456f)); p.close(); return p
    }

    private fun pizzeriaPath(d: RectF): Path {
        val p=Path(); val X={u:Float->d.left+d.width()*u}; val Y={u:Float->d.top+d.height()*u}
        p.moveTo(X(0f),Y(.651f)); p.lineTo(X(0f),Y(.482f))
        p.cubicTo(X(.06f),Y(.478f),X(.14f),Y(.473f),X(.217f),Y(.473f))
        p.cubicTo(X(.235f),Y(.479f),X(.254f),Y(.487f),X(.262f),Y(.510f))
        p.cubicTo(X(.284f),Y(.516f),X(.294f),Y(.548f),X(.293f),Y(.617f))
        p.cubicTo(X(.287f),Y(.635f),X(.279f),Y(.647f),X(.271f),Y(.651f)); p.close(); return p
    }

    private fun clinicPath(d: RectF): Path {
        val p=Path(); val X={u:Float->d.left+d.width()*u}; val Y={u:Float->d.top+d.height()*u}
        p.moveTo(X(.625f),Y(.517f)); p.lineTo(X(.625f),Y(.457f))
        p.cubicTo(X(.645f),Y(.453f),X(.674f),Y(.451f),X(.697f),Y(.453f))
        p.cubicTo(X(.718f),Y(.455f),X(.731f),Y(.466f),X(.732f),Y(.497f))
        p.cubicTo(X(.718f),Y(.503f),X(.697f),Y(.508f),X(.675f),Y(.512f))
        p.cubicTo(X(.654f),Y(.515f),X(.638f),Y(.517f),X(.625f),Y(.517f)); p.close(); return p
    }

    private fun schoolPath(d: RectF): Path {
        val p=Path(); val X={u:Float->d.left+d.width()*u}; val Y={u:Float->d.top+d.height()*u}
        p.moveTo(X(.758f),Y(.647f)); p.lineTo(X(.758f),Y(.594f))
        p.cubicTo(X(.752f),Y(.575f),X(.746f),Y(.559f),X(.746f),Y(.547f))
        p.cubicTo(X(.752f),Y(.528f),X(.760f),Y(.510f),X(.766f),Y(.492f))
        p.cubicTo(X(.820f),Y(.490f),X(.918f),Y(.490f),X(1f),Y(.492f))
        p.lineTo(X(1f),Y(.647f)); p.cubicTo(X(.91f),Y(.649f),X(.83f),Y(.649f),X(.758f),Y(.647f)); p.close(); return p
    }

    private fun matiasPath(d: RectF): Path {
        val p=Path(); val X={u:Float->d.left+d.width()*u}; val Y={u:Float->d.top+d.height()*u}
        p.moveTo(X(.145f),Y(.866f))
        p.cubicTo(X(.132f),Y(.842f),X(.138f),Y(.810f),X(.168f),Y(.773f))
        p.cubicTo(X(.200f),Y(.735f),X(.250f),Y(.684f),X(.295f),Y(.630f))
        p.cubicTo(X(.322f),Y(.573f),X(.365f),Y(.520f),X(.424f),Y(.483f))
        p.cubicTo(X(.472f),Y(.449f),X(.520f),Y(.438f),X(.574f),Y(.466f))
        p.cubicTo(X(.618f),Y(.516f),X(.655f),Y(.575f),X(.660f),Y(.611f))
        p.cubicTo(X(.650f),Y(.649f),X(.620f),Y(.682f),X(.588f),Y(.733f))
        p.cubicTo(X(.558f),Y(.790f),X(.528f),Y(.861f),X(.488f),Y(1f))
        p.lineTo(X(.229f),Y(1f))
        p.cubicTo(X(.211f),Y(.963f),X(.179f),Y(.918f),X(.145f),Y(.866f)); p.close(); return p
    }
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
