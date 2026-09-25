package com.trotabares1978.elteoremadelmorfi

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class MorfiImageIntroView(context: Context) : View(context) {
    private val bitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.morfi_intro)
        ?: error("No se pudo cargar la ilustración real")

    // Start only when the first frame is actually drawn. This prevents MediaPlayer
    // preparation from consuming the whole intro before the user sees it.
    private var startedAt = 0L
    private var entered = false
    private var enteredAt = 0L

    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(c: Canvas) {
        val now = System.currentTimeMillis()
        if (startedAt == 0L) startedAt = now

        c.drawColor(Color.WHITE)

        if (!entered) {
            val t = (now - startedAt) / 1000f
            drawIntro(c, t)
            if (t < 10.8f) postInvalidateOnAnimation()
        } else {
            drawMainMenu(c, (now - enteredAt) / 900f)
        }
    }

    private fun drawIntro(c: Canvas, t: Float) {
        val d = fitRect(width.toFloat(), height.toFloat())

        // The reveal is cumulative: once an element appears it stays visible.
        // Nothing draws the complete illustration until the sequence is finished.

        val maskLayer = c.saveLayer(d, null)
        c.drawColor(Color.WHITE)

        // 0.0–1.25 — sky, with an irregular hand-drawn horizon.
        revealBand(c, d, 0.00f, 0.00f, 1.00f, 0.35f,
            ease((t - 0.05f) / 1.20f), 0.010f)

        // 0.75–1.9 — sun.
        revealBlob(c, d, 0.61f, 0.36f, 0.12f, 0.075f,
            ease((t - 0.75f) / 1.15f), 6f)

        // 1.35–2.65 — river.
        revealBand(c, d, 0.00f, 0.335f, 1.00f, 0.49f,
            ease((t - 1.35f) / 1.30f), 0.012f)

        // 2.05–3.65 — road grows from the horizon toward Matías.
        revealRoad(c, d, ease((t - 2.05f) / 1.60f))

        // 3.0–4.0 — Astillero + ship, following the actual left-side silhouette.
        revealAstillero(c, d, ease((t - 3.00f) / 1.00f))

        // 3.75–4.75 — pizzeria.
        revealPizzeria(c, d, ease((t - 3.75f) / 1.00f))

        // 4.50–5.50 — clinic.
        revealClinic(c, d, ease((t - 4.50f) / 1.00f))

        // 5.25–6.25 — school.
        revealSchool(c, d, ease((t - 5.25f) / 1.00f))

        // 5.85–7.75 — Matías, with a tighter character-shaped silhouette.
        revealMatias(c, d, ease((t - 5.85f) / 1.90f))

        // 7.20–8.55 — title area.
        revealTitle(c, d, ease((t - 7.20f) / 1.35f))

        // 8.25–9.55 — wooden ENTRAR sign.
        revealEnter(c, d, ease((t - 8.25f) / 1.30f))

        // Apply cumulative reveal mask to the original illustration.
        maskPaint.reset()
        maskPaint.isAntiAlias = true
        maskPaint.color = Color.WHITE
        maskPaint.style = Paint.Style.FILL
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

        // The shapes above were drawn on the same layer; use the image itself as
        // the destination in a second isolated layer so previous reveals remain.
        c.restoreToCount(maskLayer)

        // Re-render cleanly using the same sequence as an alpha mask.
        val reveal = c.saveLayer(d, null)
        c.drawBitmap(bitmap, null, d, imagePaint)

        val mask = Path()
        val mc = Canvas()
        val mb = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        mc.setBitmap(mb)
        mc.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        buildMask(mc, d, t)

        maskPaint.reset()
        maskPaint.isAntiAlias = true
        maskPaint.color = Color.WHITE
        maskPaint.style = Paint.Style.FILL
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        c.drawBitmap(mb, 0f, 0f, maskPaint)
        maskPaint.xfermode = null
        c.restoreToCount(reveal)
        mb.recycle()

        if (t >= 9.70f) {
            c.drawBitmap(bitmap, null, d, imagePaint)
        }
    }

    private fun buildMask(c: Canvas, d: RectF, t: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }

        revealBand(c, d, 0f, 0f, 1f, .35f, ease((t-.05f)/1.20f), .010f, p)
        revealBlob(c, d, .61f, .36f, .12f, .075f, ease((t-.75f)/1.15f), 6f, p)
        revealBand(c, d, 0f, .335f, 1f, .49f, ease((t-1.35f)/1.30f), .012f, p)
        revealRoad(c, d, ease((t-2.05f)/1.60f), p)
        revealAstillero(c, d, ease((t-3.00f)/1.00f), p)
        revealPizzeria(c, d, ease((t-3.75f)/1.00f), p)
        revealClinic(c, d, ease((t-4.50f)/1.00f), p)
        revealSchool(c, d, ease((t-5.25f)/1.00f), p)
        revealMatias(c, d, ease((t-5.85f)/1.90f), p)
        revealTitle(c, d, ease((t-7.20f)/1.35f), p)
        revealEnter(c, d, ease((t-8.25f)/1.30f), p)
    }

    private fun revealBand(c: Canvas, d: RectF, l: Float, top: Float, r: Float, bottom: Float, amount: Float, wobble: Float, paint: Paint = maskPaint) {
        if (amount <= 0f) return
        val a = amount.coerceIn(0f,1f)
        val y0 = d.top + d.height()*top
        val y1 = y0 + d.height()*(bottom-top)*a
        val x0 = d.left + d.width()*l
        val x1 = d.left + d.width()*r
        val w = d.width()
        val p = Path()
        p.moveTo(x0,y0)
        p.cubicTo(x0+w*.22f,y0-w*wobble,x0+w*.40f,y0+w*wobble,x0+w*.58f,y0)
        p.cubicTo(x0+w*.76f,y0-w*wobble,x0+w*.90f,y0+w*wobble,x1,y0)
        p.lineTo(x1,y1)
        p.cubicTo(x0+w*.88f,y1+w*wobble,x0+w*.68f,y1-w*wobble,x0+w*.50f,y1)
        p.cubicTo(x0+w*.32f,y1+w*wobble,x0+w*.12f,y1-w*wobble,x0,y1)
        p.close()
        paint.maskFilter = if (amount < .98f) BlurMaskFilter(5f, BlurMaskFilter.Blur.NORMAL) else null
        c.drawPath(p, paint)
        paint.maskFilter = null
    }

    private fun revealBlob(c: Canvas, d: RectF, cx: Float, cy: Float, rw: Float, rh: Float, amount: Float, blur: Float, paint: Paint = maskPaint) {
        if (amount <= 0f) return
        val a=amount.coerceIn(0f,1f)
        val x=d.left+d.width()*cx
        val y=d.top+d.height()*cy
        val w=d.width()*rw*(.18f+.82f*a)
        val h=d.height()*rh*(.18f+.82f*a)
        val p=Path()
        organicEllipsePath(p,x,y,w,h)
        paint.maskFilter=BlurMaskFilter(blur,BlurMaskFilter.Blur.NORMAL)
        c.drawPath(p,paint)
        paint.maskFilter=null
    }

    private fun revealRoad(c: Canvas,d: RectF,amount:Float,paint:Paint=maskPaint){
        if(amount<=0f)return
        val a=amount.coerceIn(0f,1f)
        val horizon=d.top+d.height()*.445f
        val bottom=d.top+d.height()*(.50f+.52f*a)
        val cx=d.left+d.width()*.515f
        val p=Path()
        p.moveTo(cx-d.width()*.012f,horizon)
        p.cubicTo(cx-d.width()*.03f,horizon+d.height()*.08f,cx-d.width()*(.07f+.10f*a),bottom-d.height()*.08f,cx-d.width()*(.055f+.25f*a),bottom)
        p.lineTo(cx+d.width()*(.055f+.25f*a),bottom)
        p.cubicTo(cx+d.width()*(.07f+.10f*a),bottom-d.height()*.08f,cx+d.width()*.03f,horizon+d.height()*.08f,cx+d.width()*.012f,horizon)
        p.close()
        paint.maskFilter=BlurMaskFilter(7f,BlurMaskFilter.Blur.NORMAL)
        c.drawPath(p,paint)
        paint.maskFilter=null
    }

    private fun revealAstillero(c:Canvas,d:RectF,amount:Float,paint:Paint=maskPaint){
        revealRegion(c,d,amount, arrayOf(
            .00f to .39f,.37f to .39f,.40f to .48f,.39f to .55f,.34f to .63f,.02f to .70f,.00f to .70f
        ),paint)
    }
    private fun revealPizzeria(c:Canvas,d:RectF,amount:Float,paint:Paint=maskPaint){
        revealRegion(c,d,amount,arrayOf(
            .00f to .69f,.27f to .69f,.31f to .77f,.29f to .88f,.12f to .93f,.00f to .91f
        ),paint)
    }
    private fun revealClinic(c:Canvas,d:RectF,amount:Float,paint:Paint=maskPaint){
        revealRegion(c,d,amount,arrayOf(
            .68f to .68f,.91f to .68f,1f to .72f,1f to .84f,.84f to .84f,.66f to .82f
        ),paint)
    }
    private fun revealSchool(c:Canvas,d:RectF,amount:Float,paint:Paint=maskPaint){
        revealRegion(c,d,amount,arrayOf(
            .77f to .70f,1f to .70f,1f to .88f,.75f to .88f,.70f to .80f
        ),paint)
    }

    private fun revealMatias(c:Canvas,d:RectF,amount:Float,paint:Paint=maskPaint){
        if(amount<=0f)return
        val a=amount.coerceIn(0f,1f)
        val p=Path()
        val left=d.left+d.width()*.13f
        val right=d.left+d.width()*.68f
        val top=d.top+d.height()*(.70f+.03f*(1f-a))
        val bot=d.top+d.height()*(1f+.04f*(1f-a))
        p.moveTo(left,bot)
        p.cubicTo(left-d.width()*.02f,bot-d.height()*.12f,left+d.width()*.04f,top+d.height()*.16f,left+d.width()*.20f,top+d.height()*.06f)
        p.cubicTo(left+d.width()*.24f,top-d.height()*.03f,left+d.width()*.30f,top-d.height()*.08f,left+d.width()*.39f,top)
        p.cubicTo(left+d.width()*.47f,top-d.height()*.02f,left+d.width()*.52f,top+d.height()*.01f,right-d.width()*.02f,top+d.height()*.10f)
        p.cubicTo(right,top+d.height()*.25f,right-d.width()*.06f,bot-d.height()*.08f,right-d.width()*.02f,bot)
        p.close()
        paint.maskFilter=BlurMaskFilter(6f,BlurMaskFilter.Blur.NORMAL)
        c.drawPath(p,paint)
        paint.maskFilter=null
    }

    private fun revealTitle(c:Canvas,d:RectF,amount:Float,paint:Paint=maskPaint){
        if(amount<=0f)return
        val a=amount.coerceIn(0f,1f)
        val p=Path()
        val y=d.top+d.height()*.045f
        val h=d.height()*.27f*a
        p.moveTo(d.left+d.width()*.07f,y+h*.15f)
        p.cubicTo(d.left+d.width()*.20f,y-h*.02f,d.left+d.width()*.36f,y+h*.04f,d.left+d.width()*.50f,y)
        p.cubicTo(d.left+d.width()*.66f,y+h*.05f,d.left+d.width()*.84f,y-h*.01f,d.left+d.width()*.94f,y+h*.13f)
        p.lineTo(d.left+d.width()*.91f,y+h)
        p.cubicTo(d.left+d.width()*.70f,y+h*.92f,d.left+d.width()*.48f,y+h*1.02f,d.left+d.width()*.25f,y+h*.94f)
        p.cubicTo(d.left+d.width()*.14f,y+h*.90f,d.left+d.width()*.08f,y+h*.62f,d.left+d.width()*.07f,y+h*.15f)
        p.close()
        paint.maskFilter=BlurMaskFilter(5f,BlurMaskFilter.Blur.NORMAL)
        c.drawPath(p,paint)
        paint.maskFilter=null
    }

    private fun revealEnter(c:Canvas,d:RectF,amount:Float,paint:Paint=maskPaint){
        if(amount<=0f)return
        val a=amount.coerceIn(0f,1f)
        val y=d.top+d.height()*.835f
        val h=d.height()*.145f*a
        val p=Path()
        p.moveTo(d.left+d.width()*.19f,y)
        p.cubicTo(d.left+d.width()*.35f,y-d.height()*.012f,d.left+d.width()*.67f,y+d.height()*.008f,d.left+d.width()*.81f,y+d.height()*.01f)
        p.lineTo(d.left+d.width()*.79f,y+h)
        p.cubicTo(d.left+d.width()*.61f,y+h*1.03f,d.left+d.width()*.38f,y+h*.98f,d.left+d.width()*.20f,y+h*.90f)
        p.close()
        paint.maskFilter=BlurMaskFilter(6f,BlurMaskFilter.Blur.NORMAL)
        c.drawPath(p,paint)
        paint.maskFilter=null
    }

    private fun revealRegion(c:Canvas,d:RectF,amount:Float,points:Array<Pair<Float,Float>>,paint:Paint){
        if(amount<=0f)return
        val a=amount.coerceIn(0f,1f)
        val center=points.map{it.second}.average().toFloat()
        val p=Path()
        val n=(points.size*a).toInt().coerceAtLeast(1).coerceAtMost(points.size)
        for(i in 0 until n){
            val q=points[i]
            val x=d.left+d.width()*q.first
            val y=d.top+d.height()*q.second
            if(i==0)p.moveTo(x,y) else p.lineTo(x,y)
        }
        if(n>=2)p.close()
        paint.maskFilter=BlurMaskFilter(7f,BlurMaskFilter.Blur.NORMAL)
        c.drawPath(p,paint)
        paint.maskFilter=null
    }

    private fun organicEllipsePath(p:Path,cx:Float,cy:Float,w:Float,h:Float){
        val rx=w/2f; val ry=h/2f
        p.moveTo(cx-rx*.88f,cy-ry*.10f)
        p.cubicTo(cx-rx*.98f,cy-ry*.62f,cx-rx*.48f,cy-ry*1.02f,cx-rx*.04f,cy-ry*.90f)
        p.cubicTo(cx+rx*.42f,cy-ry*1.02f,cx+rx*.94f,cy-ry*.60f,cx+rx*.82f,cy-ry*.08f)
        p.cubicTo(cx+rx*1.00f,cy+ry*.36f,cx+rx*.48f,cy+ry*.96f,cx,cy+ry*.82f)
        p.cubicTo(cx-rx*.44f,cy+ry*1.02f,cx-rx*1.00f,cy+ry*.56f,cx-rx*.88f,cy-ry*.10f)
        p.close()
    }

    private fun drawMainMenu(c:Canvas,elapsed:Float){
        val d=fitRect(width.toFloat(),height.toFloat())
        c.drawBitmap(bitmap,null,d,imagePaint)
        val a=ease(elapsed.coerceIn(0f,1f))
        textPaint.color=Color.rgb(55,43,32); textPaint.alpha=(255*a).toInt()
        textPaint.textAlign=Paint.Align.CENTER; textPaint.typeface=Typeface.DEFAULT_BOLD
        textPaint.textSize=width*.043f
        c.drawText("EL MUNDO DE MATÍAS",width/2f,height*.735f,textPaint)
        textPaint.typeface=Typeface.DEFAULT; textPaint.textSize=width*.027f
        c.drawText("Elegí por dónde empezar",width/2f,height*.775f,textPaint)
        val labels=arrayOf("MORFI","¡EUREKA!","DETECTIVES","LABORATORIO")
        val xs=floatArrayOf(.27f,.27f,.73f,.73f); val ys=floatArrayOf(.81f,.855f,.81f,.855f)
        for(i in labels.indices){
            textPaint.color=Color.rgb(82,111,66); textPaint.alpha=(235*a).toInt()
            c.drawRoundRect(width*(xs[i]-.20f),height*(ys[i]-.025f),width*(xs[i]+.20f),height*(ys[i]+.018f),18f,18f,textPaint)
            textPaint.color=Color.WHITE; textPaint.textSize=width*.021f; textPaint.typeface=Typeface.DEFAULT_BOLD
            c.drawText(labels[i],width*xs[i],height*(ys[i]+.002f),textPaint)
        }
        textPaint.alpha=255
    }

    private fun fitRect(w:Float,h:Float):RectF{
        val s=min(w/bitmap.width,h/bitmap.height)
        val rw=bitmap.width*s; val rh=bitmap.height*s
        return RectF((w-rw)/2f,(h-rh)/2f,(w+rw)/2f,(h+rh)/2f)
    }

    private fun ease(x:Float):Float{
        val v=x.coerceIn(0f,1f); return v*v*(3f-2f*v)
    }

    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(e.action==MotionEvent.ACTION_UP && !entered){
            val t=(System.currentTimeMillis()-startedAt)/1000f
            if(t>=9.2f){
                entered=true
                enteredAt=System.currentTimeMillis()
                postInvalidateOnAnimation()
            }
        }
        return true
    }
}
