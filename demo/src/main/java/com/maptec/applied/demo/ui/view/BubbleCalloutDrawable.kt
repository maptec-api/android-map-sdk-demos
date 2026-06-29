package com.maptec.applied.demo.ui.view

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import android.graphics.drawable.Drawable

/**
 * 气泡样式 Drawable：圆角矩形 + 底部居中三角形“尾巴”。
 * 支持动态设置：圆角、背景色、边框颜色与宽度。
 * 边框为内边框（绘制在形状内部，不向外扩展）。
 */
class BubbleCalloutDrawable : Drawable() {

    private val path = Path()
    private val rectF = RectF()
    private val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    /** 圆角半径（像素） */
    var cornerRadiusPx: Float = 12f
        set(value) {
            field = value.coerceAtLeast(0f)
            invalidateSelf()
        }

    /** 背景色 */
    var backgroundColor: Int = Color.parseColor("#424242")
        set(value) {
            field = value
            paintFill.color = value
            invalidateSelf()
        }

    /** 边框颜色；设为透明则可不显示边框 */
    var borderColor: Int = Color.parseColor("#FFFFFF")
        set(value) {
            field = value
            paintStroke.color = value
            invalidateSelf()
        }

    /** 边框宽度（像素）；0 表示无边框 */
    var borderWidthPx: Float = 1f
        set(value) {
            field = value.coerceAtLeast(0f)
            paintStroke.strokeWidth = value
            invalidateSelf()
        }

    /** 底部三角形高度（像素） */
    var tailHeightPx: Float = 10f
        set(value) {
            field = value.coerceAtLeast(0f)
            invalidateSelf()
        }

    /** 底部三角形底边一半宽度（像素） */
    var tailHalfWidthPx: Float = 8f
        set(value) {
            field = value.coerceAtLeast(0f)
            invalidateSelf()
        }

    init {
        paintFill.color = backgroundColor
        paintStroke.color = borderColor
        paintStroke.strokeWidth = borderWidthPx
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0) return

        val w = b.width().toFloat()
        val h = b.height().toFloat()
        val r = cornerRadiusPx.coerceIn(0f, minOf(w, h) / 2f)
        val tailH = tailHeightPx.coerceIn(0f, h * 0.5f)
        val maxTailW = (w / 2f - r - 2f).coerceAtLeast(0f)
        val tailW = tailHalfWidthPx.coerceIn(0f, maxTailW)
        val bodyBottom = h - tailH

        path.reset()
        // 顺时针描边：从顶部中点偏左 (r, 0) 开始
        path.moveTo(r, 0f)
        // 左上圆角：圆心 (r,r)，从 270° 扫到 0°
        rectF.set(0f, 0f, r * 2, r * 2)
//        path.arcTo(rectF, 270f, 90f)
        path.lineTo(w - r, 0f)
        // 右上圆角：圆心 (w-r,r)，从 270° 扫到 0°
        rectF.set(w - r * 2, 0f, w, r * 2)
        path.arcTo(rectF, 270f, 90f)
        path.lineTo(w, bodyBottom - r)
        // 右下圆角：圆心 (w-r, bodyBottom-r)，从 0° 扫到 90°
        rectF.set(w - r * 2, bodyBottom - r * 2, w, bodyBottom)
        path.arcTo(rectF, 0f, 90f)
        // 底部尾巴：从右下圆角结束点 (w-r, bodyBottom) 到三角形尖再到左侧
        path.lineTo(w / 2 + tailW, bodyBottom)
        path.lineTo(w / 2, h)
        path.lineTo(w / 2 - tailW, bodyBottom)
        path.lineTo(r, bodyBottom)
        // 左下圆角：圆心 (r, bodyBottom-r)，从 90° 扫到 180°
        rectF.set(0f, bodyBottom - r * 2, r * 2, bodyBottom)
        path.arcTo(rectF, 90f, 90f)
        path.lineTo(0f, r)
        // 回到起点：左上圆角剩余弧，从 180° 扫到 270°
        rectF.set(0f, 0f, r * 2, r * 2)
        path.arcTo(rectF, 180f, 90f)
        path.close()

        canvas.drawPath(path, paintFill)
        if (borderWidthPx > 0 && borderColor != Color.TRANSPARENT) {
            // 内边框：裁切到形状内部后画描边，描边宽度加倍使可见部分为 borderWidthPx
            canvas.save()
            canvas.clipPath(path, Region.Op.INTERSECT)
            val savedWidth = paintStroke.strokeWidth
            paintStroke.strokeWidth = borderWidthPx * 2f
            canvas.drawPath(path, paintStroke)
            paintStroke.strokeWidth = savedWidth
            canvas.restore()
        }
    }

    override fun setAlpha(alpha: Int) {
        paintFill.alpha = alpha
        paintStroke.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        paintFill.colorFilter = colorFilter
        paintStroke.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}