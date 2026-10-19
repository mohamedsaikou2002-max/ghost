package com.casper.lab

import kotlin.math.sqrt

/**
 * Core, Android-independent calculations on artifact-screened normal-to-normal intervals.
 * No interpretation, diagnosis, or automatic labeling of arbitrary RF waveforms as heartbeats.
 */
data class HrvStats(
    val count: Int,
    val meanNnMs: Double,
    val meanHeartRateBpm: Double,
    val rmssdMs: Double,
    val sdnnMs: Double,
    val pnn50Percent: Double
)

object HrvMath {
    fun parseNnCsv(input: String): List<Double> {
        val values = input.split(',', ';', '\n', '\t', ' ')
            .filter { it.isNotBlank() }
            .map { token -> token.toDoubleOrNull() ?: error("Invalid interval: $token") }
        require(values.size >= 3) { "Enter at least 3 NN intervals." }
        require(values.size <= 10_000) { "Use 10,000 or fewer intervals." }
        require(values.all { it in 300.0..2000.0 && it.isFinite() }) {
            "Intervals must be between 300 and 2,000 milliseconds."
        }
        return values
    }

    fun calculate(nn: List<Double>): HrvStats {
        require(nn.size >= 3) { "At least 3 normal-to-normal intervals are required." }
        require(nn.all { it.isFinite() && it in 300.0..2000.0 }) { "Invalid NN intervals." }
        val mean = nn.average()
        val differences = nn.zipWithNext { a, b -> b - a }
        val rmssd = sqrt(differences.sumOf { it * it } / differences.size)
        val sdnn = sqrt(nn.sumOf { (it - mean) * (it - mean) } / (nn.size - 1))
        val pnn50 = differences.count { kotlin.math.abs(it) > 50.0 } * 100.0 / differences.size
        return HrvStats(nn.size, mean, 60_000.0 / mean, rmssd, sdnn, pnn50)
    }
}
