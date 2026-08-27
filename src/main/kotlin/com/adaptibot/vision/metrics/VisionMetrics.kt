package com.adaptibot.vision.metrics

import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/** Timing instrumentation for the vision pipeline. */
object VisionMetrics {

    private const val MAX_SAMPLES_PER_PHASE = 50_000

    enum class Phase {
        CAPTURE,
        TEMPLATE_DECODE,
        TO_MAT,
        MATCH_TEMPLATE,
        MIN_MAX_LOC,
        OCR,
        TOTAL,
    }

    private val logger = LoggerFactory.getLogger(VisionMetrics::class.java)

    private val samples = ConcurrentHashMap<Phase, MutableList<Long>>()

    fun <T> measure(phase: Phase, block: () -> T): T {
        val start = System.nanoTime()
        try {
            return block()
        } finally {
            record(phase, System.nanoTime() - start)
        }
    }

    fun record(phase: Phase, durationNanos: Long) {
        val phaseSamples = samples.computeIfAbsent(phase) {
            Collections.synchronizedList(ArrayList<Long>(1024))
        }
        if (phaseSamples.size < MAX_SAMPLES_PER_PHASE) {
            phaseSamples.add(durationNanos)
        }
    }

    fun reset() = samples.clear()

    fun hasSamples(): Boolean = samples.values.any { it.isNotEmpty() }

    fun summary(): String {
        val statsByPhase = Phase.entries.mapNotNull { phase ->
            val values = samples[phase]?.let { synchronized(it) { it.toLongArray() } }
            if (values == null || values.isEmpty()) null else phase to PhaseStats.of(values)
        }

        if (statsByPhase.isEmpty()) return "Vision metrics: no samples collected."

        return buildString {
            appendLine("Vision metrics (durations in ms):")
            appendLine(String.format("  %-16s %7s %9s %9s %9s %9s", "PHASE", "COUNT", "AVG", "P50", "P95", "MAX"))
            statsByPhase.forEach { (phase, stats) ->
                appendLine(
                    String.format(
                        "  %-16s %7d %9.2f %9.2f %9.2f %9.2f",
                        phase.name, stats.count, stats.avgMs, stats.p50Ms, stats.p95Ms, stats.maxMs
                    )
                )
            }
        }.trimEnd()
    }

    fun logSummary() {
        if (hasSamples()) logger.info(summary())
    }

    private data class PhaseStats(
        val count: Int,
        val avgMs: Double,
        val p50Ms: Double,
        val p95Ms: Double,
        val maxMs: Double,
    ) {
        companion object {
            fun of(values: LongArray): PhaseStats {
                val sorted = values.sortedArray()
                return PhaseStats(
                    count = sorted.size,
                    avgMs = sorted.average().toMillis(),
                    p50Ms = sorted.percentile(0.50).toMillis(),
                    p95Ms = sorted.percentile(0.95).toMillis(),
                    maxMs = sorted.last().toDouble().toMillis(),
                )
            }

            private fun LongArray.percentile(fraction: Double): Double {
                val index = ((size - 1) * fraction).toInt().coerceIn(0, size - 1)
                return this[index].toDouble()
            }

            private fun Double.toMillis(): Double = this / 1_000_000.0
        }
    }
}

