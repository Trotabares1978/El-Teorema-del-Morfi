package com.trotabares1978.elteoremadelmorfi

import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Window

class MainActivity : Activity() {
    private var music: MediaPlayer? = null
    private var fadeAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        setContentView(MorfiImageIntroView(this))

        music = MediaPlayer.create(this, R.raw.el_teorema_del_morfi)?.apply {
            isLooping = true
            setVolume(0f, 0f)
            start()
            fadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 3500L
                startDelay = 800L
                addUpdateListener { music?.setVolume(it.animatedValue as Float, it.animatedValue as Float) }
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
        music?.release()
        music = null
        super.onDestroy()
    }
}
