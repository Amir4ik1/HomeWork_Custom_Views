package otus.homework.customview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import androidx.core.graphics.toColorInt

class PieChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class PieSlice(
        val category: String,
        val value: Float,
        val percent: Float,
        val startAngle: Float,
        val sweepAngle: Float,
        val color: Int
    )

    private var payloads: List<PayloadModel> = emptyList()
    private var pieSlices: List<PieSlice> = emptyList()
    private var totalAmount: Float = 0f

    private val colors = listOf(
        "#b19cd9".toColorInt(),
        "#ffb4a2".toColorInt(),
        "#9ad0f5".toColorInt(),
        "#b6e2d3".toColorInt(),
        "#fff7ae".toColorInt(),
        "#f2b5d4".toColorInt(),
        "#c4dbe0".toColorInt(),
        "#e6e6e6".toColorInt()
    )

    private val piePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 40f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#888888".toColorInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val rectF = RectF()
    private var ringWidth = 80f
    private var centerText = ""
    private var centerSubText = ""
    private var selectedCategory: String? = null

    private var onCategoryClickListener: ((String) -> Unit)? = null

    fun setPayloads(payloads: List<PayloadModel>) {
        this.payloads = payloads
        totalAmount = payloads.sumOf { it.amount }.toFloat()
        prepareSlices()
        invalidate()
    }

    fun setCenterText(text: String, subText: String = "") {
        centerText = text
        centerSubText = subText
        invalidate()
    }

    fun setOnCategoryClickListener(listener: (String) -> Unit) {
        onCategoryClickListener = listener
    }

    private fun prepareSlices() {
        val grouped = payloads.groupBy { it.category }
        val list = mutableListOf<PieSlice>()
        var startAngle = -90f
        var colorIdx = 0

        for ((category, items) in grouped) {
            val value = items.sumOf { it.amount }.toFloat()
            val percent = if (totalAmount == 0f) 0f else value / totalAmount
            val sweep = percent * 360f
            val color = colors[colorIdx % colors.size]
            list.add(PieSlice(category, value, percent, startAngle, sweep, color))
            startAngle += sweep
            colorIdx++
        }
        pieSlices = list
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (pieSlices.isEmpty()) return

        val minDim = width.coerceAtMost(height)
        val radius = minDim * 0.39f
        ringWidth = minDim * 0.18f
        piePaint.strokeWidth = ringWidth
        rectF.set(
            width / 2f - radius,
            height / 2f - radius,
            width / 2f + radius,
            height / 2f + radius
        )

        for (slice in pieSlices) {
            piePaint.color = slice.color
            piePaint.strokeWidth = if (slice.category == selectedCategory) ringWidth * 1.14f else ringWidth
            canvas.drawArc(rectF, slice.startAngle, slice.sweepAngle - 2, false, piePaint)
        }

        for (slice in pieSlices) {
            val angle = Math.toRadians((slice.startAngle + slice.sweepAngle / 2).toDouble())
            val textRadius = radius
            val labelX = width / 2f + textRadius * cos(angle).toFloat()
            val labelY = height / 2f + textRadius * sin(angle).toFloat() + labelPaint.textSize / 2.6f
            val percentLabel = "${(slice.percent * 100).roundToInt()}%"
            if (slice.sweepAngle >= 15f) {
                canvas.drawText(percentLabel, labelX, labelY, labelPaint)
            }
        }

        for (slice in pieSlices) {
            val angle = Math.toRadians((slice.startAngle + slice.sweepAngle).toDouble())
            val x0 = width / 2f + (radius - ringWidth / 2) * cos(angle).toFloat()
            val y0 = height / 2f + (radius - ringWidth / 2) * sin(angle).toFloat()
            val x1 = width / 2f + (radius + ringWidth / 2) * cos(angle).toFloat()
            val y1 = height / 2f + (radius + ringWidth / 2) * sin(angle).toFloat()
            canvas.drawLine(x0, y0, x1, y1, separatorPaint)
        }

        if (centerText.isNotBlank()) {
            textPaint.textSize = minDim * 0.12f
            textPaint.color = "#444444".toColorInt()
            canvas.drawText(centerText, width / 2f, height / 2f - 10f, textPaint)
        }
        if (centerSubText.isNotBlank()) {
            textPaint.textSize = minDim * 0.065f
            textPaint.color = "#BBBBBB".toColorInt()
            canvas.drawText(centerSubText, width / 2f, height / 2f + 40f, textPaint)
        }
        textPaint.color = "#888888".toColorInt()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val centerX = width / 2f
            val centerY = height / 2f
            val dx = event.x - centerX
            val dy = event.y - centerY
            val dist = sqrt(dx * dx + dy * dy)

            val minDim = width.coerceAtMost(height)
            val radius = minDim * 0.39f
            val innerRadius = radius - ringWidth / 2
            val outerRadius = radius + ringWidth / 2

            if (dist in innerRadius..outerRadius) {
                val touchAngle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360) % 360
                val slice = pieSlices.firstOrNull {
                    touchAngle in it.startAngle..(it.startAngle + it.sweepAngle)
                }
                slice?.let {
                    selectedCategory = it.category
                    onCategoryClickListener?.invoke(it.category)
                    invalidate()
                }
            }
        }
        return true
    }

    override fun onSaveInstanceState(): Parcelable {
        return Bundle().apply {
            putParcelable("super", super.onSaveInstanceState())
            putString("selectedCategory", selectedCategory)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        (state as? Bundle)?.let {
            selectedCategory = it.getString("selectedCategory", null)
            super.onRestoreInstanceState(it.getParcelable("super"))
        }
    }
}
