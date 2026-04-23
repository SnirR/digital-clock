package com.digitalclock

import android.content.res.Configuration
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Calendar
import java.util.Locale

/**
 * Digital clock: large HH:MM + smaller SS to the right, green DSEG7 LCD font over
 * a full-black screen. Keeps the screen on, hides system bars (immersive), and
 * supports rotation without state loss.
 */
class MainActivity : ComponentActivity() {

    private lateinit var rootView: FrameLayout
    private lateinit var primaryView: TextView
    private lateinit var secondsView: TextView
    private val measurePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            updateTime()
            // Align the next tick to the next wall-clock second boundary to avoid drift.
            val now = System.currentTimeMillis()
            val delay = 1000L - (now % 1000L)
            handler.postDelayed(this, delay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw behind system bars and hide them for a true full-screen look.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(buildLayout())
        applyImmersiveMode()
    }

    private fun buildLayout(): View {
        val dsegFont = ResourcesCompat.getFont(this, R.font.dseg7_bold)
        measurePaint.typeface = dsegFont

        rootView = FrameLayout(this).apply {
            setBackgroundColor(0xFF000000.toInt())
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            // Bottom-align so the smaller seconds glyphs sit on the same baseline as HH:MM.
            gravity = Gravity.BOTTOM
            // Force LTR so the smaller seconds view always sits to the RIGHT of HH:MM,
            // regardless of the device's system language (Hebrew/Arabic would otherwise flip it).
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            layoutParams = FrameLayout.LayoutParams(
                WRAP_CONTENT,
                WRAP_CONTENT,
                Gravity.CENTER,
            )
        }

        primaryView = TextView(this).apply {
            text = "00:00"
            typeface = dsegFont
            setTextColor(0xFF00E53A.toInt())
            includeFontPadding = false
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        secondsView = TextView(this).apply {
            text = "00"
            typeface = dsegFont
            setTextColor(0xFF00E53A.toInt())
            includeFontPadding = false
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                leftMargin = (8 * resources.displayMetrics.density).toInt()
            }
        }

        row.addView(primaryView)
        row.addView(secondsView)
        rootView.addView(row)

        // Re-size glyphs whenever the root view dimensions change (initial layout + rotation).
        rootView.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val w = right - left
            val h = bottom - top
            if (w > 0 && h > 0 && (w != oldRight - oldLeft || h != oldBottom - oldTop)) {
                handler.post { sizeGlyphs(w, h) }
            }
        }

        return rootView
    }

    private fun sizeGlyphs(w: Int, h: Int) {
        if (w == 0 || h == 0) return
        val dp = resources.displayMetrics.density

        // Measure primary + seconds text widths at a reference text size so we can scale
        // them to fill ~92% of the screen width. DSEG7 is monospaced so "00:00" is the
        // widest HH:MM we render.
        measurePaint.textSize = 100f
        val primaryWidthAtRef = measurePaint.measureText("00:00")
        val secondsWidthAtRef = measurePaint.measureText("00")
        val primaryRatio = primaryWidthAtRef / 100f
        val secondsRatio = secondsWidthAtRef / 100f

        val margin = 8 * dp
        val targetWidth = w * 0.92f - margin
        val primarySizeByWidth = targetWidth / (primaryRatio + SECONDS_SCALE * secondsRatio)
        // DSEG7 digits fill most of the em box; cap at 70% of screen height.
        val primarySizeByHeight = h * 0.70f
        val primarySize = minOf(primarySizeByWidth, primarySizeByHeight)
            .coerceAtLeast(16f * dp)

        primaryView.setTextSize(TypedValue.COMPLEX_UNIT_PX, primarySize)
        secondsView.setTextSize(TypedValue.COMPLEX_UNIT_PX, primarySize * SECONDS_SCALE)
    }

    private fun applyImmersiveMode() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Layout change listener on rootView handles re-sizing automatically.
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveMode()
    }

    override fun onResume() {
        super.onResume()
        handler.post(tick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    private fun updateTime() {
        val c = Calendar.getInstance()
        val h = c.get(Calendar.HOUR_OF_DAY)
        val m = c.get(Calendar.MINUTE)
        val s = c.get(Calendar.SECOND)
        primaryView.text = String.format(Locale.US, "%d:%02d", h, m)
        secondsView.text = String.format(Locale.US, "%02d", s)
    }

    companion object {
        private const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
        private const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
        private const val SECONDS_SCALE = 0.6f
    }
}
