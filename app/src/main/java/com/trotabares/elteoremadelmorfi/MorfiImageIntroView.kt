package com.trotabares1978.elteoremadelmorfi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class MorfiImageIntroView(context: Context) : View(context) {
    private val bitmap = BitmapFactory.decodeResource(resources, R.drawable.morfi_intro)
        ?: error("No se pudo cargar la ilustración del Morfi")
    private val backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.morfi_background)
        ?: error("No se pudo cargar el fondo del Morfi")
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var stageMasks: Array<Bitmap?> = arrayOfNulls(5)
    private var startedAt = 0L
    private var entered = false
    @Volatile private var masksReady = false

    init { setLayerType(LAYER_TYPE_SOFTWARE, null) }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        stageMasks.forEach { it?.recycle() }
        stageMasks = arrayOfNulls(5)
        masksReady = false
        Thread {
            buildStageMasks()
            post { masksReady = true; invalidate() }
        }.start()
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(Color.WHITE)
        if (!entered) {
            // El reloj de la animación NO empieza hasta que las máscaras estén listas.
            // Así evitamos que la pantalla pase del fondo directo a la ilustración completa
            // mientras se están calculando las capas en segundo plano.
            if (!masksReady) {
                val d = fitRect(width.toFloat(), height.toFloat())
                c.drawBitmap(backgroundBitmap, null, d, imagePaint)
                postInvalidateOnAnimation()
                return
            }

            if (startedAt == 0L) startedAt = System.currentTimeMillis()
            val t = (System.currentTimeMillis() - startedAt) / 1000f
            drawOpening(c, t)
            if (t < 7f) postInvalidateOnAnimation()
        }
    }

    private fun drawOpening(c: Canvas, t: Float) {
        val d = fitRect(width.toFloat(), height.toFloat())
        c.drawBitmap(backgroundBitmap, null, d, imagePaint)
        if (!masksReady) return
        drawStage(c, d, stageMasks[0], fade((t-.45f)/1.25f))
        drawStage(c, d, stageMasks[1], fade((t-1.75f)/1.35f))
        drawStage(c, d, stageMasks[2], fade((t-3.25f)/1.55f))
        drawStage(c, d, stageMasks[3], fade((t-4.95f)/1.25f))
        drawStage(c, d, stageMasks[4], fade((t-6.20f)/.80f))
    }

    private fun drawStage(c: Canvas, d: RectF, mask: Bitmap?, amount: Float) {
        if (mask == null || amount <= 0f) return
        val save = c.saveLayer(d, null)
        c.drawBitmap(bitmap, null, d, imagePaint)
        maskPaint.reset()
        maskPaint.alpha = (255f * amount.coerceIn(0f,1f)).toInt()
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        c.drawBitmap(mask, null, d, maskPaint)
        maskPaint.xfermode = null
        c.restoreToCount(save)
    }

    private fun buildStageMasks() {
        val w = bitmap.width
        val h = bitmap.height

        fun maskWithPath(build: (Path) -> Unit): Bitmap {
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.color = Color.WHITE
            val path = Path()
            build(path)
            canvas.drawPath(path, p)
            return out
        }

        // Camino: capa propia y continua; no contiene edificios ni a Matías.
        stageMasks[0] = maskWithPath { p ->
            p.moveTo(w*.485f, h*.425f)
            p.cubicTo(w*.52f, h*.45f, w*.56f, h*.53f, w*.62f, h*.64f)
            p.cubicTo(w*.70f, h*.78f, w*.82f, h*.91f, w*.90f, h*1.02f)
            p.lineTo(w*.38f, h*1.02f)
            p.cubicTo(w*.42f, h*.90f, w*.45f, h*.78f, w*.47f, h*.65f)
            p.cubicTo(w*.48f, h*.55f, w*.48f, h*.47f, w*.485f, h*.425f)
            p.close()
        }

        // Lugares: aparecen juntos, pero cada uno tiene su propia silueta.
        stageMasks[1] = maskWithPath { p ->
            p.moveTo(0f,h*.35f); p.lineTo(w*.32f,h*.35f)
            p.lineTo(w*.39f,h*.41f); p.lineTo(w*.35f,h*.48f)
            p.lineTo(0f,h*.48f); p.close()

            p.moveTo(0f,h*.47f); p.lineTo(w*.34f,h*.47f)
            p.lineTo(w*.40f,h*.53f); p.lineTo(w*.39f,h*.68f)
            p.lineTo(0f,h*.68f); p.close()

            p.moveTo(w*.70f,h*.38f); p.lineTo(w.toFloat(),h*.37f)
            p.lineTo(w.toFloat(),h*.48f); p.lineTo(w*.75f,h*.49f); p.close()

            p.moveTo(w*.69f,h*.47f); p.lineTo(w.toFloat(),h*.46f)
            p.lineTo(w.toFloat(),h*.69f); p.lineTo(w*.77f,h*.67f)
            p.lineTo(w*.68f,h*.58f); p.close()
        }

        // Matías: una capa superior independiente.
        stageMasks[2] = maskWithPath { p ->
            p.moveTo(w*.38f,h*.54f)
            p.cubicTo(w*.38f,h*.50f,w*.40f,h*.47f,w*.44f,h*.46f)
            p.cubicTo(w*.46f,h*.43f,w*.48f,h*.45f,w*.51f,h*.46f)
            p.cubicTo(w*.56f,h*.43f,w*.64f,h*.45f,w*.68f,h*.50f)
            p.cubicTo(w*.69f,h*.54f,w*.67f,h*.59f,w*.64f,h*.62f)
            p.cubicTo(w*.62f,h*.67f,w*.60f,h*.74f,w*.59f,h*.84f)
            p.lineTo(w*.59f,h*1.02f)
            p.lineTo(w*.18f,h*1.02f)
            p.cubicTo(w*.15f,h*.93f,w*.14f,h*.86f,w*.16f,h*.78f)
            p.cubicTo(w*.18f,h*.72f,w*.23f,h*.67f,w*.28f,h*.64f)
            p.cubicTo(w*.31f,h*.61f,w*.35f,h*.58f,w*.38f,h*.54f)
            p.close()
        }

        // Título: sólo la franja superior. "Más que un libro" queda fuera.
        val fp = IntArray(w*h)
        bitmap.getPixels(fp,0,w,0,0,w,h)
        val titlePx = IntArray(w*h)
        for (y in 0 until h) {
            if (y < (h*.025f).toInt() || y > (h*.255f).toInt()) continue
            for (x in 0 until w) {
                if (x < (w*.05f).toInt() || x > (w*.95f).toInt()) continue
                val f = fp[y*w+x]
                val lum = .299f*Color.red(f)+.587f*Color.green(f)+.114f*Color.blue(f)
                val a = when {
                    lum <= 100f -> 255
                    lum <= 150f -> ((150f-lum)*255f/50f).toInt().coerceIn(0,255)
                    else -> 0
                }
                titlePx[y*w+x] = Color.argb(a,255,255,255)
            }
        }
        stageMasks[3] = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).also {
            it.setPixels(titlePx,0,w,0,0,w,h)
        }

        // Entrar: únicamente el cartel original de la portada.
        stageMasks[4] = maskWithPath { p ->
            p.moveTo(w*.205f,h*.845f)
            p.lineTo(w*.795f,h*.845f)
            p.lineTo(w*.81f,h*.965f)
            p.lineTo(w*.19f,h*.965f)
            p.close()
        }
    }

    private fun fitRect(w:Float,h:Float):RectF{
        val top=h*.035f; val bottom=h*.965f; val th=bottom-top
        val s=max(w/bitmap.width,th/bitmap.height)
        val rw=bitmap.width*s; val rh=bitmap.height*s
        return RectF((w-rw)/2f,top+(th-rh)/2f,(w+rw)/2f,top+(th+rh)/2f)
    }

    private fun fade(x:Float):Float{ val v=x.coerceIn(0f,1f); return v*v*(3f-2f*v) }

    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(e.action==MotionEvent.ACTION_UP && !entered && masksReady){
            val t=(System.currentTimeMillis()-startedAt)/1000f
            val d=fitRect(width.toFloat(),height.toFloat())
            val b=RectF(d.left+d.width()*.215f,d.top+d.height()*.842f,d.left+d.width()*.790f,d.top+d.height()*.975f)
            if(t>=6.20f && b.contains(e.x,e.y)){
                entered=true
                context.startActivity(Intent(context,MenuActivity::class.java))
                (context as? Activity)?.finish()
            }
        }
        return true
    }

    override fun onDetachedFromWindow(){
        stageMasks.forEach{it?.recycle()}
        stageMasks=arrayOfNulls(5)
        super.onDetachedFromWindow()
    }
}
