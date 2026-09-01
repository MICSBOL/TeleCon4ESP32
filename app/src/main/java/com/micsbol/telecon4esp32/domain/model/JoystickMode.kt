package com.micsbol.telecon4esp32.domain.model

import androidx.compose.runtime.Stable

enum class JoystickAxis {
    VERTICAL,
    HORIZONTAL,
    COMBINED,
}

/**
 * Domain model for joystick behaviour modes.
 * @Stable is a Compose compiler hint kept here because JoystickMode is used directly
 * as a Composable parameter; it avoids unnecessary recompositions without adding any
 * runtime dependency beyond the Compose annotation processor.
 */
@Stable
sealed class JoystickMode {
    abstract val initialPosition: Pair<Int, Int>
    data class Spring(override val initialPosition: Pair<Int, Int> = CENTER) : JoystickMode()
    data class Hold(override val initialPosition: Pair<Int, Int> = CENTER) : JoystickMode()
    data class VerticalSpring(override val initialPosition: Pair<Int, Int> = CENTER) : JoystickMode()
    data class VerticalHold(override val initialPosition: Pair<Int, Int> = CENTER) : JoystickMode()
    data class HorizontalSpring(override val initialPosition: Pair<Int, Int> = CENTER) : JoystickMode()
    data class HorizontalHold(override val initialPosition: Pair<Int, Int> = CENTER) : JoystickMode()

    fun allowsInitialPosition(position: Pair<Int, Int>): Boolean =
        position in allowedRestPositions()

    fun allowedRestPositions(): List<Pair<Int, Int>> = when (axis) {
        JoystickAxis.VERTICAL -> listOf(CENTER, UP, DOWN)
        JoystickAxis.HORIZONTAL -> listOf(CENTER, LEFT, RIGHT)
        JoystickAxis.COMBINED -> listOf(CENTER, UP, DOWN, LEFT, RIGHT)
    }

    val isSpring: Boolean
        get() = this is Spring || this is VerticalSpring || this is HorizontalSpring

    val axis: JoystickAxis
        get() = when (this) {
            is Spring, is Hold -> JoystickAxis.COMBINED
            is VerticalSpring, is VerticalHold -> JoystickAxis.VERTICAL
            is HorizontalSpring, is HorizontalHold -> JoystickAxis.HORIZONTAL
        }

    fun withSpring(spring: Boolean): JoystickMode =
        from(spring = spring, axis = axis, position = initialPosition)

    fun withAxis(axis: JoystickAxis): JoystickMode =
        from(spring = isSpring, axis = axis, position = initialPosition)

    /**
     * Vertical and Horizontal are independent. Combined is both on.
     * At least one axis always stays enabled.
     */
    fun togglingAxis(clicked: JoystickAxis): JoystickMode {
        val newAxis = when (clicked) {
            JoystickAxis.VERTICAL -> when (axis) {
                JoystickAxis.COMBINED -> JoystickAxis.HORIZONTAL
                JoystickAxis.HORIZONTAL -> JoystickAxis.COMBINED
                JoystickAxis.VERTICAL -> JoystickAxis.VERTICAL
            }
            JoystickAxis.HORIZONTAL -> when (axis) {
                JoystickAxis.COMBINED -> JoystickAxis.VERTICAL
                JoystickAxis.VERTICAL -> JoystickAxis.COMBINED
                JoystickAxis.HORIZONTAL -> JoystickAxis.HORIZONTAL
            }
            JoystickAxis.COMBINED -> axis
        }
        return withAxis(newAxis)
    }

    fun withInitialPosition(position: Pair<Int, Int>): JoystickMode =
        from(spring = isSpring, axis = axis, position = position)

    /** Keep the current rest position when switching mode if the new mode allows it. */
    fun replacedWith(template: JoystickMode): JoystickMode =
        from(spring = template.isSpring, axis = template.axis, position = initialPosition)

    fun initialPositionNormalized(): Pair<Float, Float> =
        gridPositionToNormalized(initialPosition)

    companion object {
        val CENTER = Pair(6, 6)
        val UP = Pair(6, 0)
        val DOWN = Pair(6, 12)
        val LEFT = Pair(0, 6)
        val RIGHT = Pair(12, 6)

        fun from(
            spring: Boolean,
            axis: JoystickAxis,
            position: Pair<Int, Int>,
        ): JoystickMode {
            val allowed = when (axis) {
                JoystickAxis.VERTICAL -> listOf(CENTER, UP, DOWN)
                JoystickAxis.HORIZONTAL -> listOf(CENTER, LEFT, RIGHT)
                JoystickAxis.COMBINED -> listOf(CENTER, UP, DOWN, LEFT, RIGHT)
            }
            val rest = if (position in allowed) position else CENTER
            return when (axis) {
                JoystickAxis.COMBINED -> if (spring) Spring(rest) else Hold(rest)
                JoystickAxis.VERTICAL -> if (spring) VerticalSpring(rest) else VerticalHold(rest)
                JoystickAxis.HORIZONTAL -> {
                    if (spring) HorizontalSpring(rest) else HorizontalHold(rest)
                }
            }
        }

        fun gridPositionToNormalized(position: Pair<Int, Int>): Pair<Float, Float> {
            val x = (position.first - 6) / 6f
            val y = (6 - position.second) / 6f
            return Pair(x, y)
        }

        fun JoystickMode.toStringRepresentation(): String {
            val modeName = this::class.java.simpleName
            val posX = this.initialPosition.first
            val posY = this.initialPosition.second
            return "$modeName,$posX,$posY"
        }

        fun fromString(savedString: String?): JoystickMode {
            if (savedString == null) return Spring()
            val parts = savedString.split(',')
            if (parts.size != 3) return Spring()
            val modeName = parts[0]
            val posX = parts[1].toIntOrNull() ?: 6
            val posY = parts[2].toIntOrNull() ?: 6
            val position = Pair(posX, posY)
            return when (modeName) {
                "Hold"             -> Hold(position)
                "VerticalSpring"   -> VerticalSpring(position)
                "VerticalHold"     -> VerticalHold(position)
                "HorizontalSpring" -> HorizontalSpring(position)
                "HorizontalHold"   -> HorizontalHold(position)
                else               -> Spring(position)
            }
        }
    }
}

