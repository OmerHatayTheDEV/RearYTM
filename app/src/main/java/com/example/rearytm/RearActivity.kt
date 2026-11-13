package com.example.rearytm

import android.app.Activity
import android.graphics.Color
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class RearActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var title: TextView
    private lateinit var artist: TextView
    private lateinit var bar: ProgressBar
    private lateinit var play: Button
    private var idx = 0
    private fun cur() = Media.sessions(this).let { if (it.isEmpty()) null else it[idx % it.size] }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        title = TextView(this).apply { textSize = 18f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; maxLines = 2; setOnClickListener { idx++; refresh() } }
        artist = TextView(this).apply { textSize = 14f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER; maxLines = 1 }
        bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 1000 }
        fun b(t: String, a: () -> Unit) = Button(this).apply {
            text = t; textSize = 22f; setOnClickListener { a(); refresh() }
        }
        play = b("▶") { cur()?.let {
            if (it.playbackState?.state == PlaybackState.STATE_PLAYING) it.transportControls.pause() else it.transportControls.play() } }
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            addView(b("⏮") { cur()?.transportControls?.skipToPrevious() })
            addView(play)
            addView(b("⏭") { cur()?.transportControls?.skipToNext() })
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK); setPadding(24, 24, 24, 24)
            addView(title); addView(artist); addView(bar); addView(row)
        })
    }

    private val tick = object : Runnable { override fun run() { refresh(); h.postDelayed(this, 500) } }
    override fun onResume() { super.onResume(); tick.run() }
    override fun onPause() { super.onPause(); h.removeCallbacks(tick) }

    private fun refresh() {
        val c = cur()
        if (c == null) { title.text = "Seçili uygulamalarda bir şey çal\n(veya yetki/uygulama seç)"; artist.text = ""; return }
        val m = c.metadata; val s = c.playbackState
        title.text = m?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        artist.text = m?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val dur = m?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        if (dur > 0 && s != null) bar.progress = (s.position * 1000 / dur).toInt().coerceIn(0, 1000)
        play.text = if (s?.state == PlaybackState.STATE_PLAYING) "⏸" else "▶"
    }
}
