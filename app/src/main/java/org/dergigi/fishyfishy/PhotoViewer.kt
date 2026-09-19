package org.dergigi.fishyfishy

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.min

@Composable
fun PhotoViewer(photo: GuidePhoto, title: String, credit: String, dismiss: () -> Unit) {
    val strings = LocalStrings.current
    val painter = painterResource(photo.image)
    var scale by remember(photo.image) { mutableFloatStateOf(1f) }
    var offset by remember(photo.image) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    fun bounded(position: Offset, zoom: Float): Offset {
        val intrinsic = painter.intrinsicSize
        if (viewport.width == 0 || viewport.height == 0 || intrinsic.width <= 0 || intrinsic.height <= 0) return Offset.Zero
        val fit = min(viewport.width / intrinsic.width, viewport.height / intrinsic.height)
        val x = ((intrinsic.width * fit * zoom - viewport.width) / 2).coerceAtLeast(0f)
        val y = ((intrinsic.height * fit * zoom - viewport.height) / 2).coerceAtLeast(0f)
        return Offset(position.x.coerceIn(-x, x), position.y.coerceIn(-y, y))
    }
    fun zoomTo(value: Float, focus: Offset = Offset(viewport.width / 2f, viewport.height / 2f), pan: Offset = Offset.Zero) {
        val next = value.coerceIn(1f, 6f)
        val relative = focus - Offset(viewport.width / 2f, viewport.height / 2f)
        offset = bounded(relative - (relative - offset) * (next / scale) + pan, next)
        scale = next
    }
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$title · ${photo.label}", color = Color.White, modifier = Modifier.weight(1f))
                IconButton(onClick = dismiss) { Icon(Icons.Rounded.Close, strings("Close photo"), tint = Color.White) }
            }
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()
                .onSizeChanged { viewport = it; offset = bounded(offset, scale) }
                .pointerInput(photo.image) {
                    detectTapGestures(onDoubleTap = { point -> zoomTo(if (scale > 1f) 1f else 3f, point) })
                }
                .pointerInput(photo.image) {
                    detectTransformGestures { centroid, pan, zoom, _ -> zoomTo(scale * zoom, centroid, pan) }
                }) {
                Image(painter, title, Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale
                    translationX = offset.x; translationY = offset.y
                }, contentScale = ContentScale.Fit)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { zoomTo(scale / 1.5f) }, enabled = scale > 1f) {
                    Icon(Icons.Rounded.ZoomOut, strings("Zoom out"), tint = if (scale > 1f) Color.White else Color.Gray)
                }
                TextButton(onClick = { zoomTo(1f) }) { Text(strings("Reset"), color = Color.White) }
                IconButton(onClick = { zoomTo(scale * 1.5f) }, enabled = scale < 6f) {
                    Icon(Icons.Rounded.ZoomIn, strings("Zoom in"), tint = if (scale < 6f) Color.White else Color.Gray)
                }
            }
            Text(strings("Pinch to zoom · Drag to explore · Double-tap to zoom"), color = Color.LightGray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 12.dp))
            Text(credit, color = Color.LightGray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(12.dp))
        }
    }
}
