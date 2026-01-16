package com.example.emitterapp.ui.rc_screen.components

sealed class JoystickMode{

    data class Spring(val initialPosition: Pair<Int, Int> = CENTER) : JoystickMode()
    data class Hold(val initialPosition: Pair<Int, Int> = CENTER): JoystickMode()
    data class VerticalSpring(val initialPosition: Pair<Int, Int> = CENTER) : JoystickMode()
    data class VerticalHold(val initialPosition: Pair<Int, Int> = CENTER): JoystickMode()
    data class HorizontalSpring(val initialPosition: Pair<Int, Int> = CENTER) : JoystickMode()
    data class HorizontalHold(val initialPosition: Pair<Int, Int> = CENTER): JoystickMode()
    companion object{
        val CENTER = Pair(6, 6)
        val UP = Pair(6, 0)
        val DOWN = Pair(6, 12)
        val LEFT = Pair(0, 6)
        val RIGHT = Pair(12, 6)
    }
}