package otus.homework.customview

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import kotlin.math.*
import androidx.core.graphics.toColorInt

class LineChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Point(val day: Int, val sum: Float)
    data class Series(val category: String, val color: Int, val points: List<Point>)

    private var seriesList: List<Series> = emptyList()
    private var minDay = 0
    private var maxDay = 0
    private var maxSum = 1f

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#bbbbbb".toColorInt()
        strokeWidth = 3f
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#eeeeee".toColorInt()
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        strokeWidth = 7f
        style = Paint.Style.STROKE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#10FF6C4A".toColorInt()
        style = Paint.Style.FILL
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#888888".toColorInt()
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }
    private val yLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#bbbbbb".toColorInt()
        textSize = 24f
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.DEFAULT
    }

    private var chartRect = RectF()
    private var seriesPaths: List<Pair<Path, Path>> = emptyList()
    private var pointsScreen: List<List<Pair<Float, Float>>> = emptyList()
    private var needsUpdate = false

    fun setData(series: List<Series>) {
        if (series.isEmpty()) return
        minDay = series.flatMap { it.points }.minOf { it.day }
        maxDay = series.flatMap { it.points }.maxOf { it.day }
        maxSum = series.flatMap { it.points }.maxOf { it.sum }.takeIf { it > 0 } ?: 1f

        val daysFull = (minDay..maxDay).toList()
        val result = series.map {
            val map = it.points.associateBy { pt -> pt.day }
            val filled = daysFull.map { day -> Point(day, map[day]?.sum ?: 0f) }
            it.copy(points = filled)
        }
        seriesList = result
        needsUpdate = true
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val wrapWidth = (resources.displayMetrics.density * 360).toInt()
        val wrapHeight = (resources.displayMetrics.density * 200).toInt()

        val width = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
            MeasureSpec.AT_MOST -> min(wrapWidth, MeasureSpec.getSize(widthMeasureSpec))
            else -> wrapWidth
        }
        val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> min(wrapHeight, MeasureSpec.getSize(heightMeasureSpec))
            else -> wrapHeight
        }
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        needsUpdate = true
    }

    private fun updatePathsIfNeeded() {
        if (!needsUpdate || seriesList.isEmpty()) return

        val leftPad = 70f
        val rightPad = 36f
        val topPad = 44f
        val botPad = 54f

        val width = width.toFloat()
        val height = height.toFloat()
        chartRect.set(
            leftPad,
            topPad,
            width - rightPad,
            height - botPad
        )

        val daysCount = (maxDay - minDay).coerceAtLeast(1)
        val stepX = chartRect.width() / daysCount
        val stepY = if (maxSum == 0f) 1f else chartRect.height() / maxSum

        pointsScreen = seriesList.map { serie ->
            serie.points.map { pt ->
                val x = chartRect.left + (pt.day - minDay) * stepX
                val y = chartRect.bottom - pt.sum * stepY
                x to y
            }
        }

        seriesPaths = pointsScreen.map { points ->
            val linePath = Path()
            val fillPath = Path()
            points.forEachIndexed { idx, (x, y) ->
                if (idx == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, chartRect.bottom)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            if (points.isNotEmpty()) {
                fillPath.lineTo(points.last().first, chartRect.bottom)
                fillPath.close()
            }
            linePath to fillPath
        }
        needsUpdate = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (seriesList.isEmpty()) return
        updatePathsIfNeeded()

        val yStepCount = 4
        val yGridStep = maxSum / yStepCount
        for (i in 0..yStepCount) {
            val yVal = yGridStep * i
            val y = chartRect.bottom - (yVal * chartRect.height() / maxSum)
            canvas.drawLine(chartRect.left, y, chartRect.right, y, gridPaint)
            canvas.drawText(yVal.roundToInt().toString(), chartRect.left - 10f, y + 10f, yLabelPaint)
        }

        val xStepCount = min(6, (seriesList[0].points.size - 1))
        val xLabelStep = max(1, seriesList[0].points.size / (xStepCount.takeIf { it > 0 } ?: 1))
        for (i in seriesList[0].points.indices step xLabelStep) {
            val (x, _) = pointsScreen[0][i]
            val dayNum = seriesList[0].points[i].day
            canvas.drawLine(x, chartRect.bottom, x, chartRect.top, gridPaint)
            canvas.drawText("День $dayNum", x, chartRect.bottom + 28f, textPaint)
        }

        canvas.drawLine(chartRect.left, chartRect.bottom, chartRect.right, chartRect.bottom, axisPaint)
        canvas.drawLine(chartRect.left, chartRect.bottom, chartRect.left, chartRect.top, axisPaint)

        seriesList.forEachIndexed { idx, serie ->
            val (line, fill) = seriesPaths[idx]
            fillPaint.color = ColorUtils.setAlphaComponent(serie.color, 40)
            linePaint.color = serie.color
            pointPaint.color = serie.color
            canvas.drawPath(fill, fillPaint)
            canvas.drawPath(line, linePaint)
            pointsScreen[idx].forEach { (x, y) ->
                canvas.drawCircle(x, y, 9f, pointPaint)
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable("InstState", super.onSaveInstanceState())
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        (state as? Bundle)?.let {
            super.onRestoreInstanceState(it.getParcelable("InstState"))
        }
    }
}

