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
        drawStage(c, d, stageMasks[0], fade((t-.30f)/.65f))
        drawStage(c, d, stageMasks[1], fade((t-.95f)/.90f))
        drawStage(c, d, stageMasks[2], fade((t-1.80f)/.90f))
        drawStage(c, d, stageMasks[3], fade((t-2.70f)/1.10f))
        drawStage(c, d, stageMasks[4], fade((t-3.95f)/.70f))
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
        val fw=bitmap.width; val fh=bitmap.height
        val bw=backgroundBitmap.width; val bh=backgroundBitmap.height
        val sx=fw.toFloat()/bw; val sy=fh.toFloat()/bh
        val fp=IntArray(fw*fh); bitmap.getPixels(fp,0,fw,0,0,fw,fh)
        val regions=arrayOf(
            RectF(0f,.38f,1f,1f),
            RectF(0f,.28f,1f,.70f),
            RectF(.08f,.48f,.72f,1f),
            RectF(.05f,.04f,.95f,.275f),
            RectF(.18f,.82f,.82f,1f)
        )
        for(index in regions.indices){
            val px=IntArray(fw*fh); val r=regions[index]
            for(y in 0 until fh){
                val yn=y.toFloat()/fh
                if(yn<r.top || yn>r.bottom) continue
                for(x in 0 until fw){
                    val xn=x.toFloat()/fw
                    if(xn<r.left || xn>r.right) continue
                    val bx=(x/sx).toInt().coerceIn(0,bw-1)
                    val by=(y/sy).toInt().coerceIn(0,bh-1)
                    val f=fp[y*fw+x]; val b=backgroundBitmap.getPixel(bx,by)
                    val a=if(index==3){
                        val lum=.299f*Color.red(f)+.587f*Color.green(f)+.114f*Color.blue(f)
                        when { lum<=105f->255; lum<=165f->((165f-lum)*255f/60f).toInt().coerceIn(0,255); else->0 }
                    }else{
                        val dist=kotlin.math.abs(Color.red(f)-Color.red(b))+
                            kotlin.math.abs(Color.green(f)-Color.green(b))+
                            kotlin.math.abs(Color.blue(f)-Color.blue(b))
                        when { dist>=120->255; dist>=75->((dist-75)*255/45).coerceIn(0,255); else->0 }
                    }
                    px[y*fw+x]=Color.argb(a,255,255,255)
                }
            }
            stageMasks[index]=Bitmap.createBitmap(fw,fh,Bitmap.Config.ARGB_8888).also{it.setPixels(px,0,fw,0,0,fw,fh)}
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
            if(t>=3.95f && b.contains(e.x,e.y)){
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
