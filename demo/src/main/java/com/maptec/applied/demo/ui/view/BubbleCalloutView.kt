package com.maptec.applied.demo.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.maptec.applied.demo.R

/**
 * 气泡样式的 MarkerView 内容：圆角矩形 + 底部三角尾巴，两行文字。
 * 支持动态：圆角、两行文案、最大宽度、背景色、边框（颜色/宽度）。
 */
class BubbleCalloutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val drawable = BubbleCalloutDrawable()
    private val line1: TextView
    private val line2: TextView

    private var _maxWidthPx: Int = 0

    init {
        LayoutInflater.from(context).inflate(R.layout.marker_bubble_callout, this, true)
        line1 = findViewById(R.id.marker_bubble_line1)
        line2 = findViewById(R.id.marker_bubble_line2)
        background = drawable
        elevation = 8f
    }

    /** 第一行文案（如 "24 min"） */
    var line1Text: CharSequence
        get() = line1.text
        set(value) {
            line1.text = value
        }

    /** 第二行文案（如 "7.9 km"） */
    var line2Text: CharSequence
        get() = line2.text
        set(value) {
            line2.text = value
        }

    /** 圆角半径（像素） */
    var cornerRadiusPx: Float
        get() = drawable.cornerRadiusPx
        set(value) {
            drawable.cornerRadiusPx = value
            requestLayout()
        }

    /** 最大宽度（像素）；0 表示不限制 */
    var maxWidthPx: Int
        get() = _maxWidthPx
        set(value) {
            if (_maxWidthPx != value) {
                _maxWidthPx = value
                requestLayout()
            }
        }

    /** 是否固定宽度（强制使用 maxWidthPx 作为宽度） */
    var isFixedWidth: Boolean = true
        set(value) {
            field = value
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxW = _maxWidthPx
        val widthSpec = if (maxW > 0) {
            if (isFixedWidth) {
                MeasureSpec.makeMeasureSpec(maxW, MeasureSpec.EXACTLY)
            } else {
                val mode = MeasureSpec.getMode(widthMeasureSpec)
                val size = MeasureSpec.getSize(widthMeasureSpec)
                when {
                    mode == View.MeasureSpec.UNSPECIFIED || size <= 0 ->
                        MeasureSpec.makeMeasureSpec(maxW, View.MeasureSpec.AT_MOST)
                    else -> {
                        val capped = size.coerceAtMost(maxW)
                        MeasureSpec.makeMeasureSpec(capped, mode)
                    }
                }
            }
        } else {
            widthMeasureSpec
        }
        super.onMeasure(widthSpec, heightMeasureSpec)
    }
    /** 背景色 */
    var bubbleBackgroundColor: Int
        get() = drawable.backgroundColor
        set(value) {
            drawable.backgroundColor = value
            requestLayout()
        }

    /** 边框颜色 */
    var borderColor: Int
        get() = drawable.borderColor
        set(value) {
            drawable.borderColor = value
            requestLayout()
        }

    /** 边框宽度（像素） */
    var borderWidthPx: Float
        get() = drawable.borderWidthPx
        set(value) {
            drawable.borderWidthPx = value
            requestLayout()
        }

    fun setRichText(html: String) {
        line1.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        line1.maxLines = Int.MAX_VALUE
        line1.isSingleLine = false
        line2.visibility = View.GONE
        requestLayout()
    }
}
