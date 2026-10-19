import com.casper.lab.HrvMath
import kotlin.math.abs

fun near(a: Double, b: Double, epsilon: Double = 0.00001) {
    check(abs(a-b) < epsilon) { "Expected $b but got $a" }
}

fun main() {
    val a = HrvMath.calculate(listOf(800.0,800.0,800.0,800.0))
    near(a.rmssdMs,0.0)
    near(a.sdnnMs,0.0)
    near(a.meanHeartRateBpm,75.0)
    near(a.pnn50Percent,0.0)

    val b = HrvMath.calculate(listOf(800.0,900.0,800.0,900.0))
    near(b.rmssdMs,100.0)
    near(b.meanNnMs,850.0)
    near(b.sdnnMs,57.735026918962575)
    near(b.pnn50Percent,100.0)
    check(HrvMath.parseNnCsv("800, 900; 800\n900") == listOf(800.0,900.0,800.0,900.0))
    try { HrvMath.parseNnCsv("30,800,900"); error("invalid value not rejected") } catch (_: IllegalArgumentException) { }
    println("PASS: 7 HRV tests (zero variability, RMSSD, SDNN, pNN50, BPM, parse, invalid intervals)")
}
