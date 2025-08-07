package on.emission.maps.util.color

import android.graphics.Color

object ColormapUtil {

    // A simple Blue-Green-Yellow colormap for now.
    private val SIMPLE_COLORMAP = intArrayOf(
        0xFF0000FF.toInt(), // Blue
        0xFF00FF00.toInt(), // Green
        0xFFFFFF00.toInt()  // Yellow
    )

    private val DIVERGING_COLORMAP = intArrayOf(
        0xFF0000FF.toInt(), // Blue
        0xFFFFFFFF.toInt(), // White
        0xFFFF0000.toInt()  // Red
    )

    fun getColor(value: Double, min: Double, max: Double, colormapName: String, useLogScale: Boolean): Int {
        if (min >= max) return Color.GRAY
        if (value.isNaN()) return Color.GRAY

        val normalizedValue = if (useLogScale) {
            if (value <= 0 || min <= 0) 0.0 else (Math.log(value) - Math.log(min)) / (Math.log(max) - Math.log(min))
        } else {
            (value - min) / (max - min)
        }

        val clippedValue = normalizedValue.coerceIn(0.0, 1.0)

        return when (colormapName) {
            "Simple" -> interpolateColor(clippedValue, SIMPLE_COLORMAP)
            "Diverging" -> interpolateColor(clippedValue, DIVERGING_COLORMAP)
            else -> interpolateColor(clippedValue, SIMPLE_COLORMAP)
        }
    }

    private fun interpolateColor(fraction: Double, colors: IntArray): Int {
        if (fraction <= 0.0) return colors.first()
        if (fraction >= 1.0) return colors.last()

        val position = fraction * (colors.size - 1)
        val index = position.toInt()
        val localFraction = position - index

        val startColor = colors[index]
        val endColor = colors[index + 1]

        val startA = (startColor shr 24) and 0xff
        val startR = (startColor shr 16) and 0xff
        val startG = (startColor shr 8) and 0xff
        val startB = startColor and 0xff

        val endA = (endColor shr 24) and 0xff
        val endR = (endColor shr 16) and 0xff
        val endG = (endColor shr 8) and 0xff
        val endB = endColor and 0xff

        val a = (startA + localFraction * (endA - startA)).toInt()
        val r = (startR + localFraction * (endR - startR)).toInt()
        val g = (startG + localFraction * (endG - startG)).toInt()
        val b = (startB + localFraction * (endB - startB)).toInt()

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
