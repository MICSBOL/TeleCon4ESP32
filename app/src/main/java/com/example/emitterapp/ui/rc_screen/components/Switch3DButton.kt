package com.example.emitterapp.ui.rc_screen.components

import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.emitterapp.R
import kotlinx.coroutines.delay

@Composable
fun Switch3DButton(
    modifier: Modifier = Modifier,
    isOn: Boolean,
    onStateChange: (Boolean) -> Unit
) {
    val frames = listOf(
        R.drawable.switch__01,
        R.drawable.switch__02,
        R.drawable.switch__03,
        R.drawable.switch__04,
        R.drawable.switch__05,
        R.drawable.switch__06,
        R.drawable.switch__07,
        R.drawable.switch__08,
        R.drawable.switch__09,
        R.drawable.switch__10,
    )

    var frame by remember { mutableStateOf(if (isOn) frames.size - 1 else 0) }

    var isBusy by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    val mediaPlayer = remember {
        if (isInPreview) {
            null
        } else {
            MediaPlayer.create(context, R.raw.click_sound)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    LaunchedEffect(isOn) {

        isBusy = true

        if (isOn) {
            for (i in frames.size - 1 downTo 0) {
                frame = i
                delay(5)
            }
        } else {
            for (i in 0 until frames.size) {
                frame = i
                delay(5)
            }
        }
        isBusy = false
    }

    Image(
        painter = painterResource(id = frames[frame]),
        contentDescription = if (isOn) "On" else "Off",
        modifier = modifier
            .size(70.dp)
            .pointerInput(isBusy, isOn) {
                detectDragGestures { change, dragAmount ->
                    if (isBusy) return@detectDragGestures
                    val verticalDrag = dragAmount.y
                    if (verticalDrag >3 && isOn) {
                        mediaPlayer?.safeStart()
                        onStateChange(false)
                    } else if (verticalDrag < -3 && !isOn) {
                        mediaPlayer?.safeStart()
                        onStateChange(true)
                    }
                }
            }
    )
}

private fun MediaPlayer.safeStart(){
    if(this.isPlaying){
        this.stop()
        this.prepare()
    }
    this.start()
}