package on.emission.maps.util.color

import org.junit.Assert.assertEquals
import org.junit.Test

class ColormapUtilTest {

    @Test
    fun testGetColor_simple() {
        val color = ColormapUtil.getColor(0.5, 0.0, 1.0, "Simple", false)
        // Midpoint should be green
        assertEquals(0xFF00FF00.toInt(), color)
    }

    @Test
    fun testGetColor_diverging() {
        val color = ColormapUtil.getColor(0.5, 0.0, 1.0, "Diverging", false)
        // Midpoint should be white
        assertEquals(0xFFFFFFFF.toInt(), color)
    }

    @Test
    fun testGetColor_outOfBounds() {
        val color = ColormapUtil.getColor(1.5, 0.0, 1.0, "Simple", false)
        // Should be clamped to the last color (yellow)
        assertEquals(0xFFFFFF00.toInt(), color)
    }

    @Test
    fun testGetColor_logScale() {
        // This is harder to test without knowing the exact interpolation
        // I will just check that it runs without crashing
        ColormapUtil.getColor(50.0, 1.0, 100.0, "Simple", true)
    }
}
