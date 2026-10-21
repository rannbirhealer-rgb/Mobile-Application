package com.example.alarmboss.engine

/** The fade-in used by "start quietly and get louder": a straight line from a soft start up to the target. */
object VolumeRamp {
    const val RAMP_MS = 30_000L

    /** Where the fade begins, as a fraction of the target. Audible, so a deep sleeper still starts to stir. */
    const val START_FRACTION = 0.15f

    /** The player volume (0..1) [elapsedMs] after the alarm started, reaching [target] once [rampMs] has passed. */
    fun volumeAt(elapsedMs: Long, rampMs: Long, target: Float): Float {
        if (rampMs <= 0L || elapsedMs >= rampMs) return target
        val start = target * START_FRACTION
        val progress = elapsedMs.coerceAtLeast(0L) / rampMs.toFloat()
        return start + (target - start) * progress
    }
}
