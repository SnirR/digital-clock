package com.digitalclock

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Calendar
import java.util.Locale

/**
 * Digital clock: large HH:MM + smaller SS to the right, green 7-segment over full-black screen.
 * Keeps the screen on, hides system bars (immersive), and supports rotation without state loss.
 */
class MainActivity : ComponentActivity() {

    private lateinit var primaryView: SevenSegmentView
    private lateinit var secondsView: SevenSegmentView
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
        val root = FrameLayout(this).apply {
            setBackgroundColor(0xFF000000.toInt())
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            // Force LTR so the smaller seconds view always sits to the RIGHT of HH:MM,
            // regardless of the device's system language (Hebrew/Arabic would otherwise flip it).
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            layoutParams = FrameLayout.LayoutParams(
                WRAP_CONTENT,
                WRAP_CONTENT,
                Gravity.CENTER,
            )
        }

        primaryView = SevenSegmentView(this).apply {
            text = "00:00"
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        secondsView = SevenSegmentView(this).apply {
            text = "00"
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                // Align the smaller seconds glyphs with the bottom of the primary HH:MM glyphs.
                gravity = Gravity.BOTTOM
                leftMargin = (8 * resources.displayMetrics.density).toInt()
            }
        }

        row.addView(primaryView)
        row.addView(secondsView)
        root.addView(row)

        root.viewTreeObserver.addOnGlobalLayoutListener(object :
            android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                sizeGlyphs(root.width, root.height)
            }
        })

        return root
    }

    private fun sizeGlyphs(w: Int, h: Int) {
        if (w == 0 || h == 0) return
        // Pick the primary digit height so the HH:MM + SS row fits within ~88% of the screen width.
        // HH:MM width ≈ 3.02 * h (4 digits @0.58h + colon 0.22h + 4 gaps 0.12h)
        // SS width at secondary ratio 0.6 ≈ 0.768 * h (2 digits + 1 gap, all scaled by 0.6)
        val dp = resources.displayMetrics.density
        val targetWidth = w * 0.88f
        val primaryByWidth = (targetWidth - 8 * dp) / (3.02f + 0.768f)
        val primaryByHeight = h * 0.34f
        val primary = minOf(primaryByWidth, primaryByHeight).coerceAtLeast(32f)
        primaryView.digitHeight = primary
        secondsView.digitHeight = primary * 0.6f
    }

    private fun applyImmersiveMode() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Let the layout settle, then re-size glyphs for the new orientation.
        window.decorView.post {
            sizeGlyphs(window.decorView.width, window.decorView.height)
        }
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
        // HH:MM with no leading zero on the hour, to match the reference image.
        primaryView.text = String.format(Locale.US, "%d:%02d", h, m)
        secondsView.text = String.format(Locale.US, "%02d", s)
    }

    companion object {
        private const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
        private const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
