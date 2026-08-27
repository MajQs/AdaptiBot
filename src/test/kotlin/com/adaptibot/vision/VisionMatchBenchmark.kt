package com.adaptibot.vision

import nu.pattern.OpenCV
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc
import java.util.Random

/**
 * Baseline benchmark for the template-matching hot path (Step 0 of the vision performance plan).
 *
 * Deliberately **not** part of the regular test suite — it is a measurement harness, not a test:
 * it asserts nothing and takes tens of seconds. Run it manually to produce the before/after table.
 *
 * Run with:
 * `./gradlew test --tests "com.adaptibot.vision.VisionMatchBenchmark" -Dbenchmark=true`
 */
@Disabled("Measurement harness - run manually, see class docs")
class VisionMatchBenchmark {

    private companion object {
        const val SCREEN_WIDTH = 3840
        const val SCREEN_HEIGHT = 2160
        const val TEMPLATE_SIZE = 50

        /** Position at which the template is planted in the synthetic screenshot. */
        const val PLANT_X = 2600
        const val PLANT_Y = 1400

        const val WARMUP_ITERATIONS = 3
        const val MEASURED_ITERATIONS = 10

        @JvmStatic
        @BeforeAll
        fun loadOpenCv() = OpenCV.loadLocally()
    }

    @Test
    fun `measure match cost across search areas`() {
        val screenColor = syntheticScreenshot()
        val templateColor = screenColor.submat(
            Rect(PLANT_X, PLANT_Y, TEMPLATE_SIZE, TEMPLATE_SIZE)
        ).clone()
        val absentTemplate = randomMat(TEMPLATE_SIZE, TEMPLATE_SIZE)

        val screenGray = screenColor.toGray()
        val templateGray = templateColor.toGray()
        val absentGray = absentTemplate.toGray()

        // Regions expressed relative to the planted template, mirroring the plan's variants.
        val movesWithinLarge = Rect(PLANT_X - 200, PLANT_Y - 200, 800, 600)
        val movesWithinSmall = Rect(PLANT_X - 100, PLANT_Y - 100, 400, 300)
        val fixedWindow = Rect(PLANT_X - 8, PLANT_Y - 8, TEMPLATE_SIZE + 16, TEMPLATE_SIZE + 16)

        println()
        println("=== Vision match baseline (${SCREEN_WIDTH}x$SCREEN_HEIGHT, template ${TEMPLATE_SIZE}x$TEMPLATE_SIZE) ===")
        println(String.format("%-34s %10s %14s", "SCENARIO", "AVG [ms]", "POSITIONS"))

        listOf(
            Scenario("Anywhere / present / color", screenColor, templateColor),
            Scenario("Anywhere / absent  / color", screenColor, absentTemplate),
            Scenario("Anywhere / present / gray", screenGray, templateGray),
            Scenario("Anywhere / absent  / gray", screenGray, absentGray),
            Scenario("MovesWithin 800x600 / color", screenColor.submat(movesWithinLarge), templateColor),
            Scenario("MovesWithin 400x300 / color", screenColor.submat(movesWithinSmall), templateColor),
            Scenario("Fixed 66x66 / color", screenColor.submat(fixedWindow), templateColor),
        ).forEach { it.runAndPrint() }

        println("=========================================================================")
        println()
    }

    private class Scenario(val name: String, val haystack: Mat, val needle: Mat) {

        fun runAndPrint() {
            repeat(WARMUP_ITERATIONS) { matchOnce() }

            val start = System.nanoTime()
            repeat(MEASURED_ITERATIONS) { matchOnce() }
            val avgMs = (System.nanoTime() - start) / MEASURED_ITERATIONS / 1_000_000.0

            val positions = (haystack.cols() - needle.cols() + 1).toLong() *
                    (haystack.rows() - needle.rows() + 1).toLong()

            println(String.format("%-34s %10.2f %14d", name, avgMs, positions))
        }

        private fun matchOnce() {
            val result = Mat(
                haystack.rows() - needle.rows() + 1,
                haystack.cols() - needle.cols() + 1,
                CvType.CV_32FC1
            )
            Imgproc.matchTemplate(haystack, needle, result, Imgproc.TM_CCOEFF_NORMED)
            Core.minMaxLoc(result)
            result.release()
        }
    }

    /**
     * Random noise rather than a flat fill: a uniform image makes `TM_CCOEFF_NORMED`
     * degenerate and would produce unrealistically optimistic timings.
     */
    private fun syntheticScreenshot(): Mat = randomMat(SCREEN_WIDTH, SCREEN_HEIGHT)

    private fun randomMat(width: Int, height: Int): Mat {
        val mat = Mat(height, width, CvType.CV_8UC3)
        val pixels = ByteArray(width * height * 3)
        Random(42).nextBytes(pixels)
        mat.put(0, 0, pixels)
        return mat
    }

    private fun Mat.toGray(): Mat {
        val gray = Mat()
        Imgproc.cvtColor(this, gray, Imgproc.COLOR_BGR2GRAY)
        return gray
    }
}

