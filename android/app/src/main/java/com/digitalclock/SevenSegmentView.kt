package com.digitalclock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

/**
 * Minimal 7-segment digit renderer. Supports digits 0-9 and ':' separators.
 * Each segment is drawn as a chamfered hex bar, mimicking a classic LED/LCD look.
 */
class SevenSegmentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val path = Path()

    var text: String = ""
        set(value) {
            if (field != value) {
                field = value
                if (isInLayout || layoutParams != null) requestLayout()
                invalidate()
            }
        }

    var digitColor: Int = 0xFF00E53A.toInt()
        set(value) {
            if (field != value) { field = value; invalidate() }
        }

    /** Target height of a single digit, in pixels. */
    var digitHeight: Float = 200f
        set(value) {
            if (field != value) { field = value; requestLayout(); invalidate() }
        }

    private val thicknessRatio = 0.15f          // bar thickness vs digit height
    private val digitWidthRatio = 0.58f         // digit width vs digit height
    private val colonWidthRatio = 0.22f         // colon glyph width vs digit height
    private val charSpacingRatio = 0.08f        // gap between glyphs

    private fun glyphWidth(ch: Char): Float = when (ch) {
        ':' -> digitHeight * colonWidthRatio
        ' ' -> digitHeight * colonWidthRatio
        else -> digitHeight * digitWidthRatio
    }

    private fun totalWidth(): Float {
        if (text.isEmpty()) return 0f
        val spacing = digitHeight * charSpacingRatio
        var w = 0f
        text.forEachIndexed { i, c ->
            if (i > 0) w += spacing
            w += glyphWidth(c)
        }
        return w
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredW = (totalWidth() + paddingLeft + paddingRight).toInt()
        val desiredH = (digitHeight + paddingTop + paddingBottom).toInt()
        setMeasuredDimension(
            resolveSize(max(desiredW, suggestedMinimumWidth), widthMeasureSpec),
            resolveSize(max(desiredH, suggestedMinimumHeight), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (text.isEmpty()) return
        paint.color = digitColor
        val spacing = digitHeight * charSpacingRatio
        var x = paddingLeft.toFloat()
        val y = paddingTop.toFloat()
        text.forEachIndexed { i, c ->
            if (i > 0) x += spacing
            val w = glyphWidth(c)
            when (c) {
                ':' -> drawColon(canvas, x, y, w, digitHeight)
                in '0'..'9' -> drawDigit(canvas, c - '0', x, y, w, digitHeight)
                else -> { /* skip */ }
            }
            x += w
        }
    }

    private fun drawColon(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val t = digitHeight * thicknessRatio
        val cx = x + w / 2f
        // position the two dots roughly at the centres of the upper and lower halves
        val upperY = y + h * 0.33f
        val lowerY = y + h * 0.67f
        val r = t * 0.55f
        canvas.drawRoundRect(cx - r, upperY - r, cx + r, upperY + r, r * 0.4f, r * 0.4f, paint)
        canvas.drawRoundRect(cx - r, lowerY - r, cx + r, lowerY + r, r * 0.4f, r * 0.4f, paint)
    }

    /**
     * Segments indexed a..g:
     *   a = top
     *   b = top-right
     *   c = bottom-right
     *   d = bottom
     *   e = bottom-left
     *   f = top-left
     *   g = middle
     */
    private fun drawDigit(canvas: Canvas, digit: Int, x: Float, y: Float, w: Float, h: Float) {
        val mask = SEGMENT_MASKS[digit]
        val t = h * thicknessRatio
        val gap = t * 0.06f
        val midY = y + h / 2f
        // Horizontal segments (a, g, d)
        if (mask and A != 0) drawHBar(canvas, x + t / 2 + gap, y, x + w - t / 2 - gap, y + t)
        if (mask and G != 0) drawHBar(canvas, x + t / 2 + gap, midY - t / 2, x + w - t / 2 - gap, midY + t / 2)
        if (mask and D != 0) drawHBar(canvas, x + t / 2 + gap, y + h - t, x + w - t / 2 - gap, y + h)
        // Vertical segments (f, b, e, c)
        if (mask and F != 0) drawVBar(canvas, x, y + t / 2 + gap, x + t, midY - t / 2 - gap)
        if (mask and B != 0) drawVBar(canvas, x + w - t, y + t / 2 + gap, x + w, midY - t / 2 - gap)
        if (mask and E != 0) drawVBar(canvas, x, midY + t / 2 + gap, x + t, y + h - t / 2 - gap)
        if (mask and C != 0) drawVBar(canvas, x + w - t, midY + t / 2 + gap, x + w, y + h - t / 2 - gap)
    }

    private fun drawHBar(canvas: Canvas, x0: Float, y0: Float, x1: Float, y1: Float) {
        val t = y1 - y0
        val h = t / 2f
        path.rewind()
        path.moveTo(x0 + h, y0)
        path.lineTo(x1 - h, y0)
        path.lineTo(x1, y0 + h)
        path.lineTo(x1 - h, y1)
        path.lineTo(x0 + h, y1)
        path.lineTo(x0, y0 + h)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawVBar(canvas: Canvas, x0: Float, y0: Float, x1: Float, y1: Float) {
        val t = x1 - x0
        val h = t / 2f
        path.rewind()
        path.moveTo(x0, y0 + h)
        path.lineTo(x0 + h, y0)
        path.lineTo(x1, y0 + h)
        path.lineTo(x1, y1 - h)
        path.lineTo(x0 + h, y1)
        path.lineTo(x0, y1 - h)
        path.close()
        canvas.drawPath(path, paint)
    }

    companion object {
        private const val A = 1 shl 0
        private const val B = 1 shl 1
        private const val C = 1 shl 2
        private const val D = 1 shl 3
        private const val E = 1 shl 4
        private const val F = 1 shl 5
        private const val G = 1 shl 6

        private val SEGMENT_MASKS = intArrayOf(
            A or B or C or D or E or F,         // 0
            B or C,                             // 1
            A or B or D or E or G,              // 2
            A or B or C or D or G,              // 3
            B or C or F or G,                   // 4
            A or C or D or F or G,              // 5
            A or C or D or E or F or G,         // 6
            A or B or C,                        // 7
            A or B or C or D or E or F or G,    // 8
            A or B or C or D or F or G,         // 9
        )
    }
}
