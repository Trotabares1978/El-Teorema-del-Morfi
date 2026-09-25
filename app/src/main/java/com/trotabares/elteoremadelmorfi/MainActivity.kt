package com.trotabares.elteoremadelmorfi

import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.Window
import kotlin.math.min

class MainActivity : Activity() {
    private var music: MediaPlayer? = null
    private var fadeAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        setContentView(MorfiIntroView(this))

        music = MediaPlayer.create(this, R.raw.el_teorema_del_morfi)?.apply {
            isLooping = true
            setVolume(0f, 0f)
            start()

            // El fade pertenece al ciclo de vida de la Activity: si la pantalla
            // muere, cancelamos el animator antes de liberar el MediaPlayer.
            fadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 3500L
                startDelay = 800L
                addUpdateListener { animator ->
                    val v = animator.animatedValue as Float
                    music?.setVolume(v, v)
                }
                start()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        music?.let { if (it.isPlaying) it.pause() }
    }

    override fun onResume() {
        super.onResume()
        music?.let { if (!it.isPlaying) it.start() }
    }

    override fun onDestroy() {
        fadeAnimator?.cancel()
        fadeAnimator = null

        music?.run {
            if (isPlaying) stop()
            release()
        }
        music = null

        super.onDestroy()
    }
}

private class MorfiIntroView(context: android.content.Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val started = System.currentTimeMillis()
    private var elapsed = 0f
    private var entered = false
    private var finalStartedAt = 0L

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        isClickable = true
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        val now = System.currentTimeMillis()
        elapsed = (now - started) / 1000f

        if (entered) {
            val finalA = ease((now - finalStartedAt) / 1100f).coerceIn(0f, 1f)
            c.drawColor(Color.rgb(248, 238, 218))
            drawFinalScene(c, w, h, finalA)
            if (finalA < 1f) postInvalidateOnAnimation()
            return
        }

        val world = ease(elapsed / 3.2f).coerceIn(0f, 1f)
        val road = ease((elapsed - 1.35f) / 1.75f).coerceIn(0f, 1f)
        val places = ease((elapsed - 3.0f) / 1.9f).coerceIn(0f, 1f)
        val matias = ease((elapsed - 4.75f) / 1.15f).coerceIn(0f, 1f)
        val title = ease((elapsed - 5.55f) / 0.85f).coerceIn(0f, 1f)
        val button = ease((elapsed - 6.35f) / 0.7f).coerceIn(0f, 1f)

        c.drawColor(Color.WHITE)
        drawWorld(c, w, h, world, road)
        drawPlaces(c, w, h, places)
        drawMatias(c, w, h, matias)
        drawTitle(c, w, h, title)
        drawButton(c, w, h, button)

        if (elapsed < 8.5f) postInvalidateOnAnimation()
    }

    private fun drawFinalScene(c: Canvas, w: Float, h: Float, a: Float) {
        p.alpha = (255 * a).toInt()

        // Pantalla principal definitiva: una ilustración navegable, no una interfaz genérica.
        p.shader = LinearGradient(0f, 0f, 0f, h,
            Color.rgb(244, 226, 191), Color.rgb(236, 242, 232), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null

        // Río y costa de Ensenada.
        p.color = Color.rgb(91, 151, 160)
        c.drawRect(0f, h*.43f, w, h, p)
        p.color = Color.rgb(111, 145, 91)
        path.reset()
        path.moveTo(0f,h*.41f); path.quadTo(w*.22f,h*.38f,w*.42f,h*.42f)
        path.quadTo(w*.68f,h*.39f,w,h*.42f); path.lineTo(w,h*.52f); path.lineTo(0f,h*.52f); path.close()
        c.drawPath(path,p)

        // Sol.
        p.color = Color.rgb(239, 178, 76)
        c.drawCircle(w*.79f,h*.18f,min(w,h)*.075f,p)

        // Astillero al fondo.
        p.color = Color.rgb(62,66,63)
        c.drawRect(w*.05f,h*.34f,w*.28f,h*.43f,p)
        p.strokeWidth=maxOf(2f,w*.005f)
        p.style=Paint.Style.STROKE
        c.drawLine(w*.08f,h*.34f,w*.18f,h*.22f,p)
        c.drawLine(w*.18f,h*.22f,w*.27f,h*.34f,p)
        c.drawLine(w*.18f,h*.22f,w*.18f,h*.39f,p)
        p.style=Paint.Style.FILL

        // Barco.
        p.color=Color.rgb(225,226,216)
        path.reset()
        path.moveTo(w*.30f,h*.405f); path.lineTo(w*.53f,h*.405f)
        path.lineTo(w*.49f,h*.45f); path.lineTo(w*.35f,h*.45f); path.close()
        c.drawPath(path,p)
        p.color=Color.rgb(52,60,62)
        c.drawRect(w*.36f,h*.36f,w*.46f,h*.405f,p)

        // Pizzería.
        p.color=Color.rgb(165,92,57)
        c.drawRect(w*.70f,h*.34f,w*.92f,h*.46f,p)
        p.color=Color.rgb(82,56,42)
        path.reset()
        path.moveTo(w*.67f,h*.34f); path.lineTo(w*.81f,h*.275f); path.lineTo(w*.95f,h*.34f); path.close()
        c.drawPath(path,p)
        p.color=Color.rgb(244,201,89)
        p.textAlign=Paint.Align.CENTER
        p.typeface=Typeface.DEFAULT_BOLD
        p.textSize=w*.026f
        c.drawText("PIZZERÍA",w*.81f,h*.37f,p)

        // Camino protagonista.
        p.color=Color.rgb(154,119,79)
        path.reset()
        path.moveTo(w*.46f,h*.44f); path.lineTo(w*.54f,h*.44f)
        path.lineTo(w*.88f,h); path.lineTo(w*.12f,h); path.close()
        c.drawPath(path,p)
        p.color=Color.rgb(226,197,126)
        p.style=Paint.Style.STROKE
        p.strokeWidth=maxOf(2f,w*.009f)
        path.reset()
        path.moveTo(w*.50f,h*.45f); path.lineTo(w*.51f,h*.54f); path.lineTo(w*.55f,h*.70f); path.lineTo(w*.60f,h)
        c.drawPath(path,p)
        p.style=Paint.Style.FILL

        // Matías, pequeño y reconocible, en primer plano.
        val my=h*.82f
        p.color=Color.rgb(39,55,70)
        c.drawRect(w*.455f,my,w*.495f,my+h*.16f,p)
        c.drawRect(w*.505f,my,w*.545f,my+h*.16f,p)
        p.color=Color.rgb(196,69,47)
        c.drawRoundRect(w*.425f,my-h*.15f,w*.575f,my+h*.02f,22f,22f,p)
        p.color=Color.rgb(224,164,116)
        c.drawCircle(w*.50f,my-h*.20f,w*.062f,p)
        p.color=Color.rgb(43,34,30)
        path.reset()
        path.moveTo(w*.44f,my-h*.205f)
        path.cubicTo(w*.44f,my-h*.30f,w*.55f,my-h*.31f,w*.57f,my-h*.22f)
        path.lineTo(w*.54f,my-h*.245f); path.lineTo(w*.51f,my-h*.22f)
        path.lineTo(w*.48f,my-h*.25f); path.lineTo(w*.45f,my-h*.22f); path.close()
        c.drawPath(path,p)
        p.color=Color.rgb(45,35,31)
        c.drawCircle(w*.478f,my-h*.20f,w*.005f,p)
        c.drawCircle(w*.522f,my-h*.20f,w*.005f,p)

        // Título, como portada.
        p.color=Color.rgb(54,43,32)
        p.textAlign=Paint.Align.CENTER
        p.typeface=Typeface.create("sans-serif-condensed",Typeface.BOLD)
        p.textSize=w*.065f
        c.drawText("EL TEOREMA",w*.50f,h*.075f,p)
        p.textSize=w*.074f
        c.drawText("DEL MORFI",w*.50f,h*.135f,p)
        p.typeface=Typeface.create("sans-serif",Typeface.NORMAL)
        p.textSize=w*.028f
        p.color=Color.rgb(92,72,54)
        c.drawText("Más que un libro · el mundo de Matías",w*.50f,h*.17f,p)

        // Paneles de navegación integrados en la escena.
        drawMenuCard(c,w,h,w*.035f,h*.53f,w*.285f,h*.635f,"🍕","MORFI",Color.rgb(180,82,48))
        drawMenuCard(c,w,h,w*.035f,h*.655f,w*.285f,h*.76f,"⚓","¡EUREKA!",Color.rgb(55,112,127))
        drawMenuCard(c,w,h,w*.715f,h*.53f,w*.965f,h*.635f,"🔎","DETECTIVES",Color.rgb(86,93,69))
        drawMenuCard(c,w,h,w*.715f,h*.655f,w*.965f,h*.76f,"🧪","LABORATORIO",Color.rgb(116,76,59))

        p.alpha=(210*a).toInt()
        p.color=Color.WHITE
        c.drawRoundRect(w*.31f,h*.785f,w*.69f,h*.855f,24f,24f,p)
        p.color=Color.rgb(57,53,47)
        p.textSize=w*.030f
        p.typeface=Typeface.DEFAULT_BOLD
        c.drawText("🦠  EL MUNDO INVISIBLE     🔢  NÚMEROS     🚢  BARCOS",
            w*.50f,h*.828f,p)

        p.color=Color.rgb(82,111,66)
        c.drawRoundRect(w*.34f,h*.885f,w*.66f,h*.95f,24f,24f,p)
        p.color=Color.WHITE
        p.textSize=w*.030f
        c.drawText("GALERÍA  ·  PERSONAJES  ·  MAPA",w*.50f,h*.925f,p)
        p.alpha=255
    }

    private fun drawMenuCard(c: Canvas,w:Float,h:Float,l:Float,t:Float,r:Float,b:Float,icon:String,label:String,fill:Int) {
        p.color=Color.WHITE
        c.drawRoundRect(l+3f,t+5f,r+3f,b+5f,18f,18f,p)
        p.color=fill
        c.drawRoundRect(l,t,r,b,18f,18f,p)
        p.color=Color.WHITE
        p.textAlign=Paint.Align.CENTER
        p.typeface=Typeface.DEFAULT_BOLD
        p.textSize=w*.048f
        c.drawText(icon,(l+r)/2f,t+(b-t)*.48f,p)
        p.textSize=w*.026f
        c.drawText(label,(l+r)/2f,b-(b-t)*.16f,p)
    }

    private fun drawSign(c: Canvas,x: Float,y: Float,label: String,a: Float) {
        p.alpha=(255*a).toInt()
        p.color=Color.rgb(103,70,45)
        c.drawRect(x,y,x+width*.12f,y+height*.055f,p)
        p.color=Color.WHITE
        p.textAlign=Paint.Align.CENTER
        p.typeface=Typeface.DEFAULT_BOLD
        p.textSize=width*.021f
        c.drawText(label,x+width*.06f,y+height*.036f,p)
        p.alpha=255
    }

    private fun drawWorld(c: Canvas, w: Float, h: Float, a: Float, road: Float) {
        if (a <= 0f) return

        p.shader = LinearGradient(0f, 0f, 0f, h * .55f,
            Color.rgb(214, 235, 239), Color.rgb(247, 214, 157), Shader.TileMode.CLAMP)
        p.alpha = (255 * a).toInt()
        c.drawRect(0f, 0f, w, h * .52f * a, p)
        p.shader = null

        p.color = Color.rgb(244, 173, 69)
        p.alpha = (255 * a).toInt()
        c.drawCircle(w * .72f, h * .235f, min(w, h) * .055f * a, p)

        p.color = Color.WHITE
        p.alpha = (90 * a).toInt()
        c.drawOval(w*.08f, h*.17f, w*.27f, h*.21f, p)
        c.drawOval(w*.18f, h*.145f, w*.38f, h*.205f, p)
        c.drawOval(w*.55f, h*.12f, w*.76f, h*.17f, p)

        p.alpha = (255 * a).toInt()
        p.color = Color.rgb(89, 157, 171)
        c.drawRect(0f, h*.45f, w, h, p)
        p.color = Color.rgb(111, 139, 83)
        c.drawRect(0f, h*.43f, w, h*.505f, p)

        p.color = Color.rgb(73, 103, 62)
        for (i in 0..16) {
            val x = w * (i / 16f)
            c.drawCircle(x, h*.455f, h*.018f + (i % 3) * h*.004f, p)
        }

        if (road > 0f) {
            val topY = h*.455f
            val bottomY = topY + (h*.545f) * road
            path.reset()
            path.moveTo(w*.495f, topY)
            path.lineTo(w*.515f, topY)
            path.lineTo(w*.94f, bottomY)
            path.lineTo(w*.05f, bottomY)
            path.close()

            p.color = Color.rgb(143, 112, 77)
            p.alpha = (255 * road).toInt()
            c.drawPath(path, p)

            p.color = Color.rgb(101, 78, 57)
            p.style = Paint.Style.STROKE
            p.strokeWidth = maxOf(2f, w*.008f)
            c.drawPath(path, p)
            p.style = Paint.Style.FILL

            p.color = Color.rgb(229, 201, 126)
            p.alpha = (230 * road).toInt()
            p.strokeWidth = maxOf(2f, w*.012f)
            p.style = Paint.Style.STROKE
            val cy1 = topY + (bottomY-topY)*.22f
            val cy2 = topY + (bottomY-topY)*.48f
            val cy3 = topY + (bottomY-topY)*.76f
            c.drawLine(w*.505f, topY+h*.012f, w*.52f, cy1, p)
            c.drawLine(w*.52f, cy1, w*.55f, cy2, p)
            c.drawLine(w*.55f, cy2, w*.58f, cy3, p)
            p.style = Paint.Style.FILL
        }

        if (road > .45f) {
            val dockA = ((road-.45f)/.55f).coerceIn(0f,1f)
            p.alpha = (255*dockA).toInt()
            p.color = Color.rgb(106, 72, 43)
            val y = h*.485f
            c.drawRect(w*.72f, y, w*.78f, y+h*.16f*dockA, p)
            p.color = Color.rgb(159, 112, 69)
            for (i in 0..4) {
                c.drawRect(w*.715f+i*w*.015f, y, w*.724f+i*w*.015f, y+h*.145f*dockA, p)
            }
            p.color = Color.rgb(86, 61, 40)
            p.strokeWidth = maxOf(2f, w*.006f)
            c.drawLine(w*.69f, y+h*.015f, w*.81f, y+h*.015f, p)
        }
        p.alpha = 255
    }

    private fun drawPlaces(c: Canvas, w: Float, h: Float, a: Float) {
        if (a <= 0f) return
        p.alpha = (255*a).toInt()

        p.color = Color.rgb(76, 78, 75)
        c.drawRect(w*.055f, h*.355f, w*.30f, h*.445f, p)
        p.color = Color.rgb(51, 53, 51)
        c.drawRect(w*.08f, h*.335f, w*.105f, h*.44f, p)
        p.strokeWidth = maxOf(2f, w*.006f)
        p.style = Paint.Style.STROKE
        c.drawLine(w*.09f,h*.335f,w*.19f,h*.275f,p)
        c.drawLine(w*.19f,h*.275f,w*.20f,h*.36f,p)
        c.drawLine(w*.18f,h*.28f,w*.28f,h*.34f,p)
        p.style = Paint.Style.FILL

        p.color = Color.rgb(55, 61, 62)
        path.reset()
        path.moveTo(w*.10f,h*.425f); path.lineTo(w*.335f,h*.425f)
        path.lineTo(w*.30f,h*.465f); path.lineTo(w*.145f,h*.465f); path.close()
        c.drawPath(path,p)
        p.color = Color.rgb(188, 190, 182)
        c.drawRect(w*.205f,h*.355f,w*.245f,h*.425f,p)

        p.color = Color.rgb(164, 91, 55)
        c.drawRect(w*.68f,h*.365f,w*.90f,h*.47f,p)
        p.color = Color.rgb(82, 57, 42)
        path.reset()
        path.moveTo(w*.655f,h*.365f); path.lineTo(w*.79f,h*.30f); path.lineTo(w*.925f,h*.365f); path.close()
        c.drawPath(path,p)
        p.color = Color.rgb(242, 198, 88)
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = w*.035f
        c.drawText("PIZZA",w*.79f,h*.405f,p)
        drawWindow(c,w*.71f,h*.42f,w*.76f,h*.46f)
        drawWindow(c,w*.81f,h*.42f,w*.86f,h*.46f)

        p.color = Color.rgb(226, 224, 213)
        c.drawRect(w*.39f,h*.375f,w*.54f,h*.465f,p)
        p.color = Color.rgb(178, 70, 68)
        c.drawRect(w*.445f,h*.395f,w*.475f,h*.445f,p)
        c.drawRect(w*.435f,h*.405f,w*.485f,h*.435f,p)
        p.color = Color.rgb(112, 104, 88)
        c.drawRect(w*.40f,h*.365f,w*.53f,h*.378f,p)

        p.color = Color.rgb(194, 158, 102)
        c.drawRect(w*.89f,h*.34f,w*.99f,h*.47f,p)
        p.color = Color.rgb(75, 72, 62)
        p.textSize = w*.025f
        c.drawText("ESCUELA",w*.94f,h*.36f,p)

        p.alpha = 255
    }

    private fun drawWindow(c: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        p.color = Color.rgb(92, 138, 149)
        c.drawRect(left, top, right, bottom, p)
    }

    private fun drawMatias(c: Canvas, w: Float, h: Float, a: Float) {
        if (a <= 0f) return
        val lift = (1f-a)*h*.12f
        val baseY = h*.895f-lift
        p.alpha = (255*a).toInt()

        p.color = Color.argb((75*a).toInt(),35,31,25)
        c.drawOval(w*.34f,baseY+h*.045f,w*.66f,baseY+h*.09f,p)

        p.color = Color.rgb(39, 55, 70)
        path.reset()
        path.moveTo(w*.45f,baseY-h*.03f); path.lineTo(w*.495f,baseY-h*.03f)
        path.lineTo(w*.53f,baseY+h*.075f); path.lineTo(w*.485f,baseY+h*.075f); path.close()
        c.drawPath(path,p)
        path.reset()
        path.moveTo(w*.505f,baseY-h*.03f); path.lineTo(w*.55f,baseY-h*.03f)
        path.lineTo(w*.59f,baseY+h*.075f); path.lineTo(w*.545f,baseY+h*.075f); path.close()
        c.drawPath(path,p)

        p.color = Color.rgb(48,45,41)
        c.drawOval(w*.47f,baseY+h*.06f,w*.535f,baseY+h*.09f,p)
        c.drawOval(w*.54f,baseY+h*.06f,w*.605f,baseY+h*.09f,p)

        p.color = Color.rgb(196, 69, 47)
        c.drawRoundRect(w*.42f,baseY-h*.16f,w*.58f,baseY+h*.015f,25f,25f,p)

        p.color = Color.rgb(224, 164, 116)
        c.drawRoundRect(w*.40f,baseY-h*.12f,w*.45f,baseY-h*.005f,18f,18f,p)
        c.save()
        c.rotate(-16f,w*.58f,baseY-h*.09f)
        c.drawRoundRect(w*.56f,baseY-h*.15f,w*.61f,baseY-h*.015f,18f,18f,p)
        c.restore()

        p.color = Color.rgb(224, 164, 116)
        c.drawRect(w*.475f,baseY-h*.205f,w*.525f,baseY-h*.15f,p)
        c.drawCircle(w*.50f,baseY-h*.245f,w*.065f,p)

        p.color = Color.rgb(43, 34, 30)
        path.reset()
        path.moveTo(w*.435f,baseY-h*.25f)
        path.cubicTo(w*.435f,baseY-h*.33f,w*.48f,baseY-h*.335f,w*.53f,baseY-h*.30f)
        path.cubicTo(w*.57f,baseY-h*.29f,w*.565f,baseY-h*.235f,w*.555f,baseY-h*.205f)
        path.lineTo(w*.54f,baseY-h*.255f)
        path.lineTo(w*.52f,baseY-h*.225f)
        path.lineTo(w*.495f,baseY-h*.27f)
        path.lineTo(w*.47f,baseY-h*.225f)
        path.lineTo(w*.445f,baseY-h*.255f)
        path.close()
        c.drawPath(path,p)

        p.color = Color.rgb(45,35,31)
        c.drawCircle(w*.477f,baseY-h*.245f,w*.006f,p)
        c.drawCircle(w*.523f,baseY-h*.245f,w*.006f,p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = maxOf(1.5f,w*.004f)
        path.reset()
        path.moveTo(w*.48f,baseY-h*.215f)
        path.quadTo(w*.50f,baseY-h*.198f,w*.525f,baseY-h*.215f)
        c.drawPath(path,p)
        p.style = Paint.Style.FILL

        p.color = Color.rgb(48, 67, 77)
        c.drawRoundRect(w*.39f,baseY-h*.145f,w*.445f,baseY-h*.015f,14f,14f,p)
        p.color = Color.rgb(224, 164, 116)
        p.strokeWidth = maxOf(2f,w*.005f)
        p.style = Paint.Style.STROKE
        c.drawLine(w*.44f,baseY-h*.14f,w*.455f,baseY-h*.06f,p)
        p.style = Paint.Style.FILL
        p.alpha = 255
    }

    private fun drawTitle(c: Canvas,w:Float,h:Float,a:Float) {
        if (a <= 0f) return
        p.alpha=(255*a).toInt()
        p.textAlign=Paint.Align.CENTER
        p.typeface=Typeface.create("sans-serif-condensed",Typeface.BOLD)
        p.color=Color.rgb(53,42,32)
        p.textSize=w*.073f
        c.drawText("EL TEOREMA DEL MORFI",w/2f,h*.075f,p)
        p.typeface=Typeface.create("sans-serif",Typeface.NORMAL)
        p.textSize=w*.038f
        p.color=Color.rgb(90,71,53)
        c.drawText("Más que un libro",w/2f,h*.115f,p)
        p.alpha=255
    }

    private fun drawButton(c:Canvas,w:Float,h:Float,a:Float) {
        if(a<=0f) return
        p.alpha=(255*a).toInt()
        p.color=Color.rgb(82,111,66)
        c.drawRoundRect(w*.31f,h*.905f,w*.69f,h*.965f,22f,22f,p)
        p.color=Color.WHITE
        p.textAlign=Paint.Align.CENTER
        p.typeface=Typeface.DEFAULT_BOLD
        p.textSize=w*.042f
        c.drawText("ENTRAR",w*.50f,h*.945f,p)
        p.alpha=255
    }

    private fun ease(x:Float):Float {
        val v=x.coerceIn(0f,1f)
        return v*v*(3f-2f*v)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && !entered && elapsed >= 6.2f) {
            if (event.x in width*.31f..width*.69f && event.y in height*.89f..height*.98f) {
                entered=true
                finalStartedAt=System.currentTimeMillis()
                postInvalidateOnAnimation()
            }
        }
        return true
    }
}
