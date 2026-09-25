package com.trotabares.elteoremadelmorfi

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

class MorfiImageIntroView(context: Context) : View(context) {
    private val bitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.morfi_intro)
        ?: error("No se pudo cargar la ilustración real")
    private val started = System.currentTimeMillis()
    private var entered = false
    private var enteredAt = 0L
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    override fun onDraw(c: Canvas) {
        val w=width.toFloat(); val h=height.toFloat(); val now=System.currentTimeMillis()
        val t=(now-started)/1000f
        c.drawColor(Color.WHITE)
        if(!entered){
            val a=ease(((t-.25f)/5f).coerceIn(0f,1f))
            val d=cover(w,h)
            c.save(); c.clipRect(0f,0f,w,d.bottom*a); c.drawBitmap(bitmap,null,d,paint); c.restore()
            if(t>5.25f){
                val q=ease((t-5.25f)/.8f)
                paint.color=Color.argb((245*q).toInt(),255,255,255); c.drawRect(0f,h*.86f,w,h,paint)
                paint.color=Color.argb((255*q).toInt(),55,43,32); paint.textAlign=Paint.Align.CENTER
                paint.typeface=Typeface.DEFAULT_BOLD; paint.textSize=w*.052f
                c.drawText("EL TEOREMA DEL MORFI",w/2,h*.91f,paint)
                paint.typeface=Typeface.DEFAULT; paint.textSize=w*.027f
                c.drawText("Más que un libro",w/2,h*.945f,paint)
            }
            if(t<6.2f) postInvalidateOnAnimation()
            return
        }

        val a=ease(((now-enteredAt)/900f).coerceIn(0f,1f))
        val d=cover(w*1.16f,h*1.16f)
        c.save(); c.translate((w-d.width())/2f,-h*.07f)
        c.drawBitmap(bitmap,null,RectF(0f,0f,d.width(),d.height()),paint); c.restore()

        paint.color=Color.argb((215*a).toInt(),255,255,255)
        c.drawRoundRect(w*.06f,h*.70f,w*.94f,h*.92f,28f,28f,paint)
        paint.color=Color.argb((255*a).toInt(),55,43,32)
        paint.textAlign=Paint.Align.CENTER; paint.typeface=Typeface.DEFAULT_BOLD
        paint.textSize=w*.043f; c.drawText("EL MUNDO DE MATÍAS",w/2,h*.75f,paint)
        paint.typeface=Typeface.DEFAULT; paint.textSize=w*.027f
        c.drawText("Elegí por dónde empezar",w/2,h*.785f,paint)

        val labels=arrayOf("MORFI","¡EUREKA!","DETECTIVES","LABORATORIO")
        val xs=floatArrayOf(.27f,.27f,.73f,.73f); val ys=floatArrayOf(.81f,.855f,.81f,.855f)
        for(i in labels.indices){
            paint.color=Color.argb((235*a).toInt(),82,111,66)
            c.drawRoundRect(w*(xs[i]-.20f),h*(ys[i]-.025f),w*(xs[i]+.20f),h*(ys[i]+.018f),18f,18f,paint)
            paint.color=Color.WHITE; paint.textSize=w*.021f; paint.typeface=Typeface.DEFAULT_BOLD
            c.drawText(labels[i],w*xs[i],h*(ys[i]+.002f),paint)
        }
        if(a<1f) postInvalidateOnAnimation()
    }

    private fun cover(w:Float,h:Float):RectF{
        val s=maxOf(w/bitmap.width,h/bitmap.height)
        val rw=bitmap.width*s; val rh=bitmap.height*s
        return RectF((w-rw)/2f,(h-rh)/2f,(w+rw)/2f,(h+rh)/2f)
    }
    private fun ease(x:Float):Float{val v=x.coerceIn(0f,1f);return v*v*(3f-2f*v)}
    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(e.action==MotionEvent.ACTION_UP&&!entered&&(System.currentTimeMillis()-started)/1000f>=5.8f){
            entered=true; enteredAt=System.currentTimeMillis(); postInvalidateOnAnimation()
        }
        return true
    }
}
