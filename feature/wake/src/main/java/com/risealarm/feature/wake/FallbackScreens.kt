package com.risealarm.feature.wake

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.risealarm.core.designsystem.*

@Composable
fun FallbackProgressScreen(title: String, instruction: String, value: Int, target: Int, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().background(RiseBackground).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StatusChip("Fallback kỹ thuật", RiseWarning)
        Spacer(Modifier.height(28.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Text(instruction, color = RiseTextMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(36.dp))
        Text("$value / $target", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { value.toFloat() / target.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth().height(12.dp),
            color = RisePrimary,
            trackColor = RiseSurfaceHigh,
        )
    }
}

@Composable
fun MathFallbackScreen(question: String, answers: List<Int>, correctCount: Int, target: Int, onAnswer: (Int) -> Unit) {
    Column(
        Modifier.fillMaxSize().background(RiseBackground).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StatusChip("Fallback kỹ thuật · $correctCount/$target", RiseWarning)
        Spacer(Modifier.weight(1f))
        Text(question, style = MaterialTheme.typography.displayLarge)
        Text("Chọn đáp án đúng để tiếp tục", color = RiseTextMuted)
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            answers.forEach { answer ->
                Button(
                    onClick = { onAnswer(answer) },
                    modifier = Modifier.weight(1f).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RiseSurfaceHigh, contentColor = Color.White),
                    shape = CircleShape,
                ) { Text(answer.toString(), style = MaterialTheme.typography.titleLarge) }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}
