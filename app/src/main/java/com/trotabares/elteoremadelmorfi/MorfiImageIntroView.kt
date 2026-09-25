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
    private var stageMasks: Array<Bitmap?> = arrayOfNulls(5)

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

        for (i in stageMasks.indices) {
            stageMasks[i]?.recycle()
            stageMasks[i] = null
        }
        buildStageMasks()
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
        val bg = backgroundBitmap

        // Estados completos y acumulativos. Nada se descubre por barridos.
        val backgroundAmount = fade(t / 0.35f)
        val roadAmount = fade((t - 0.70f) / 1.20f)
        val placesAmount = fade((t - 1.85f) / 1.45f)
        val matiasAmount = fade((t - 3.25f) / 1.30f)
        val titleAmount = fade((t - 4.05f) / 2.35f)
        val enterAmount = fade((t - 5.70f) / 1.20f)

        if (backgroundAmount > 0f) {
            imagePaint.alpha = (255f * backgroundAmount).toInt()
            c.drawBitmap(bg, null, d, imagePaint)
            imagePaint.alpha = 255
        }

        drawStage(c, d, stageMasks[0], roadAmount)
        drawStage(c, d, stageMasks[1], placesAmount)
        drawStage(c, d, stageMasks[2], matiasAmount)
        drawTitleStage(c, d, stageMasks[3], titleAmount)
        drawStage(c, d, stageMasks[4], enterAmount)
        if (t < 8.20f) postInvalidateOnAnimation()
    }

    private fun drawTitleStage(c: Canvas, d: RectF, mask: Bitmap?, amount: Float) {
        if (mask == null || amount <= 0f) return
        val save = c.saveLayer(d, null)
        imagePaint.alpha = 255
        c.drawBitmap(bitmap, null, d, imagePaint)

        // El título se revela de manera continua desde arriba hacia abajo,
        // usando la máscara real del título. No se dibuja el rectángulo de
        // fondo ni se hace un salto desde un fragmento al título completo.
        val reveal = amount.coerceIn(0f, 1f)
        val titleTop = d.top + d.height() * .025f
        val titleBottom = d.top + d.height() * .30f
        val revealY = titleTop + (titleBottom - titleTop) * reveal

        maskPaint.reset()
        maskPaint.isAntiAlias = true
        maskPaint.shader = LinearGradient(
            0f, titleTop, 0f, titleBottom,
            intArrayOf(Color.TRANSPARENT, Color.WHITE, Color.WHITE),
            floatArrayOf(
                (reveal - .16f).coerceIn(0f, 1f),
                (reveal - .03f).coerceIn(0f, 1f),
                reveal.coerceIn(0f, 1f)
            ),
            Shader.TileMode.CLAMP
        )
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        c.drawBitmap(mask, null, d, maskPaint)
        maskPaint.shader = null
        maskPaint.xfermode = null
        c.restoreToCount(save)
    }

    private fun drawStage(c: Canvas, d: RectF, mask: Bitmap?, amount: Float) {
        if (mask == null || amount <= 0f) return
        val save = c.saveLayer(d, null)
        imagePaint.alpha = 255
        c.drawBitmap(bitmap, null, d, imagePaint)
        maskPaint.reset()
        maskPaint.isAntiAlias = true
        maskPaint.alpha = (255f * amount.coerceIn(0f, 1f)).toInt()
        maskPaint.maskFilter = BlurMaskFilter(1.35f, BlurMaskFilter.Blur.NORMAL)
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        c.drawBitmap(mask, null, d, maskPaint)
        maskPaint.xfermode = null
        maskPaint.maskFilter = null
        c.restoreToCount(save)
        maskPaint.alpha = 255
    }

    private fun buildStageMasks() {
        val fw = bitmap.width
        val fh = bitmap.height
        val bw = backgroundBitmap.width
        val bh = backgroundBitmap.height
        val sx = fw.toFloat() / bw.toFloat()
        val sy = fh.toFloat() / bh.toFloat()
        val regions = arrayOf(
            RectF(.00f, .38f, 1.00f, 1.00f),
            RectF(.00f, .28f, 1.00f, .70f),
            RectF(.08f, .48f, .72f, 1.00f),
            RectF(.05f, .00f, .95f, .33f),
            RectF(.18f, .82f, .82f, 1.00f)
        )
        val finalPixels = IntArray(fw * fh)
        bitmap.getPixels(finalPixels, 0, fw, 0, 0, fw, fh)

        for (index in regions.indices) {
            val mask = Bitmap.createBitmap(fw, fh, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(fw * fh)
            val region = regions[index]
            for (y in 0 until fh) {
                val yn = y.toFloat() / fh
                if (yn < region.top || yn > region.bottom) continue
                for (x in 0 until fw) {
                    val xn = x.toFloat() / fw
                    if (xn < region.left || xn > region.right) continue
                    val bx = (x / sx).toInt().coerceIn(0, bw - 1)
                    val by = (y / sy).toInt().coerceIn(0, bh - 1)
                    val f = finalPixels[y * fw + x]
                    val b = backgroundBitmap.getPixel(bx, by)
                    val dist = kotlin.math.abs(Color.red(f) - Color.red(b)) +
                        kotlin.math.abs(Color.green(f) - Color.green(b)) +
                        kotlin.math.abs(Color.blue(f) - Color.blue(b))
                    val alpha = when {
                        dist >= 120 -> 255
                        dist >= 75 -> ((dist - 75) * 255 / 45).coerceIn(0, 255)
                        else -> 0
                    }
                    pixels[y * fw + x] = Color.argb(alpha, 255, 255, 255)
                }
            }
            mask.setPixels(pixels, 0, fw, 0, 0, fw, fh)
            stageMasks[index] = mask
        }
    }

    /**
     * Fondo REAL tomado de la ilustración original.
     * Es una sola capa continua; los elementos de primer plano se descuentan.
     */
    private fun addBackgroundLayer(master: Canvas, c: Canvas, d: RectF, amount: Float) {
        if (amount <= 0f) return
        val brush = brushCanvas ?: return
        clear(brush)

        val mask = Path()
        mask.fillType = Path.FillType.EVEN_ODD
        mask.addRect(d, Path.Direction.CW)
        mask.addPath(titlePath(d))
        mask.addPath(roadPath(d))
        mask.addPath(astilleroPath(d))
        mask.addPath(pizzeriaPath(d))
        mask.addPath(clinicPath(d))
        mask.addPath(schoolPath(d))
        mask.addPath(matiasPath(d))
        mask.addPath(enterPath(d))

        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            alpha = (255f * amount.coerceIn(0f, 1f)).toInt()
        }
        brush.drawPath(mask, p)
        addBrushToMaster(master, brush)
    }

    /**
     * Fondo real de la escena: cielo, horizonte, río y campo.
     * Es una sola capa continua; los edificios, el camino y Matías NO forman
     * parte de ella. Los objetos se superponen después mediante sus máscaras.
     */
    private fun drawPastelBackground(c: Canvas, d: RectF, amount: Float) {
        val a = amount.coerceIn(0f, 1f)
        if (a <= 0f) return

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val h = d.height()

        // Cielo pastel completo.
        p.color = Color.rgb(247, 235, 205)
        p.alpha = (255 * a).toInt()
        c.drawRect(d.left, d.top, d.right, d.top + h * .405f, p)

        // Río al fondo.
        p.color = Color.rgb(183, 211, 205)
        c.drawRect(d.left, d.top + h * .405f, d.right, d.top + h * .505f, p)

        // Campo continuo.
        p.color = Color.rgb(190, 207, 157)
        c.drawRect(d.left, d.top + h * .505f, d.right, d.bottom, p)

        // Segunda masa de campo para conservar la sensación orgánica de la
        // ilustración sin introducir recortes ni huecos.
        val field = Path()
        field.moveTo(d.left, d.top + h * .555f)
        field.cubicTo(
            d.left + d.width() * .22f, d.top + h * .515f,
            d.left + d.width() * .44f, d.top + h * .575f,
            d.left + d.width() * .64f, d.top + h * .535f
        )
        field.cubicTo(
            d.left + d.width() * .80f, d.top + h * .505f,
            d.left + d.width() * .94f, d.top + h * .555f,
            d.right, d.top + h * .525f
        )
        field.lineTo(d.right, d.bottom)
        field.lineTo(d.left, d.bottom)
        field.close()
        p.color = Color.rgb(205, 215, 164)
        c.drawPath(field, p)

        // Sol suave, también perteneciente al fondo.
        val sun = Path()
        sun.addOval(
            RectF(
                d.left + d.width() * .549f,
                d.top + h * .270f,
                d.left + d.width() * .671f,
                d.top + h * .352f
            ),
            Path.Direction.CW
        )
        p.color = Color.rgb(239, 198, 126)
        p.alpha = (235 * a).toInt()
        c.drawPath(sun, p)
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
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            alpha = (255f * amount.coerceIn(0f, 1f)).toInt()
        }
        brush.drawPath(roadPath(d), p)
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
        val brush = brushCanvas ?: return
        clear(brush)

        // Cada elemento aparece como una sola pieza completa.
        // Nada de barridos, diagonales ni recorrido del contorno.
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            alpha = (255f * amount.coerceIn(0f, 1f)).toInt()
        }
        brush.drawPath(path, fill)
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
        // La materialización ya no recorre el contorno.
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
        d.left + d.width() * .579f,
        d.top + d.height() * .312f,
        d.width() * .035f,
        d.height() * .035f
    )

    private fun roadPath(d: RectF): Path = polygonPath(d, arrayOf(
        .488f to .405f, .535f to .405f,
        .610f to .535f, .760f to .700f, 1.000f to 1.000f,
        .000f to 1.000f, .240f to .700f, .405f to .535f
    ))

    private fun astilleroPath(d: RectF): Path {
        val p=Path(); val X={u:Float->d.left+d.width()*u}; val Y={u:Float->d.top+d.height()*u}
        p.moveTo(X(0f),Y(.285f)); p.lineTo(X(.35f),Y(.285f))
        p.cubicTo(X(.36f),Y(.325f),X(.36f),Y(.395f),X(.35f),Y(.455f))
        p.lineTo(X(0f),Y(.455f)); p.close(); return p
    }

    private fun pizzeriaPath(d: RectF): Path {
        val p=Path(); val X={u:Float->d.left+d.width()*u}; val Y={u:Float->d.top+d.height()*u}
        p.moveTo(X(0f),Y(.465f))
        p.cubicTo(X(.10f),Y(.455f),X(.25f),Y(.455f),X(.35f),Y(.475f))
        p.lineTo(X(.35f),Y(.650f)); p.lineTo(X(0f),Y(.650f)); p.close(); return p
    }

    private fun clinicPath(d: RectF): Path {
        val p=Path(); val X={u:Float->d.left+d.width()*u}; val Y={u:Float->d.top+d.height()*u}
        p.moveTo(X(.585f),Y(.430f))
        p.cubicTo(X(.625f),Y(.420f),X(.685f),Y(.420f),X(.720f),Y(.440f))
        p.lineTo(X(.735f),Y(.525f))
        p.cubicTo(X(.690f),Y(.535f),X(.625f),Y(.535f),X(.585f),Y(.520f))
        p.close(); return p
    }

    private fun schoolPath(d: RectF): Path {
        val p=Path(); val X={u:Float->d.left+d.width()*u}; val Y={u:Float->d.top+d.height()*u}
        p.moveTo(X(.735f),Y(.465f))
        p.cubicTo(X(.805f),Y(.450f),X(.920f),Y(.450f),X(1f),Y(.465f))
        p.lineTo(X(1f),Y(.675f))
        p.cubicTo(X(.920f),Y(.685f),X(.805f),Y(.680f),X(.735f),Y(.665f))
        p.close(); return p
    }

    private fun matiasPath(d: RectF): Path {
        val p=Path(); val X={u:Float->d.left+d.width()*u}; val Y={u:Float->d.top+d.height()*u}
        p.moveTo(X(.105f),Y(.860f))
        p.cubicTo(X(.115f),Y(.780f),X(.180f),Y(.690f),X(.250f),Y(.610f))
        p.cubicTo(X(.305f),Y(.535f),X(.370f),Y(.475f),X(.475f),Y(.475f))
        p.cubicTo(X(.565f),Y(.475f),X(.625f),Y(.535f),X(.655f),Y(.620f))
        p.cubicTo(X(.670f),Y(.690f),X(.630f),Y(.755f),X(.585f),Y(.835f))
        p.cubicTo(X(.555f),Y(.900f),X(.535f),Y(.965f),X(.525f),Y(1f))
        p.lineTo(X(.205f),Y(1f))
        p.cubicTo(X(.160f),Y(.965f),X(.120f),Y(.915f),X(.105f),Y(.860f)); p.close(); return p
    }

    private fun titlePath(d: RectF): Path = polygonPath(d, arrayOf(
        .090f to .030f, .920f to .030f,
        .930f to .300f, .070f to .300f
    ))

    private fun enterPath(d: RectF): Path = roundedRectPath(
        RectF(
            d.left + d.width() * .215f,
            d.top + d.height() * .842f,
            d.left + d.width() * .790f,
            d.top + d.height() * .975f
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
        // Center-crop: la ilustración ocupa TODO el display, sin franjas
        // blancas arriba/abajo ni a los costados.
        val s = maxOf(w / bitmap.width, h / bitmap.height)
        val rw = bitmap.width * s
        val rh = bitmap.height * s
        // Deja margen de seguridad vertical para que el título no quede
        // debajo del recorte del center-crop.
        val safeTop = h * 0.035f
        val safeBottom = h * 0.965f
        val targetH = safeBottom - safeTop
        val ss = maxOf(w / bitmap.width, targetH / bitmap.height)
        val rrw = bitmap.width * ss
        val rrh = bitmap.height * ss
        return RectF((w - rrw) / 2f, safeTop + (targetH - rrh) / 2f, (w + rrw) / 2f, safeTop + (targetH + rrh) / 2f)
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

    private fun fade(x: Float): Float {
        val v = x.coerceIn(0f, 1f)
        // Suave, pero con presencia desde el primer instante: evita los
        // segundos de pantalla blanca y el efecto de "aparece de golpe".
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
