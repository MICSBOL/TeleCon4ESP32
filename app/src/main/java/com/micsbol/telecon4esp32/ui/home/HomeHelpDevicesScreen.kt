package com.micsbol.telecon4esp32.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.NeoScaffold
import com.micsbol.telecon4esp32.ui.theme.AppGlass
import com.micsbol.telecon4esp32.ui.theme.Neo

/**
 * Photos shipped with this screen, resized only:
 * DevKit front and other side — Ubahnverleih, CC0 1.0
 * DevKit top — Edwiyanto, CC BY-SA 4.0
 * ESP32-CAM front — Nowforever, CC BY-SA 4.0
 * ESP32-CAM in hand — Marcio Teixeira, CC BY 2.0 (Flickr 51752417752)
 * ESP32-CAM close-up — Marcio Teixeira, CC BY 2.0 (Flickr 51762025554)
 */
private data class BoardPhoto(
    @DrawableRes val imageRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val creditRes: Int,
)

private data class RequiredBoard(
    @StringRes val nameRes: Int,
    @StringRes val roleRes: Int,
    @StringRes val bodyRes: Int,
    val specRes: List<Int>,
    val photos: List<BoardPhoto>,
)

private val requiredBoards = listOf(
    RequiredBoard(
        nameRes = R.string.home_help_device_devkit_name,
        roleRes = R.string.home_help_device_devkit_role,
        bodyRes = R.string.home_help_device_devkit_body,
        specRes = listOf(
            R.string.home_help_device_devkit_spec_module,
            R.string.home_help_device_devkit_spec_cpu,
            R.string.home_help_device_devkit_spec_radio,
            R.string.home_help_device_devkit_spec_usb,
            R.string.home_help_device_devkit_spec_app,
        ),
        photos = listOf(
            BoardPhoto(
                imageRes = R.drawable.help_device_esp32_devkit,
                descriptionRes = R.string.home_help_device_devkit_image,
                creditRes = R.string.home_help_device_devkit_credit,
            ),
            BoardPhoto(
                imageRes = R.drawable.help_device_esp32_devkit_angle,
                descriptionRes = R.string.home_help_device_devkit_image_angle,
                creditRes = R.string.home_help_device_devkit_credit_angle,
            ),
            BoardPhoto(
                imageRes = R.drawable.help_device_esp32_devkit_top,
                descriptionRes = R.string.home_help_device_devkit_image_top,
                creditRes = R.string.home_help_device_devkit_credit_top,
            ),
        ),
    ),
    RequiredBoard(
        nameRes = R.string.home_help_device_cam_name,
        roleRes = R.string.home_help_device_cam_role,
        bodyRes = R.string.home_help_device_cam_body,
        specRes = listOf(
            R.string.home_help_device_cam_spec_board,
            R.string.home_help_device_cam_spec_camera,
            R.string.home_help_device_cam_spec_memory,
            R.string.home_help_device_cam_spec_power,
            R.string.home_help_device_cam_spec_app,
        ),
        photos = listOf(
            BoardPhoto(
                imageRes = R.drawable.help_device_esp32_cam,
                descriptionRes = R.string.home_help_device_cam_image,
                creditRes = R.string.home_help_device_cam_credit,
            ),
            BoardPhoto(
                imageRes = R.drawable.help_device_esp32_cam_scale,
                descriptionRes = R.string.home_help_device_cam_image_scale,
                creditRes = R.string.home_help_device_cam_credit_scale,
            ),
            BoardPhoto(
                imageRes = R.drawable.help_device_esp32_cam_closeup,
                descriptionRes = R.string.home_help_device_cam_image_closeup,
                creditRes = R.string.home_help_device_cam_credit_closeup,
            ),
        ),
    ),
)

@Composable
fun HomeHelpDevicesScreen(navController: NavController) {
    NeoScaffold(
        title = stringResource(R.string.home_help_devices_title),
        subtitle = stringResource(R.string.home_help_devices_subtitle),
        onNavigateBack = { navController.navigateUp() },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.home_help_devices_intro),
                    color = Neo.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )
            }
            items(requiredBoards, key = { it.nameRes }) { board ->
                RequiredBoardCard(board)
            }
        }
    }
}

@Composable
private fun RequiredBoardCard(board: RequiredBoard) {
    val cardShape = RoundedCornerShape(16.dp)
    var zoomPage by remember(board.nameRes) { mutableStateOf<Int?>(null) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(AppGlass.DialogSurface)
            .border(1.dp, Color.White.copy(alpha = 0.16f), cardShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BoardPhotoSlider(
            photos = board.photos,
            onZoom = { zoomPage = it },
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(board.nameRes),
                color = Neo.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(board.roleRes),
                color = Neo.Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = stringResource(board.bodyRes),
            color = Neo.TextPrimary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            board.specRes.forEach { spec ->
                Text(
                    text = stringResource(spec),
                    color = Neo.TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
    zoomPage?.let { page ->
        BoardPhotoZoomDialog(
            photos = board.photos,
            initialPage = page,
            onDismiss = { zoomPage = null },
        )
    }
}

@Composable
private fun BoardPhotoSlider(
    photos: List<BoardPhoto>,
    onZoom: (Int) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { photos.size })
    val frame = RoundedCornerShape(12.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(frame)
                .background(Color.White),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val photo = photos[page]
                Image(
                    painter = painterResource(photo.imageRes),
                    contentDescription = stringResource(photo.descriptionRes),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                )
            }
            IconButton(
                onClick = { onZoom(pagerState.currentPage) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Filled.ZoomIn,
                    contentDescription = stringResource(R.string.home_help_device_zoom),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(photos.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (selected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (selected) Neo.Accent else Color.White.copy(alpha = 0.75f)),
                    )
                }
            }
        }
        Text(
            text = stringResource(photos[pagerState.currentPage].creditRes),
            color = Neo.TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
    }
}

@Composable
private fun BoardPhotoZoomDialog(
    photos: List<BoardPhoto>,
    initialPage: Int,
    onDismiss: () -> Unit,
) {
    var page by remember { mutableIntStateOf(initialPage) }
    val photo = photos[page]
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f)),
        ) {
            key(page) {
                ZoomableBoardPhoto(
                    photo = photo,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 72.dp),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ZoomToolButton(
                    icon = Icons.Filled.Close,
                    description = stringResource(R.string.home_help_device_zoom_close),
                    onClick = onDismiss,
                )
            }
            if (photos.size > 1) {
                ZoomToolButton(
                    icon = Icons.Filled.ChevronLeft,
                    description = stringResource(R.string.home_help_device_photo_previous),
                    onClick = { page = if (page == 0) photos.lastIndex else page - 1 },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp),
                )
                ZoomToolButton(
                    icon = Icons.Filled.ChevronRight,
                    description = stringResource(R.string.home_help_device_photo_next),
                    onClick = { page = if (page == photos.lastIndex) 0 else page + 1 },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp),
                )
            }
            Text(
                text = stringResource(photo.creditRes),
                color = Neo.TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun ZoomableBoardPhoto(
    photo: BoardPhoto,
    modifier: Modifier = Modifier,
) {
    val scaleState = remember { mutableFloatStateOf(1f) }
    val offsetState = remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val next = (scaleState.floatValue * zoomChange).coerceIn(1f, 5f)
        scaleState.floatValue = next
        offsetState.value = if (next <= 1.02f) Offset.Zero else offsetState.value + panChange
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(photo.imageRes),
            contentDescription = stringResource(photo.descriptionRes),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .transformable(transformState)
                .graphicsLayer {
                    scaleX = scaleState.floatValue
                    scaleY = scaleState.floatValue
                    translationX = offsetState.value.x
                    translationY = offsetState.value.y
                },
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ZoomToolButton(
                icon = Icons.Filled.ZoomOut,
                description = stringResource(R.string.home_help_device_zoom_out),
                onClick = {
                    scaleState.floatValue = 1f
                    offsetState.value = Offset.Zero
                },
            )
            ZoomToolButton(
                icon = Icons.Filled.ZoomIn,
                description = stringResource(R.string.home_help_device_zoom_in),
                onClick = {
                    scaleState.floatValue = (scaleState.floatValue * 1.6f).coerceAtMost(5f)
                },
            )
        }
    }
}

@Composable
private fun ZoomToolButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .background(Color.Black.copy(alpha = 0.45f), CircleShape),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
        )
    }
}
