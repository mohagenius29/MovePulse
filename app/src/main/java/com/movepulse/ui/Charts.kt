package com.movepulse.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val n = values.size.coerceAtLeast(1)
            val slot = size.width / n
            val barW = slot * 0.7f
            values.forEachIndexed { i, v ->
                val h = size.height * (v / max)
                drawRoundRect(
                    color = if (v > 0f) color else color.copy(alpha = 0.15f),
                    topLeft = Offset(i * slot + (slot - barW) / 2f, size.height - h.coerceAtLeast(3f)),
                    size = Size(barW, h.coerceAtLeast(3f)),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ProgressRing(progress: Float, color: Color, track: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = 22f, cap = StrokeCap.Round)
        val inset = 11f
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
        drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = stroke)
    }
}

@Composable
fun LegendRow(color: Color, label: String, minutes: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Text(label, modifier = Modifier.weight(1f))
        Text("$minutes min")
    }
}
