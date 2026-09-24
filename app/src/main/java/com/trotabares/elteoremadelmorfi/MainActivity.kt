package com.trotabares.elteoremadelmorfi

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
import kotlin.math.min

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        setContentView(MorfiIntroView(this))
    }
}

private class MorfiIntroView(context: android.content.Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val start = System.currentTimeMillis()
    private var progress = 0f

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        isClickable = true
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        val t = (System.currentTimeMillis() - start) / 1000f

        // 0-2.8: world is drawn; 2.8-7.2: places appear; 7.2+: Matías/title/button.
        val world = ease(t / 2.8f).coerceIn(0f, 1f)
        val places = ease((t - 2.8f) / 2.0f).coerceIn(0f, 1f)
        val matias = ease((t - 4.8f) / 1.2f).coerceIn(0f, 1f)
        val title = ease((t - 5.6f) / 0.9f).coerceIn(0f, 1f)
        val button = ease((t - 6.5f) / 0.8f).coerceIn(0f, 1f)

        c.drawColor(Color.WHITE)

        // Sky, sun and river "draw themselves" vertically.
        val skyBottom = h * (0.48f * world)
        if (skyBottom > 0f) {
            paint.color = Color.rgb(244, 205, 143)
            c.drawRect(0f, 0f, w, skyBottom, paint)
        }

        if (world > 0f) {
            paint.color = Color.rgb(245, 177, 76)
            c.drawCircle(w * .72f, h * .23f, min(w, h) * .055f * world, paint)
        }

        if (world > .12f) {
            val riverTop = h * .48f
            paint.color = Color.rgb(105, 170, 183)
            c.drawRect(0f, riverTop, w, h, paint)
        }

        // Land strips.
        if (world > .35f) {
            paint.color = Color.rgb(126, 154, 91)
            c.drawRect(0f, h*.43f, w, h*.53f, paint)
            paint.color = Color.rgb(164, 142, 96)
            c.drawRect(0f, h*.50f, w, h, paint)
        }

        // Road grows from horizon toward viewer.
        if (world > .5f) {
            paint.color = Color.rgb(117, 103, 86)
            val p = Path()
            val topY = h*.47f
            val bottomY = h
            val current = topY + (bottomY-topY)*world
            p.moveTo(w*.49f, topY)
            p.lineTo(w*.54f, topY)
            p.lineTo(w*.92f, current)
            p.lineTo(w*.06f, current)
            p.close()
            c.drawPath(p, paint)
        }

        // Wooden pier at the water.
        if (world > .72f) {
            paint.color = Color.rgb(105, 72, 43)
            val y = h*.48f
            c.drawRect(w*.66f, y, w*.74f, y+h*.10f, paint)
            for (i in 0..4) c.drawRect(w*.655f+i*w*.018f, y, w*.665f+i*w*.018f, y+h*.10f, paint)
        }

        // Places appear, rather than being drawn.
        drawPlaces(c, w, h, places)

        if (matias > 0f) drawMatias(c, w, h, matias)

        if (title > 0f) {
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            paint.color = Color.rgb(54, 43, 31)
            paint.textSize = w*.075f
            paint.alpha = (255*title).toInt()
            c.drawText("EL TEOREMA DEL MORFI", w/2f, h*.09f, paint)
            paint.textSize = w*.038f
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            c.drawText("Más que un libro", w/2f, h*.125f, paint)
            paint.alpha = 255
        }

        if (button > 0f) {
            paint.alpha = (255*button).toInt()
            paint.color = Color.rgb(101, 72, 43)
            val l=w*.33f; val r=w*.67f; val top=h*.91f; val bot=h*.975f
            c.drawRoundRect(l,top,r,bot,18f,18f,paint)
            paint.color=Color.WHITE
            paint.textSize=w*.042f
            paint.typeface=Typeface.DEFAULT_BOLD
            c.drawText("ENTRAR",w/2f,top+(bot-top)*.68f,paint)
            paint.alpha=255
        }

        progress = t
        if (t < 8.5f) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawPlaces(c: Canvas, w: Float, h: Float, a: Float) {
        if (a <= 0f) return
        fun fade() { paint.alpha=(255*a).toInt() }
        // Astillero + ship
        fade(); paint.color=Color.rgb(72,72,68)
        c.drawRect(w*.08f,h*.39f,w*.30f,h*.49f,paint)
        paint.color=Color.rgb(201,201,193)
        c.drawRect(w*.19f,h*.32f,w*.25f,h*.39f,paint)
        paint.color=Color.rgb(52,52,50)
        val ship=Path(); ship.moveTo(w*.13f,h*.45f); ship.lineTo(w*.35f,h*.45f); ship.lineTo(w*.31f,h*.48f); ship.lineTo(w*.16f,h*.48f); ship.close(); c.drawPath(ship,paint)
        // Pizzeria
        paint.color=Color.rgb(168,91,54)
        c.drawRect(w*.67f,h*.39f,w*.84f,h*.49f,paint)
        paint.color=Color.rgb(89,57,40)
        val roof=Path(); roof.moveTo(w*.64f,h*.39f); roof.lineTo(w*.755f,h*.33f); roof.lineTo(w*.87f,h*.39f); roof.close(); c.drawPath(roof,paint)
        // Clinic
        paint.color=Color.rgb(221,221,210)
        c.drawRect(w*.42f,h*.40f,w*.53f,h*.48f,paint)
        paint.color=Color.rgb(172,65,65)
        c.drawRect(w*.46f,h*.415f,w*.49f,h*.46f,paint)
        c.drawRect(w*.445f,h*.43f,w*.505f,h*.445f,paint)
        // School
        paint.color=Color.rgb(195,157,101)
        c.drawRect(w*.86f,h*.40f,w*.98f,h*.49f,paint)
        paint.alpha=255
    }

    private fun drawMatias(c: Canvas, w: Float, h: Float, a: Float) {
        val base=h*.90f
        val y=base-(1f-a)*h*.12f
        paint.alpha=(255*a).toInt()
        paint.color=Color.rgb(240,190,145)
        c.drawCircle(w*.50f,y-h*.19f,w*.055f,paint)
        paint.color=Color.rgb(205,74,48)
        c.drawRoundRect(w*.43f,y-h*.14f,w*.57f,y+h*.015f,28f,28f,paint)
        paint.color=Color.rgb(45,67,87)
        c.drawRect(w*.445f,y,w*.555f,y+h*.07f,paint)
        paint.alpha=255
    }

    private fun ease(x: Float): Float {
        val v=x.coerceIn(0f,1f)
        return v*v*(3f-2f*v)
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_UP && progress >= 7f) {
            // Placeholder: next screen will be connected after the intro is validated.
            return true
        }
        return true
    }
}
