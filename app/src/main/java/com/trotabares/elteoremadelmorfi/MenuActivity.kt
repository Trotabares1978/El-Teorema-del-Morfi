package com.trotabares1978.elteoremadelmorfi

import android.app.Activity
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View

class MenuActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
        setContentView(MainMenuView())
    }

    private inner class MainMenuView : View(this@MenuActivity) {
        private val bitmap = BitmapFactory.decodeResource(resources, R.drawable.morfi_intro)
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private var selected = -1

        private val labels = arrayOf("MORFI", "¡EUREKA!", "DETECTIVES", "LABORATORIO")
        private val xs = floatArrayOf(.27f, .27f, .73f, .73f)
        private val ys = floatArrayOf(.81f, .885f, .73f, .805f)

        override fun onDraw(c: Canvas) {
            super.onDraw(c)
            c.drawColor(Color.WHITE)
            val d = fitRect(width.toFloat(), height.toFloat())
            c.drawBitmap(bitmap, null, d, paint)

            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.color = Color.rgb(55, 43, 32)
            paint.textSize = width * .043f
            c.drawText("EL MUNDO DE MATÍAS", width / 2f, height * .635f, paint)

            paint.typeface = Typeface.DEFAULT
            paint.textSize = width * .027f
            c.drawText("Elegí por dónde empezar", width / 2f, height * .680f, paint)

            for (i in labels.indices) {
                val cx = width * xs[i]
                val cy = height * ys[i]
                paint.color = if (selected == i) Color.rgb(63, 91, 48) else Color.rgb(82, 111, 66)
                c.drawRoundRect(cx - width*.19f, cy - height*.028f, cx + width*.19f, cy + height*.028f, 24f, 24f, paint)
                paint.color = Color.WHITE
                paint.textSize = width * .021f
                paint.typeface = Typeface.DEFAULT_BOLD
                c.drawText(labels[i], cx, cy + height*.007f, paint)
            }

            paint.typeface = Typeface.DEFAULT
            paint.textSize = width * .018f
            paint.color = Color.rgb(105, 91, 75)
            c.drawText("Cada opción abrirá su propia experiencia.", width / 2f, height * .935f, paint)
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            if (e.action == MotionEvent.ACTION_UP) {
                for (i in labels.indices) {
                    val cx = width * xs[i]
                    val cy = height * ys[i]
                    val r = RectF(cx - width*.20f, cy - height*.045f, cx + width*.20f, cy + height*.045f)
                    if (r.contains(e.x, e.y)) {
                        selected = i
                        invalidate()
                        break
                    }
                }
            }
            return true
        }

        private fun fitRect(w: Float, h: Float): RectF {
            val s = maxOf(w / bitmap.width, h / bitmap.height)
            val rw = bitmap.width * s
            val rh = bitmap.height * s
            return RectF((w-rw)/2f, (h-rh)/2f, (w+rw)/2f, (h+rh)/2f)
        }
    }
}
