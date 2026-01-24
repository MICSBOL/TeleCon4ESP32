package com.example.emitterapp.ui.rc_screen.components

sealed class JoystickMode{
    abstract val initialPosition: Pair<Int, Int>
    data class Spring(override val initialPosition: Pair<Int, Int> = CENTER) : JoystickMode()
    data class Hold(override val initialPosition: Pair<Int, Int> = CENTER): JoystickMode()
    data class VerticalSpring(override val initialPosition: Pair<Int, Int> = CENTER) : JoystickMode()
    data class VerticalHold(override val initialPosition: Pair<Int, Int> = CENTER): JoystickMode()
    data class HorizontalSpring(override val initialPosition: Pair<Int, Int> = CENTER) : JoystickMode()
    data class HorizontalHold(override val initialPosition: Pair<Int, Int> = CENTER): JoystickMode()
    companion object{
        val CENTER = Pair(6, 6)
        val UP = Pair(6, 0)
        val DOWN = Pair(6, 12)
        val LEFT = Pair(0, 6)
        val RIGHT = Pair(12, 6)


        fun JoystickMode.toStringRepresentation(): String {
            val modeName = this::class.java.simpleName
            val posX = this.initialPosition.first
            val posY = this.initialPosition.second
            return "$modeName,$posX,$posY"
        }

        fun fromString(savedString: String?): JoystickMode {
            if (savedString == null) return Spring() // Default value

            val parts = savedString.split(',')
            if (parts.size != 3) return Spring() // Corrupted data, return default

            val modeName = parts[0]
            val posX = parts[1].toIntOrNull() ?: 6
            val posY = parts[2].toIntOrNull() ?: 6
            val position = Pair(posX, posY)

            return when (modeName) {
                "Hold" -> Hold(position)
                "VerticalSpring" -> VerticalSpring(position)
                "VerticalHold" -> VerticalHold(position)
                "HorizontalSpring" -> HorizontalSpring(position)
                "HorizontalHold" -> HorizontalHold(position)
                else -> Spring(position)
            }
        }
    }
}