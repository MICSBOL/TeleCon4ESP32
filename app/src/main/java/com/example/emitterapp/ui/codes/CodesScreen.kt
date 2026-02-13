package com.example.emitterapp.ui.codes

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.IOException
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.delay

private data class PdfInfo(
    val title: String,
    val icon: ImageVector,
    val assetFileName: String
)

private val availablePdfs = listOf(
    PdfInfo(
        title = "ESP32",
        icon = Icons.Default.Memory,
        assetFileName = "SecondDocumentation.pdf"
    ),
    PdfInfo(
        title = "Kotlin Notes",
        icon = Icons.Default.Code,
        assetFileName = "FirstDocumentation.pdf"
    ),
    PdfInfo(
        title = "ESP32_EN",
        icon = Icons.Default.Memory,
        assetFileName = "C_arduino_documentation.pdf"
    ),
    PdfInfo(
        title = "ESP32_ES",
        icon = Icons.Default.Code,
        assetFileName = "ESP32_C_Documentation_ES.pdf"
    )
)

@Composable
fun CodesScreen() {
    var selectedPdf by remember { mutableStateOf<String?>(null) }

    if (selectedPdf == null) {
        PdfGrid(
            onPdfClick = { fileName ->
                selectedPdf = fileName
            }
        )
    } else {
        PdfViewer(
            assetFileName = selectedPdf!!,
            onBack = {
                selectedPdf = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PdfGrid(onPdfClick: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Code Documents") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            modifier = Modifier
                .padding(paddingValues)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availablePdfs) { pdf ->
                PdfGridItem(pdfInfo = pdf, onClick = { onPdfClick(pdf.assetFileName) })
            }
        }
    }
}

@Composable
private fun PdfGridItem(pdfInfo: PdfInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = pdfInfo.icon,
                contentDescription = pdfInfo.title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = pdfInfo.title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PdfViewer(assetFileName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    val lazyListState = rememberLazyListState()
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    var debouncedScale by remember { mutableStateOf(1f) }

    LaunchedEffect(scale) {
        if(scale == 1f){
            debouncedScale = 1f
            return@LaunchedEffect
        }

        delay(200L)
        debouncedScale = scale
    }

    LaunchedEffect(assetFileName) {
        try {
            val tempFile = File(context.cacheDir, assetFileName)
            context.assets.open(assetFileName).use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            fileDescriptor =
                ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = fileDescriptor?.let { PdfRenderer(it) }
        } catch (e: IOException) {
            e.printStackTrace()
            onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pdfRenderer?.close()
            fileDescriptor?.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(assetFileName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    }) {
                        Icon(Icons.Default.ZoomOutMap, contentDescription = "Reset Zoom")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        if (pdfRenderer == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .clipToBounds()
                    .background(Color.Gray)
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldScale = scale
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale

                            offset = (offset + centroid - centroid * newScale / oldScale) + pan

                            val maxPanX = (size.width / 2) * (scale - 1)
                            val maxPanY = (size.height / 2) * (scale - 1)
                            offset = Offset(
                                x = offset.x.coerceIn(-maxPanX, maxPanX),
                                y = offset.y.coerceIn(-maxPanY, maxPanY)
                            )
                        }
                    }
            ) {
                LazyColumn(
                    state = lazyListState,
                    userScrollEnabled = scale == 1f,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val pageCount = pdfRenderer?.pageCount ?: 0
                    items(pageCount) { pageIndex ->
                        PdfPage(
                            renderer = pdfRenderer!!,
                            pageIndex = pageIndex,
                            scale = debouncedScale
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPage(renderer: PdfRenderer, pageIndex: Int, scale: Float) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(key1 = renderer, key2 = pageIndex, key3 = scale) {
        launch(Dispatchers.IO) {
            val currentPage = renderer.openPage(pageIndex)

            val width = (currentPage.width * scale).toInt()
            val height = (currentPage.height * scale).toInt()

            val newBitmap = createBitmap(
                width, height, Bitmap.Config.ARGB_8888
            )
            currentPage.render(newBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            currentPage.close()
            bitmap = newBitmap
        }
    }

    val imageBitmap = bitmap?.asImageBitmap()
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "PDF Page ${pageIndex + 1}",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .background(Color.White)
                .fillMaxWidth()
                .aspectRatio(
                    (renderer.getPageWidth(pageIndex).toFloat()) / (renderer.getPageHeight(pageIndex).toFloat())
                )
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f / 1.41f)
                .background(Color.LightGray)
        ){
            CircularProgressIndicator()
        }
    }
}

private fun PdfRenderer.getPageWidth(pageIndex: Int): Int {
    return openPage(pageIndex).use { it.width }
}

private fun PdfRenderer.getPageHeight(pageIndex: Int): Int {
    return openPage(pageIndex).use { it.height }
}