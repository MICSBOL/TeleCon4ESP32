package com.micsbol.telecon4esp32.ui.control_panel.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import com.micsbol.telecon4esp32.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PushButtonSide(
    modifier: Modifier,
    side: ButtonSide = ButtonSide.LEFT,
    onPress: () -> Unit
){
    val frames = remember(side) {
        val prefix = if(side == ButtonSide.LEFT) "btn_left_" else "btn_right_"
        (1..20).map {
            val number = it.toString().padStart(2, '0')
            val resourceName = "$prefix$number"
            R.drawable::class.java.getField(resourceName).getInt(null)
        }
    }

    var frame by remember { mutableIntStateOf(0) }

    val coroutineScope = rememberCoroutineScope()
    var animationJob by remember { mutableStateOf<Job?>(null)}

    Image(
        painter = painterResource(id = frames[frame]),
        contentDescription = null,
        modifier = modifier
            .pointerInput(side ){
                detectTapGestures(
                    onPress = {
                        onPress()
                        animationJob?.cancel()
                        animationJob = coroutineScope.launch {
                            for(i in frames.indices){
                                frame = i
                                delay(5)
                            }
                        }

                        val released = tryAwaitRelease()

                        if(released) {
                            animationJob?.cancel()
                            animationJob = coroutineScope.launch {
                                for(i in frames.indices.reversed()){
                                    frame = i
                                    delay(5)
                                }
                            }
                        }
                    }
                )
            }
    )
}

enum class ButtonSide{
    LEFT,
    RIGHT
}