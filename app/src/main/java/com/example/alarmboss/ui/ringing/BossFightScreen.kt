package com.example.alarmboss.ui.ringing

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarmboss.R
import com.example.alarmboss.ui.ringing.tasks.*

@Composable
fun BossFightScreen(difficulty: Int, onSolved: () -> Unit) {
    var stage by remember { mutableIntStateOf(0) }
    
    val bossBg = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF380310),
                Color(0xFF1C0208),
                Color(0xFF0D0104)
            )
        )
    }

    val stageNames = listOf("Rune Math", "Memory Sequence", "Labyrinth Escape")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bossBg)
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Boss Emblem & Title
        Surface(
            shape = CircleShape,
            color = Color(0xFF5A0819),
            border = BorderStroke(2.dp, Color(0xFFFF2A55)),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("💀", fontSize = 32.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.boss_title),
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFFFF4D6D),
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Spacer(Modifier.height(4.dp))

        // Boss Health / Stage Bar
        val healthProgress = (3 - stage) / 3f
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.boss_stage, stage + 1, 3),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = stageNames.getOrElse(stage) { "Final Stand" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF85A1)
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { healthProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFFFF2A55),
                trackColor = Color(0xFF4A0815)
            )
        }

        Spacer(Modifier.height(12.dp))
        
        Box(modifier = Modifier.weight(1f)) {
            Crossfade(targetState = stage, label = "BossStage") { currentStage ->
                when (currentStage) {
                    0 -> MathTaskScreen(difficulty = difficulty) { stage++ }
                    1 -> SequenceTaskScreen(difficulty = difficulty) { stage++ }
                    2 -> MazeTaskScreen(difficulty = difficulty) { onSolved() }
                }
            }
        }
    }
}
