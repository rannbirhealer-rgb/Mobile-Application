package com.example.alarmboss.ui.ringing.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.alarmboss.R
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun PatternLockTaskScreen(difficulty: Int, onSolved: () -> Unit) {
    val size = if (difficulty < 5) 3 else 4
    val gridSize = size * size
    val patternLength = (difficulty / 2) + 4 // 4 to 9

    var targetPattern by remember { mutableStateOf(emptyList<Int>()) }
    var currentInput by remember { mutableStateOf(emptyList<Int>()) }
    var isMemorizePhase by remember { mutableStateOf(true) }
    var errorState by remember { mutableStateOf(false) }
    var resetTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(resetTrigger, difficulty) {
        isMemorizePhase = true
        errorState = false
        currentInput = emptyList()

        val pattern = mutableListOf<Int>()
        var current = (0 until gridSize).random()
        pattern.add(current)
        
        while (pattern.size < patternLength) {
            val neighbors = mutableListOf<Int>()
            val r = current / size
            val c = current % size
            
            if (r > 0) neighbors.add(current - size)
            if (r < size - 1) neighbors.add(current + size)
            if (c > 0) neighbors.add(current - 1)
            if (c < size - 1) neighbors.add(current + 1)
            
            val valid = neighbors.filter { it !in pattern }
            if (valid.isEmpty()) {
                pattern.clear()
                current = (0 until gridSize).random()
                pattern.add(current)
                continue
            }
            
            current = valid.random()
            pattern.add(current)
        }
        
        targetPattern = pattern
        delay(3000)
        isMemorizePhase = false
    }

    LaunchedEffect(currentInput) {
        if (!isMemorizePhase && currentInput.isNotEmpty()) {
            val currentIndex = currentInput.size - 1
            if (currentInput[currentIndex] != targetPattern[currentIndex]) {
                errorState = true
                delay(800)
                resetTrigger++
            } else if (currentInput.size == targetPattern.size) {
                delay(300)
                onSolved()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(if (isMemorizePhase) R.string.pattern_memorize else R.string.pattern_trace),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(size),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.aspectRatio(1f)
        ) {
            items(gridSize) { index ->
                val orderInTarget = targetPattern.indexOf(index)
                val isSelectedTarget = isMemorizePhase && orderInTarget != -1
                val isSelectedInput = index in currentInput
                
                val tileColor = when {
                    errorState && currentInput.lastOrNull() == index -> MaterialTheme.colorScheme.error
                    isSelectedTarget || isSelectedInput -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(tileColor)
                        .clickable(
                            // Stop taking taps once the pattern is complete: an extra tap during the
                            // finish delay would index past the end of the target and crash.
                            enabled = !isMemorizePhase && !errorState && !isSelectedInput &&
                                currentInput.size < targetPattern.size
                        ) {
                            currentInput = currentInput + index
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelectedTarget) {
                        Text("${orderInTarget + 1}", color = MaterialTheme.colorScheme.onPrimary)
                    } else if (isSelectedInput) {
                        Text("${currentInput.indexOf(index) + 1}", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
