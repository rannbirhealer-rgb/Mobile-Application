package com.example.alarmboss.ui.ringing.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.alarmboss.R
import kotlin.random.Random
import androidx.compose.foundation.text.KeyboardOptions


private data class Problem(val text: String, val answer: Int)

// difficulty 1 matches the original ranges (operands < 50, factors < 12); each step widens them.
private fun randomProblem(difficulty: Int): Problem {
    val maxOperand = 40 + difficulty * 10
    val maxFactor = 11 + difficulty
    val a = Random.nextInt(2, maxOperand)
    val b = Random.nextInt(2, maxOperand)
    return when (Random.nextInt(3)) {
        0 -> Problem("$a + $b", a + b)
        1 -> Problem("$a - $b", a - b)
        else -> {
            val small = Random.nextInt(2, maxFactor)
            val small2 = Random.nextInt(2, maxFactor)
            Problem("$small × $small2", small * small2)
        }
    }
}

/** Requires solving [problemCount] problems in a row correctly (3 by default) to dismiss the alarm. */
@Composable
fun MathTaskScreen(difficulty: Int = 1, problemCount: Int = 3, onSolved: () -> Unit) {
    var solvedCount by remember { mutableIntStateOf(0) }
    var problem by remember { mutableStateOf(randomProblem(difficulty)) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val target = problemCount
    val toggleSignDescription = stringResource(R.string.math_toggle_sign_description)

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(pluralStringResource(R.plurals.math_title, target, target), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.task_progress, solvedCount, target), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Text(problem.text, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            // Quick toggle for positive/negative input
            OutlinedButton(
                onClick = {
                    input = if (input.startsWith("-")) {
                        input.removePrefix("-")
                    } else {
                        "-$input"
                    }
                    error = false
                },
                modifier = Modifier.height(56.dp).semantics { contentDescription = toggleSignDescription }
            ) {
                Text("±", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedTextField(
                value = input,
                onValueChange = { newValue ->
                    // Only accept input if it's empty, a lone minus sign, or a valid signed integer
                    if (newValue.isEmpty() || newValue == "-" || newValue.toIntOrNull() != null) {
                        input = newValue
                        error = false
                    }
                },
                label = { Text(stringResource(R.string.task_answer)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = error,
                modifier = Modifier.weight(1f)
            )
        }
        if (error) Text(stringResource(R.string.task_try_again), color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            if (input.toIntOrNull() == problem.answer) {
                solvedCount++
                input = ""
                if (solvedCount >= target) onSolved() else problem = randomProblem(difficulty)
            } else {
                error = true
            }
        }) { Text(stringResource(R.string.task_submit)) }
    }
}
