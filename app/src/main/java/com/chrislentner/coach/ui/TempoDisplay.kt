package com.chrislentner.coach.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun TempoDisplay(tempo: String) {
    if (tempo.length != 4) return

    val phases = listOf(
        TempoPhase(tempo[0].toString(), TempoIndicator.Down),
        TempoPhase(tempo[1].toString(), TempoIndicator.Bottom),
        TempoPhase(tempo[2].toString(), TempoIndicator.Up),
        TempoPhase(tempo[3].toString(), TempoIndicator.Top)
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        phases.forEach { phase ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Top Indicator
                IndicatorBox(indicator = phase.indicator, position = IndicatorPosition.Top)

                Text(
                    text = phase.duration,
                    style = MaterialTheme.typography.headlineMedium
                )

                // Bottom Indicator
                IndicatorBox(indicator = phase.indicator, position = IndicatorPosition.Bottom)
            }
        }
    }
}

private enum class IndicatorPosition {
    Top, Bottom
}

private enum class TempoIndicator {
    Down, Bottom, Up, Top
}

private data class TempoPhase(
    val duration: String,
    val indicator: TempoIndicator
)

@Composable
private fun IndicatorBox(indicator: TempoIndicator, position: IndicatorPosition) {
    val size = 20.dp
    val color = MaterialTheme.colorScheme.onSurface

    // Determine if we should draw in this position
    val shouldDraw = when (position) {
        IndicatorPosition.Top -> indicator == TempoIndicator.Up || indicator == TempoIndicator.Top
        IndicatorPosition.Bottom -> indicator == TempoIndicator.Down || indicator == TempoIndicator.Bottom
    }

    if (shouldDraw) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 3.dp.toPx()
            val w = size.toPx()
            val h = size.toPx()

            // Center vertically in the canvas space
            val midH = h / 2

            when (indicator) {
                TempoIndicator.Down -> {
                    // Arrow pointing down (v shape)
                    // Drawn in bottom position
                    val path = androidx.compose.ui.graphics.Path()
                    path.moveTo(w * 0.2f, midH * 0.5f)
                    path.lineTo(w * 0.5f, midH * 1.5f)
                    path.lineTo(w * 0.8f, midH * 0.5f)
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                TempoIndicator.Bottom -> {
                    // Horizontal line
                    // Drawn in bottom position
                    drawLine(
                        color = color,
                        start = Offset(w * 0.2f, midH),
                        end = Offset(w * 0.8f, midH),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
                TempoIndicator.Up -> {
                    // Arrow pointing up (^ shape)
                    // Drawn in top position
                    val path = androidx.compose.ui.graphics.Path()
                    path.moveTo(w * 0.2f, midH * 1.5f)
                    path.lineTo(w * 0.5f, midH * 0.5f)
                    path.lineTo(w * 0.8f, midH * 1.5f)
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                TempoIndicator.Top -> {
                    // Horizontal line
                    // Drawn in top position
                    drawLine(
                        color = color,
                        start = Offset(w * 0.2f, midH),
                        end = Offset(w * 0.8f, midH),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    } else {
        Spacer(modifier = Modifier.size(size))
    }
}
