package com.example.emitterapp.ui.rc_screen.components_led_style

import android.content.Context
import android.graphics.BlurMaskFilter
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.emitterapp.domain.model.JoystickMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun StickLedStyle(
	modifier: Modifier = Modifier,
	mode: JoystickMode = JoystickMode.Spring(),
	stickPosition: Pair<Float, Float> = Pair(0f, 0f),
	settingsSyncGeneration: Int = 0,
	onMove: (x: Float, y: Float) -> Unit
) {
	val initialNormalized = remember(mode) {
		val initial = when (mode) {
			is JoystickMode.Spring -> mode.initialPosition
			is JoystickMode.Hold -> mode.initialPosition
			is JoystickMode.VerticalSpring -> mode.initialPosition
			is JoystickMode.VerticalHold -> mode.initialPosition
			is JoystickMode.HorizontalSpring -> mode.initialPosition
			is JoystickMode.HorizontalHold -> mode.initialPosition
		}
		Offset(
			x = (initial.first - 6) / 6f,
			y = -(initial.second - 6) / 6f
		)
	}

	val targetOffset = Offset(stickPosition.first, stickPosition.second)
	val stickAnim = remember(settingsSyncGeneration) {
		Animatable(targetOffset, Offset.VectorConverter)
	}
	val scope = rememberCoroutineScope()
	var releaseJob by remember { mutableStateOf<Job?>(null) }

	var center by remember { mutableStateOf(Offset.Zero) }
	var dragRadius by remember { mutableStateOf(0f) }
	var touchRadius by remember { mutableStateOf(0f) }

	val context = LocalContext.current
	val vibrator = remember { context.resolveVibrator() }

	LaunchedEffect(settingsSyncGeneration) {
		releaseJob?.cancel()
		stickAnim.snapTo(Offset(stickPosition.first, stickPosition.second))
	}

	fun normalizedToVector(normalized: Offset): Offset {
		return Offset(
			x = normalized.x.coerceIn(-1f, 1f) * dragRadius,
			y = -normalized.y.coerceIn(-1f, 1f) * dragRadius
		)
	}

	fun vectorToNormalized(vector: Offset): Offset {
		if (dragRadius <= 0f) return Offset.Zero
		return Offset(
			x = (vector.x / dragRadius).coerceIn(-1f, 1f),
			y = (-vector.y / dragRadius).coerceIn(-1f, 1f)
		)
	}

	fun applyModeConstraints(vector: Offset): Offset {
		return when (mode) {
			is JoystickMode.VerticalSpring, is JoystickMode.VerticalHold -> Offset(0f, vector.y)
			is JoystickMode.HorizontalSpring, is JoystickMode.HorizontalHold -> Offset(vector.x, 0f)
			else -> vector
		}
	}

	Box(
		modifier = modifier
			.onSizeChanged { size ->
				center = Offset(size.width / 2f, size.height / 2f)
				dragRadius = min(size.width.toFloat(), size.height.toFloat()) * 0.36f
				touchRadius = min(size.width.toFloat(), size.height.toFloat()) * 0.16f
			}
			.pointerInput(mode, center, dragRadius, touchRadius) {
				awaitPointerEventScope {
					while (true) {
						val down = awaitFirstDown()
						val currentStickCenter = center + normalizedToVector(stickAnim.value)
						val distanceToStick = (down.position - currentStickCenter).getDistance()

						// Ignore touches far from the handle to feel like a real gimbal.
						if (distanceToStick > touchRadius) continue

						down.consume()
						releaseJob?.cancel()
						val dragId = down.id

						while (true) {
							val event = awaitPointerEvent()
							val change = event.changes.firstOrNull { it.id == dragId }

							if (change == null || !change.pressed) {
								when (mode) {
									is JoystickMode.Spring,
									is JoystickMode.VerticalSpring,
									is JoystickMode.HorizontalSpring -> {
										vibrator?.vibrateTick()
										releaseJob = scope.launch {
											stickAnim.animateTo(
												targetValue = initialNormalized,
												animationSpec = spring(
													dampingRatio = Spring.DampingRatioMediumBouncy,
													stiffness = Spring.StiffnessMediumLow
												)
											) {
												onMove(value.x, value.y)
											}
										}
									}

									else -> {
										onMove(stickAnim.value.x, stickAnim.value.y)
									}
								}
								break
							}

							val dragVector = change.position - center
							val dragDistance = dragVector.getDistance()
							val clamped = if (dragDistance > dragRadius && dragDistance > 0f) {
								val angle = atan2(dragVector.y, dragVector.x)
								Offset(
									x = dragRadius * cos(angle),
									y = dragRadius * sin(angle)
								)
							} else {
								dragVector
							}

							val constrained = applyModeConstraints(clamped)
							val normalized = vectorToNormalized(constrained)

							scope.launch {
								stickAnim.snapTo(normalized)
							}
							onMove(normalized.x, normalized.y)
							change.consume()
						}
					}
				}
			}
	) {
		Canvas(modifier = Modifier.fillMaxSize()) {
			if (dragRadius <= 0f) return@Canvas

			val neonMain = Color(0xFF00E5FF)
			val neonAccent = Color(0xFF7C4DFF)
			val background = Color(0xFF050B16)

			drawCircle(
				color = background,
				radius = dragRadius * 1.35f,
				center = center
			)

			drawGlowCircle(center, dragRadius * 1.12f, neonAccent, glow = 28f, coreStroke = 2.4f)
			drawGlowCircle(center, dragRadius * 0.78f, neonMain, glow = 24f, coreStroke = 1.8f)
			drawGlowCircle(center, dragRadius * 0.46f, neonAccent.copy(alpha = 0.8f), glow = 18f, coreStroke = 1.4f)

			val arm = dragRadius * 1.05f
			drawGlowLine(
				start = Offset(center.x - arm, center.y),
				end = Offset(center.x + arm, center.y),
				color = neonMain,
				glow = 20f,
				coreStroke = 2f
			)
			drawGlowLine(
				start = Offset(center.x, center.y - arm),
				end = Offset(center.x, center.y + arm),
				color = neonMain,
				glow = 20f,
				coreStroke = 2f
			)

			val normalized = stickAnim.value
			val stickCenter = center + normalizedToVector(normalized)

			drawGlowLine(
				start = center,
				end = stickCenter,
				color = neonAccent,
				glow = 26f,
				coreStroke = 3f
			)

			drawGlowCircle(center, dragRadius * 0.12f, neonMain, glow = 14f, coreStroke = 3f)
			drawGlowCircle(stickCenter, dragRadius * 0.19f, neonMain, glow = 30f, coreStroke = 4f)
			drawGlowCircle(stickCenter, dragRadius * 0.10f, Color.White.copy(alpha = 0.85f), glow = 10f, coreStroke = 1.5f)
		}
	}
	
}


private fun DrawScope.drawGlowLine(
	start: Offset,
	end: Offset,
	color: Color,
	glow: Float,
	coreStroke: Float
) {
	drawIntoCanvas { canvas ->
		val paint = Paint().asFrameworkPaint().apply {
			isAntiAlias = true
			style = android.graphics.Paint.Style.STROKE
			strokeCap = android.graphics.Paint.Cap.ROUND
			this.color = color.copy(alpha = 0.4f).toArgb()
			strokeWidth = coreStroke * 3.2f
			maskFilter = BlurMaskFilter(glow, BlurMaskFilter.Blur.NORMAL)
		}
		canvas.nativeCanvas.drawLine(start.x, start.y, end.x, end.y, paint)

		paint.maskFilter = null
		paint.color = color.toArgb()
		paint.strokeWidth = coreStroke
		canvas.nativeCanvas.drawLine(start.x, start.y, end.x, end.y, paint)
	}
}

private fun DrawScope.drawGlowCircle(
	center: Offset,
	radius: Float,
	color: Color,
	glow: Float,
	coreStroke: Float
) {
	drawIntoCanvas { canvas ->
		val paint = Paint().asFrameworkPaint().apply {
			isAntiAlias = true
			style = android.graphics.Paint.Style.STROKE
			this.color = color.copy(alpha = 0.35f).toArgb()
			strokeWidth = coreStroke * 3f
			maskFilter = BlurMaskFilter(glow, BlurMaskFilter.Blur.NORMAL)
		}
		canvas.nativeCanvas.drawCircle(center.x, center.y, radius, paint)

		paint.maskFilter = null
		paint.color = color.toArgb()
		paint.strokeWidth = coreStroke
		canvas.nativeCanvas.drawCircle(center.x, center.y, radius, paint)
	}
}

private fun Context.resolveVibrator(): Vibrator? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
		val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
		manager?.defaultVibrator
	} else {
		@Suppress("DEPRECATION")
		getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
	}
}

private fun Vibrator.vibrateTick() {
	if (!hasVibrator()) return
	vibrate(VibrationEffect.createOneShot(30L, 110))
}

@Preview(showBackground = true, widthDp = 260, heightDp = 260)
@Composable
private fun StickLedStylePreview() {
	StickLedStyle(
        mode = JoystickMode.HorizontalSpring(initialPosition = 6 to 6),
        onMove = { _, _ -> })
}